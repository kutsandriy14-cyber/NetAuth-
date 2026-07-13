package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.util.Calendar
import java.net.NetworkInterface
import java.net.Inet4Address
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.cancelChildren
import okhttp3.Request
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import java.io.File

class AccountViewModel(application: Application) : AndroidViewModel(application) {
    private val clientManager = NetAuthClientManager(application)
    private val db = AppDatabase.getDatabase(application)
    val userDao = db.userDao()
    val messageDao = db.messageDao()
    val allBannedHardware = userDao.getAllBannedHardware()

    private val _blockedUsers = MutableStateFlow<Set<String>>(clientManager.blockedUsers)
    val blockedUsers: StateFlow<Set<String>> = _blockedUsers.asStateFlow()

    fun blockUser(email: String) {
        val updated = clientManager.blockedUsers + email.trim().lowercase()
        clientManager.blockedUsers = updated
        _blockedUsers.value = updated
    }

    fun unblockUser(email: String) {
        val updated = clientManager.blockedUsers - email.trim().lowercase()
        clientManager.blockedUsers = updated
        _blockedUsers.value = updated
    }

    fun isUserBlocked(email: String): Boolean {
        return _blockedUsers.value.contains(email.trim().lowercase())
    }

    // Server connection states
    private val _connectionMode = MutableStateFlow("remote") // Always remote
    val connectionMode: StateFlow<String> = _connectionMode.asStateFlow()

    private val _serverUrl = MutableStateFlow(clientManager.serverUrl)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _savedServers = MutableStateFlow(clientManager.savedServers)
    val savedServers: StateFlow<Set<String>> = _savedServers.asStateFlow()

    private val _emailSuffix = MutableStateFlow(clientManager.emailSuffix)
    val emailSuffix: StateFlow<String> = _emailSuffix.asStateFlow()

    private val _serviceKey = MutableStateFlow(clientManager.serviceKey)
    val serviceKey: StateFlow<String> = _serviceKey.asStateFlow()

    // Discovery States
    private val _discoveryState = MutableStateFlow<String>("idle") // "idle", "searching", "found", "failed"
    val discoveryState: StateFlow<String> = _discoveryState.asStateFlow()

    private val _discoveredServerIp = MutableStateFlow<String?>(null)
    val discoveredServerIp: StateFlow<String?> = _discoveredServerIp.asStateFlow()

    private val _isScanningServers = MutableStateFlow(false)
    val isScanningServers: StateFlow<Boolean> = _isScanningServers.asStateFlow()

    // Built-in server state
    private val _isBuiltInServerRunning = MutableStateFlow(false)
    val isBuiltInServerRunning: StateFlow<Boolean> = _isBuiltInServerRunning.asStateFlow()

    private val _builtInServerMessage = MutableStateFlow<String?>(null)
    val builtInServerMessage: StateFlow<String?> = _builtInServerMessage.asStateFlow()

    // Active client Database Name
    private val _activeDatabase = MutableStateFlow(clientManager.activeDatabase)
    val activeDatabase: StateFlow<String> = _activeDatabase.asStateFlow()

    // Remote users fetched dynamically (no local user caching)
    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    // Dynamic file storage state
    private val _userFiles = MutableStateFlow<List<NetworkFileResponse>>(emptyList())
    val userFiles: StateFlow<List<NetworkFileResponse>> = _userFiles.asStateFlow()

    private val _isStorageLoading = MutableStateFlow(false)
    val isStorageLoading: StateFlow<Boolean> = _isStorageLoading.asStateFlow()

    // Current logged-in user state
    private val _loggedInUser = MutableStateFlow<User?>(null)
    val loggedInUser: StateFlow<User?> = _loggedInUser.asStateFlow()

    init {
        // Force remote mode
        clientManager.connectionMode = "remote"
        _connectionMode.value = "remote"

        // Enforce strictly online/server-driven data: delete any leftover cached users from local DB
        viewModelScope.launch {
            try {
                userDao.deleteAllUsers()
            } catch (e: Exception) {
                // ignore
            }
            // Fetch remote users list from the server dynamically
            refreshServerUsers()
        }

        // Restore logged in user session if present
        val savedUser = clientManager.getLoggedInUser()
        if (savedUser != null) {
            _loggedInUser.value = savedUser
            loadUserFiles()
        }

        startAutoDiscovery()
        startPeriodicConnectionMonitor()
    }

    // App settings, language and security
    private val _language = MutableStateFlow(clientManager.language)
    val language: StateFlow<String> = _language.asStateFlow()

    var currentLanguageState by androidx.compose.runtime.mutableStateOf(clientManager.language)
        private set

    private val _appPasscode = MutableStateFlow(clientManager.appPasscode)
    val appPasscode: StateFlow<String> = _appPasscode.asStateFlow()

    private val _isAppLocked = MutableStateFlow(clientManager.appPasscode.isNotEmpty())
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    fun setLanguage(lang: String) {
        clientManager.language = lang
        _language.value = lang
        currentLanguageState = lang
    }

    fun t(key: String): String {
        return Translation.get(key, currentLanguageState)
    }

    fun setAppPasscode(passcode: String) {
        clientManager.appPasscode = passcode
        _appPasscode.value = passcode
        _isAppLocked.value = passcode.isNotEmpty()
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        if (_appPasscode.value.isNotEmpty()) {
            _isAppLocked.value = true
        }
    }

    // Host Database Server Control
    fun startServer(port: Int = 8080) {
        // Built-in server disabled on the client side
    }

    fun stopServer() {
        // Built-in server disabled on the client side
    }

    // Databases Management
    fun getAvailableDatabases(): List<String> {
        val storageDir = File(getApplication<Application>().filesDir, "storage")
        if (!storageDir.exists()) return listOf("default")
        val dbs = storageDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
        return if (dbs.isEmpty()) listOf("default") else dbs
    }

    fun createNewDatabase(name: String) {
        val sanitized = name.trim().ifEmpty { "default" }.replace(Regex("[^a-zA-Z0-9_-]"), "")
        if (sanitized.isNotEmpty()) {
            val dbFolder = File(getApplication<Application>().filesDir, "storage/$sanitized")
            dbFolder.mkdirs()
            setActiveDatabase(sanitized)
        }
    }

    fun setActiveDatabase(name: String) {
        clientManager.activeDatabase = name
        _activeDatabase.value = name
        refreshServerUsers()
        if (_loggedInUser.value != null) {
            loadUserFiles()
        }
    }

    // Server users list loading
    fun refreshServerUsers() {
        viewModelScope.launch {
            try {
                val apiService = clientManager.getService()
                val serverUsers = apiService.getUsers().map { networkUser ->
                    networkUser.toLocalUser("") // Map remote user to User object without password hash exposure
                }
                _allUsers.value = serverUsers
            } catch (e: Exception) {
                _allUsers.value = emptyList()
            }
        }
    }

    // Server-side cloud storage files management
    fun loadUserFiles() {
        val user = _loggedInUser.value ?: return
        _isStorageLoading.value = true
        viewModelScope.launch {
            try {
                val api = clientManager.getService()
                _userFiles.value = api.getFiles(user.id)
            } catch (e: Exception) {
                _userFiles.value = emptyList()
            } finally {
                _isStorageLoading.value = false
            }
        }
    }

    fun uploadUserFile(fileName: String, content: String, onResult: (Boolean, String) -> Unit) {
        val user = _loggedInUser.value ?: return
        viewModelScope.launch {
            try {
                val api = clientManager.getService()
                api.uploadFile(user.id, NetworkUploadFileRequest(fileName = fileName, content = content))
                loadUserFiles()
                onResult(true, "File uploaded successfully")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Upload failed")
            }
        }
    }

    fun deleteUserFile(fileName: String, onResult: (Boolean, String) -> Unit) {
        val user = _loggedInUser.value ?: return
        viewModelScope.launch {
            try {
                val api = clientManager.getService()
                api.deleteFile(user.id, fileName)
                loadUserFiles()
                onResult(true, "File deleted successfully")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Delete failed")
            }
        }
    }

    // Messaging operations (Server-side dynamic communication)
    fun getChatPartnersFlow(): kotlinx.coroutines.flow.Flow<List<String>> {
        val user = _loggedInUser.value ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return kotlinx.coroutines.flow.flow {
            messageDao.getChatPartners(user.email).collect { list ->
                val filtered = list.filter { it.isNotEmpty() && !isUserBlocked(it) }
                emit(filtered)
            }
        }
    }

    fun getMessagesForPartner(partnerEmail: String): kotlinx.coroutines.flow.Flow<List<Message>> {
        val user = _loggedInUser.value ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        
        // Run background task to synchronize messages from server
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val api = clientManager.getService()
                val remoteMsgs = api.getMessages(user.email, partnerEmail)
                remoteMsgs.forEach { remote ->
                    val localMsg = Message(
                        id = remote.id,
                        senderEmail = remote.senderEmail,
                        receiverEmail = remote.receiverEmail,
                        text = remote.text,
                        timestamp = remote.timestamp
                    )
                    messageDao.insertMessage(localMsg)
                }
            } catch (e: Exception) {
                // Ignore background sync issues
            }
        }
        
        // Return Flow directly from local database
        return messageDao.getChatMessages(user.email, partnerEmail)
    }

    fun sendMessage(recipientEmail: String, text: String, onResult: (Boolean, String) -> Unit) {
        val sender = _loggedInUser.value ?: return
        if (text.trim().isEmpty()) {
            onResult(false, t("empty_msg_error"))
            return
        }
        viewModelScope.launch {
            val rcpt = recipientEmail.trim().lowercase()
            
            // 1. Save locally first (offline-first!)
            val localMsg = Message(
                id = (100000..Int.MAX_VALUE).random(),
                senderEmail = sender.email,
                receiverEmail = rcpt,
                text = text.trim(),
                timestamp = System.currentTimeMillis()
            )
            
            try {
                messageDao.insertMessage(localMsg)
            } catch (e: Exception) {
                // Ignore local write failure
            }

            // 2. Try to send to server
            try {
                val api = clientManager.getService()
                api.sendMessage(SendMessageRequest(sender.email, rcpt, text.trim()))
                onResult(true, "")
            } catch (e: Exception) {
                // If timeout or server unreachable, still return success for local database usage
                onResult(true, "offline")
            }
        }
    }

    fun deleteChat(partnerEmail: String) {
        val user = _loggedInUser.value ?: return
        viewModelScope.launch {
            try {
                // Delete locally
                messageDao.deleteChat(user.email, partnerEmail.trim().lowercase())
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun deleteMessage(messageId: Int) {
        viewModelScope.launch {
            try {
                // Delete locally
                messageDao.deleteMessage(messageId)
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Phone password reset states (Server-side helper)
    private val _resetPhone = MutableStateFlow("")
    val resetPhone: StateFlow<String> = _resetPhone.asStateFlow()

    private val _resetGeneratedCode = MutableStateFlow("")
    val resetGeneratedCode: StateFlow<String> = _resetGeneratedCode.asStateFlow()

    private val _resetInputCode = MutableStateFlow("")
    val resetInputCode: StateFlow<String> = _resetInputCode.asStateFlow()

    private val _resetError = MutableStateFlow<String?>(null)
    val resetError: StateFlow<String?> = _resetError.asStateFlow()

    fun sendResetSmsCode(phone: String, onSent: (String) -> Unit) {
        viewModelScope.launch {
            val formattedPhone = phone.trim()
            if (formattedPhone.isEmpty()) {
                _resetError.value = "Enter phone number"
                return@launch
            }
            // Check matching phone number across all users on server
            val matchedUser = _allUsers.value.find { it.phoneNumber.trim() == formattedPhone }
            if (matchedUser != null) {
                val code = (100000..999999).random().toString()
                _resetPhone.value = formattedPhone
                _resetGeneratedCode.value = code
                _resetInputCode.value = ""
                _resetError.value = null
                onSent(code)
            } else {
                _resetError.value = t("phone_not_found")
            }
        }
    }

    fun verifySmsCode(code: String): Boolean {
        if (code.trim() == _resetGeneratedCode.value && code.trim().isNotEmpty()) {
            _resetError.value = null
            return true
        } else {
            _resetError.value = t("invalid_code")
            return false
        }
    }

    fun performPasswordReset(newPass: String, onSuccess: () -> Unit) {
        val phone = _resetPhone.value
        if (phone.isEmpty()) {
            _resetError.value = "No active reset flow"
            return
        }
        viewModelScope.launch {
            val match = _allUsers.value.find { it.phoneNumber.trim() == phone }
            if (match != null) {
                try {
                    val api = clientManager.getService()
                    api.updatePassword(match.id, NetworkUpdatePasswordRequest(newPass))
                    _resetPhone.value = ""
                    _resetGeneratedCode.value = ""
                    _resetInputCode.value = ""
                    _resetError.value = null
                    refreshServerUsers()
                    onSuccess()
                } catch (e: Exception) {
                    _resetError.value = "Reset failed: ${e.localizedMessage}"
                }
            } else {
                _resetError.value = "Error locating matching user"
            }
        }
    }

    // Login screen state
    private val _loginEmail = MutableStateFlow("")
    val loginEmail: StateFlow<String> = _loginEmail.asStateFlow()

    private val _loginPassword = MutableStateFlow("")
    val loginPassword: StateFlow<String> = _loginPassword.asStateFlow()

    private val _loginKeyProtect = MutableStateFlow("")
    val loginKeyProtect: StateFlow<String> = _loginKeyProtect.asStateFlow()

    private val _requireKeyProtect = MutableStateFlow(false)
    val requireKeyProtect: StateFlow<Boolean> = _requireKeyProtect.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Registration draft state
    private val _regFirstName = MutableStateFlow("")
    val regFirstName: StateFlow<String> = _regFirstName.asStateFlow()

    private val _regLastName = MutableStateFlow("")
    val regLastName: StateFlow<String> = _regLastName.asStateFlow()

    private val _regBirthYear = MutableStateFlow("")
    val regBirthYear: StateFlow<String> = _regBirthYear.asStateFlow()

    private val _regBirthMonth = MutableStateFlow("January")
    val regBirthMonth: StateFlow<String> = _regBirthMonth.asStateFlow()

    private val _regBirthDay = MutableStateFlow("")
    val regBirthDay: StateFlow<String> = _regBirthDay.asStateFlow()

    private val _regGender = MutableStateFlow("Rather not say")
    val regGender: StateFlow<String> = _regGender.asStateFlow()

    private val _regEmailOption = MutableStateFlow("")
    val regEmailOption: StateFlow<String> = _regEmailOption.asStateFlow()

    private val _regCustomEmail = MutableStateFlow("")
    val regCustomEmail: StateFlow<String> = _regCustomEmail.asStateFlow()

    private val _regPassword = MutableStateFlow("")
    val regPassword: StateFlow<String> = _regPassword.asStateFlow()

    private val _regConfirmPassword = MutableStateFlow("")
    val regConfirmPassword: StateFlow<String> = _regConfirmPassword.asStateFlow()

    private val _regPhoneNumber = MutableStateFlow("")
    val regPhoneNumber: StateFlow<String> = _regPhoneNumber.asStateFlow()

    private val _regRecoveryEmail = MutableStateFlow("")
    val regRecoveryEmail: StateFlow<String> = _regRecoveryEmail.asStateFlow()

    private val _regError = MutableStateFlow<String?>(null)
    val regError: StateFlow<String?> = _regError.asStateFlow()

    // Email suggestions generator
    private val _emailSuggestions = MutableStateFlow<List<String>>(emptyList())
    val emailSuggestions: StateFlow<List<String>> = _emailSuggestions.asStateFlow()

    // Colors
    val avatarColors = listOf(
        0xFF1A73E8.toInt(),
        0xFFEA4335.toInt(),
        0xFFFBBC05.toInt(),
        0xFF34A853.toInt(),
        0xFF8E24AA.toInt(),
        0xFF00ACC1.toInt(),
        0xFFD81B60.toInt(),
        0xFFF4511E.toInt()
    )

    // Sync helpers
    fun setConnectionMode(mode: String) {
        // Forced remote
    }

    fun setServerUrl(url: String) {
        val sanitized = if (url.endsWith("/")) url else "$url/"
        if (clientManager.serverUrl == sanitized) {
            return
        }
        clientManager.serverUrl = sanitized
        _serverUrl.value = clientManager.serverUrl
        _savedServers.value = clientManager.savedServers
        logout() // Auto-logout on server disconnect/change
        viewModelScope.launch {
            try {
                val apiService = clientManager.getService()
                apiService.checkStatus()
                _discoveryState.value = "found"
            } catch (e: Exception) {
                // Keep existing discovery state or set to failed
            }
            refreshServerUsers()
        }
    }

    fun addSavedServer(url: String) {
        val sanitized = if (url.endsWith("/")) url else "$url/"
        val updated = clientManager.savedServers.toMutableSet()
        updated.add(sanitized)
        clientManager.savedServers = updated
        _savedServers.value = updated
    }

    fun removeSavedServer(url: String) {
        val sanitized = if (url.endsWith("/")) url else "$url/"
        val updated = clientManager.savedServers.toMutableSet()
        updated.remove(sanitized)
        clientManager.savedServers = updated
        _savedServers.value = updated
    }

    fun setEmailSuffix(suffix: String) {
        val formatted = if (suffix.startsWith("@")) suffix else "@$suffix"
        if (clientManager.emailSuffix == formatted) {
            return
        }
        clientManager.emailSuffix = formatted
        _emailSuffix.value = formatted
        logout() // Auto-logout on suffix change
    }

    fun setServiceKey(key: String) {
        if (clientManager.serviceKey == key) {
            return
        }
        clientManager.serviceKey = key
        _serviceKey.value = key
        logout() // Auto-logout on key change
    }

    fun selectLoginUser(user: User) {
        _loginEmail.value = user.email
        _loginPassword.value = ""
        _loginError.value = null
    }

    fun setLoginEmail(email: String) {
        _loginEmail.value = email
        _loginError.value = null
    }

    fun setLoginPassword(password: String) {
        _loginPassword.value = password
        _loginError.value = null
    }

    fun setLoginKeyProtect(key: String) {
        _loginKeyProtect.value = key
        _loginError.value = null
    }

    fun performLogin(onSuccess: () -> Unit) {
        val email = _loginEmail.value.trim()
        val password = _loginPassword.value
        val keyProtectInput = _loginKeyProtect.value

        if (email.isEmpty() || password.isEmpty()) {
            _loginError.value = "Please fill in all fields"
            return
        }

        _loginError.value = null
        viewModelScope.launch {
            if (isCurrentDeviceBanned()) {
                _loginError.value = "Access denied: This device has been banned from the database (Hardware/IP Ban)."
                return@launch
            }
            try {
                val apiService = clientManager.getService()
                val response = apiService.login(NetworkLoginRequest(email, password))
                val localUser = response.toLocalUser(password)

                // Key Protect requested every time on login
                if (!_requireKeyProtect.value) {
                    _requireKeyProtect.value = true
                    _loginError.value = "Key Protect code required to complete login."
                    return@launch
                }

                if (keyProtectInput.trim() != localUser.keyProtect.trim()) {
                    _loginError.value = "Invalid Key Protect code"
                    return@launch
                }

                // Keep logged-in user in memory and store locally
                _loggedInUser.value = localUser
                _loginError.value = null
                _requireKeyProtect.value = false
                _loginKeyProtect.value = ""

                // Save active session for auto-login
                clientManager.saveSession(email, password)
                clientManager.saveLoggedInUser(localUser)

                refreshServerUsers()
                loadUserFiles()
                onSuccess()
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 401 || e.code() == 404) {
                    _loginError.value = "Invalid email or password"
                } else {
                    _loginError.value = "Server error: ${e.message()}"
                }
            } catch (e: Exception) {
                _loginError.value = "Server authentication error: ${e.localizedMessage ?: "Connection refused"}"
            }
        }
    }

    fun logout() {
        clientManager.clearSession()
        clientManager.clearLoggedInUser()
        _loggedInUser.value = null
        _loginPassword.value = ""
        _userFiles.value = emptyList()
    }

    // Setters for Registration
    fun setRegNames(first: String, last: String) {
        _regFirstName.value = first
        _regLastName.value = last
        _regError.value = null
    }

    fun setRegBirthInfo(day: String, month: String, year: String, gender: String) {
        _regBirthDay.value = day
        _regBirthMonth.value = month
        _regBirthYear.value = year
        _regGender.value = gender
        _regError.value = null
    }

    fun generateSuggestions() {
        val first = _regFirstName.value.trim().lowercase().filter { it.isLetterOrDigit() && !it.isWhitespace() }
        val last = _regLastName.value.trim().lowercase().filter { it.isLetterOrDigit() && !it.isWhitespace() }
        val suffix = _emailSuffix.value
        
        val validFirst = first.isNotBlank()
        val validLast = last.isNotBlank()
        
        if (!validFirst && !validLast) {
            _emailSuggestions.value = listOf("user123$suffix", "account$suffix")
            return
        }
        val s1 = if (!validLast) "$first$suffix" else if (!validFirst) "$last$suffix" else "${first}.${last}$suffix"
        val s2 = if (!validLast) "${first}1$suffix" else if (!validFirst) "${last}1$suffix" else "${last}.${first}$suffix"
        val s3 = "${first}${last}${(10..99).random()}$suffix"
        _emailSuggestions.value = listOf(s1, s2, s3)
        _regEmailOption.value = s1
    }

    fun setRegEmailOption(option: String) {
        _regEmailOption.value = option
        _regError.value = null
    }

    fun setRegCustomEmail(email: String) {
        _regCustomEmail.value = email
        _regError.value = null
    }

    fun setRegPassword(pass: String, confirm: String) {
        _regPassword.value = pass
        _regConfirmPassword.value = confirm
        _regError.value = null
    }

    fun setRegContactInfo(phone: String, recovery: String) {
        _regPhoneNumber.value = phone
        _regRecoveryEmail.value = recovery
        _regError.value = null
    }

    fun validateNames(): Boolean {
        if (_regFirstName.value.trim().isEmpty()) {
            _regError.value = "Enter first name"
            return false
        }
        _regError.value = null
        return true
    }

    fun validateBirthInfo(): Boolean {
        val day = _regBirthDay.value.toIntOrNull()
        val year = _regBirthYear.value.toIntOrNull()

        if (day == null || day !in 1..31) {
            _regError.value = "Enter a valid day of the month"
            return false
        }
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (year == null || year !in 1900..currentYear) {
            _regError.value = "Enter a valid four-digit year"
            return false
        }
        _regError.value = null
        return true
    }

    fun validateEmailStep(): Boolean {
        val option = _regEmailOption.value
        val email = if (option == "custom") _regCustomEmail.value.trim() else option

        if (email.isEmpty()) {
            _regError.value = "Choose or create an account email"
            return false
        }
        if (!email.contains("@")) {
            _regError.value = "Invalid email format. Suffix required."
            return false
        }
        _regError.value = null
        return true
    }

    fun validatePasswordStep(): Boolean {
        val pass = _regPassword.value
        val confirm = _regConfirmPassword.value

        if (pass.length < 6) {
            _regError.value = "Try a mix of letters, numbers, and symbols with at least 6 characters"
            return false
        }
        if (pass != confirm) {
            _regError.value = "These passwords don't match. Try again."
            return false
        }
        _regError.value = null
        return true
    }

    fun performRegistration(onSuccess: () -> Unit) {
        val option = _regEmailOption.value
        val email = if (option == "custom") _regCustomEmail.value.trim() else option
        val password = _regPassword.value
        val randomColor = avatarColors.random()
        val monthStr = _regBirthMonth.value
        val dayStr = _regBirthDay.value.padStart(2, '0')
        val yearStr = _regBirthYear.value
        val dob = "$yearStr-$monthStr-$dayStr"
        val firstName = _regFirstName.value.trim()
        val lastName = _regLastName.value.trim()

        _regError.value = null
        viewModelScope.launch {
            if (isCurrentDeviceBanned()) {
                _regError.value = "Access denied: This device has been banned from registering (Hardware/IP Ban)."
                return@launch
            }
            if (_allUsers.value.size >= 3) {
                _regError.value = t("max_accounts_reached")
                return@launch
            }

            val emailExists = _allUsers.value.any { it.email.equals(email, ignoreCase = true) }
            if (emailExists) {
                _regError.value = "An account with this email is already registered."
                return@launch
            }

            val nameExists = _allUsers.value.any { it.firstName.equals(firstName, ignoreCase = true) && it.lastName.equals(lastName, ignoreCase = true) }
            if (nameExists) {
                _regError.value = "An account with the same name ($firstName $lastName) already exists."
                return@launch
            }

            try {
                val apiService = clientManager.getService()
                
                // KeyProtect is set as the account password
                val keyProtect = password
                
                val request = NetworkRegisterRequest(
                    email = email,
                    passwordHash = password,
                    firstName = firstName,
                    lastName = lastName,
                    birthDate = dob,
                    gender = _regGender.value,
                    avatarColor = randomColor,
                    phoneNumber = _regPhoneNumber.value.trim(),
                    recoveryEmail = _regRecoveryEmail.value.trim(),
                    ipAddress = clientManager.deviceIp,
                    macAddress = clientManager.deviceMac,
                    keyProtect = keyProtect,
                    dataQuotaMb = 200
                )
                val response = apiService.register(request)
                val localUser = response.toLocalUser(password)
                
                // Save user session and insert into local Room DB
                _loggedInUser.value = localUser
                _regError.value = null
                resetRegDraft()
                
                clientManager.saveSession(email, password)
                clientManager.saveLoggedInUser(localUser)
                
                refreshServerUsers()
                loadUserFiles()
                onSuccess()
            } catch (e: retrofit2.HttpException) {
                val errorMsg = try {
                    val errorBody = e.response()?.errorBody()?.string()
                    if (errorBody != null && errorBody.contains("\"message\"")) {
                        val messageValue = errorBody.substringAfter("\"message\"").substringAfter("\"").substringBefore("\"")
                        messageValue
                    } else {
                        "HTTP ${e.code()}: ${e.message()}"
                    }
                } catch (jsonEx: Exception) {
                    "HTTP ${e.code()}: ${e.message()}"
                }
                _regError.value = "Registration failed: $errorMsg"
            } catch (e: Exception) {
                _regError.value = "Registration failed: ${e.localizedMessage ?: "Connection refused"}"
            }
        }
    }

    private fun resetRegDraft() {
        _regFirstName.value = ""
        _regLastName.value = ""
        _regBirthYear.value = ""
        _regBirthMonth.value = "January"
        _regBirthDay.value = ""
        _regGender.value = "Rather not say"
        _regEmailOption.value = ""
        _regCustomEmail.value = ""
        _regPassword.value = ""
        _regConfirmPassword.value = ""
        _regPhoneNumber.value = ""
        _regRecoveryEmail.value = ""
        _regError.value = null
    }

    fun updateProfile(
        firstName: String,
        lastName: String,
        birthDate: String,
        gender: String,
        phoneNumber: String,
        recoveryEmail: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUser = _loggedInUser.value ?: return
        if (firstName.trim().isEmpty()) {
            onError("First name cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                val apiService = clientManager.getService()
                val request = NetworkUpdateProfileRequest(
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    birthDate = birthDate,
                    gender = gender,
                    phoneNumber = phoneNumber.trim(),
                    recoveryEmail = recoveryEmail.trim()
                )
                val response = apiService.updateProfile(currentUser.id, request)
                
                val updatedUser = response.toLocalUser(currentUser.passwordHash)
                _loggedInUser.value = updatedUser
                onSuccess()
            } catch (e: Exception) {
                onError("Remote update failed: ${e.localizedMessage ?: "Server offline"}")
            }
        }
    }

    fun updatePassword(newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentUser = _loggedInUser.value ?: return
        if (newPass.length < 6) {
            onError("Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            try {
                val apiService = clientManager.getService()
                apiService.updatePassword(currentUser.id, NetworkUpdatePasswordRequest(newPass))
                
                val updatedUser = currentUser.copy(passwordHash = newPass)
                _loggedInUser.value = updatedUser
                onSuccess()
            } catch (e: Exception) {
                onError("Remote password update failed: ${e.localizedMessage ?: "Server offline"}")
            }
        }
    }

    fun removeLocalAccountCache(user: User) {
        viewModelScope.launch {
            try {
                val apiService = clientManager.getService()
                apiService.deleteAccount(user.id)
                refreshServerUsers()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        val currentUser = _loggedInUser.value ?: return
        
        viewModelScope.launch {
            try {
                val apiService = clientManager.getService()
                apiService.deleteAccount(currentUser.id)
            } catch (e: Exception) {
                // allow fallback
            }
            _loggedInUser.value = null
            onSuccess()
        }
    }

    fun checkServerStatus(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val apiService = clientManager.getService()
                val response = apiService.checkStatus()
                _discoveryState.value = "found"
                refreshServerUsers()
                onResult(true, "Connected to: ${response.status} (${response.message})")
            } catch (e: Exception) {
                onResult(false, "Server unreachable: ${e.localizedMessage ?: "Connection timed out"}")
            }
        }
    }

    // --- AUTOMATIC SERVER DISCOVERY SYSTEM ---

    fun startAutoDiscovery(onComplete: (Boolean) -> Unit = {}) {
        _discoveryState.value = "searching"
        _discoveredServerIp.value = null
        viewModelScope.launch {
            val udpUrl = discoverServerViaUdp()
            if (udpUrl != null) {
                setServerUrl(udpUrl)
                _discoveredServerIp.value = udpUrl
                _discoveryState.value = "found"
                refreshServerUsers()
                onComplete(true)
                return@launch
            }

            val scannedUrl = discoverServerViaSubnetScan()
            if (scannedUrl != null) {
                setServerUrl(scannedUrl)
                _discoveredServerIp.value = scannedUrl
                _discoveryState.value = "found"
                refreshServerUsers()
                onComplete(true)
            } else {
                _discoveryState.value = "failed"
                _allUsers.value = emptyList()
                onComplete(false)
            }
        }
    }

    private fun getLocalIpAddresses(): List<String> {
        val ipList = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address) {
                        addr.hostAddress?.let { ipList.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ipList
    }

    private suspend fun discoverServerViaUdp(): String? = withContext(Dispatchers.IO) {
        var socket: java.net.DatagramSocket? = null
        try {
            socket = java.net.DatagramSocket().apply {
                broadcast = true
                soTimeout = 1500
            }
            val sendData = "NETAUTH_DISCOVER".toByteArray()
            val broadcastAddr = java.net.InetAddress.getByName("255.255.255.255")
            val sendPacket = java.net.DatagramPacket(sendData, sendData.size, broadcastAddr, 8888)
            
            for (i in 0..2) {
                socket.send(sendPacket)
                kotlinx.coroutines.delay(100)
            }

            val recvBuf = ByteArray(1024)
            val receivePacket = java.net.DatagramPacket(recvBuf, recvBuf.size)
            socket.receive(receivePacket)

            val message = String(receivePacket.data, 0, receivePacket.length).trim()
            if (message.startsWith("NETAUTH_SERVER:")) {
                return@withContext message.substringAfter("NETAUTH_SERVER:")
            }
        } catch (e: Exception) {
            // timeout
        } finally {
            socket?.close()
        }
        null
    }

    private suspend fun discoverAllServersViaUdp(): List<String> = withContext(Dispatchers.IO) {
        val foundUrls = mutableListOf<String>()
        var socket: java.net.DatagramSocket? = null
        try {
            socket = java.net.DatagramSocket().apply {
                broadcast = true
                soTimeout = 800
            }
            val sendData = "NETAUTH_DISCOVER".toByteArray()
            val broadcastAddr = java.net.InetAddress.getByName("255.255.255.255")
            val sendPacket = java.net.DatagramPacket(sendData, sendData.size, broadcastAddr, 8888)
            
            for (i in 0..1) {
                socket.send(sendPacket)
                kotlinx.coroutines.delay(50)
            }

            val recvBuf = ByteArray(1024)
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 800) {
                try {
                    val receivePacket = java.net.DatagramPacket(recvBuf, recvBuf.size)
                    socket.receive(receivePacket)
                    val message = String(receivePacket.data, 0, receivePacket.length).trim()
                    if (message.startsWith("NETAUTH_SERVER:")) {
                        val url = message.substringAfter("NETAUTH_SERVER:")
                        if (!foundUrls.contains(url)) {
                            foundUrls.add(url)
                        }
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    break
                } catch (e: Exception) {
                    // ignore
                }
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            socket?.close()
        }
        foundUrls
    }

    private suspend fun discoverAllServersViaSubnetScan(): List<String> = withContext(Dispatchers.IO) {
        val ips = getLocalIpAddresses()
        val subnets = ips.map { ip ->
            val parts = ip.split(".")
            if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}." else null
        }.filterNotNull().distinct()

        if (subnets.isEmpty()) return@withContext emptyList<String>()

        val scanClient = OkHttpClient.Builder()
            .connectTimeout(250, TimeUnit.MILLISECONDS)
            .readTimeout(250, TimeUnit.MILLISECONDS)
            .build()

        val foundUrls = java.util.Collections.synchronizedSet(mutableSetOf<String>())

        coroutineScope {
            val jobs = mutableListOf<kotlinx.coroutines.Deferred<Unit>>()
            for (subnet in subnets) {
                for (i in 1..254) {
                    val hostIp = "$subnet$i"
                    val job = async {
                        try {
                            val request = Request.Builder()
                                .url("http://$hostIp:8080/api/status")
                                .build()
                            scanClient.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    val body = response.body?.string() ?: ""
                                    if (body.contains("status") || response.code == 200) {
                                        foundUrls.add("http://$hostIp:8080/")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    jobs.add(job)
                }
            }
            jobs.awaitAll()
        }
        foundUrls.toList()
    }

    fun scanForAllNearbyServers(onResult: (Int) -> Unit = {}) {
        if (_isScanningServers.value) return
        _isScanningServers.value = true
        viewModelScope.launch {
            try {
                val udpUrls = async { discoverAllServersViaUdp() }
                val subnetUrls = async { discoverAllServersViaSubnetScan() }
                
                val allFound = (udpUrls.await() + subnetUrls.await()).distinct()
                
                if (allFound.isNotEmpty()) {
                    val updated = clientManager.savedServers.toMutableSet()
                    updated.addAll(allFound)
                    clientManager.savedServers = updated
                    _savedServers.value = updated
                    
                    // If current server is empty or offline, switch to the first found
                    val isCurrentAlive = try {
                        val apiService = clientManager.getService()
                        apiService.checkStatus()
                        true
                    } catch (e: Exception) {
                        false
                    }
                    if (!isCurrentAlive) {
                        setServerUrl(allFound.first())
                    }
                }
                _isScanningServers.value = false
                onResult(allFound.size)
            } catch (e: Exception) {
                _isScanningServers.value = false
                onResult(0)
            }
        }
    }

    private suspend fun discoverServerViaSubnetScan(): String? = withContext(Dispatchers.IO) {
        val ips = getLocalIpAddresses()
        val subnets = ips.map { ip ->
            val parts = ip.split(".")
            if (parts.size == 4) "${parts[0]}.${parts[1]}.${parts[2]}." else null
        }.filterNotNull().distinct()

        if (subnets.isEmpty()) return@withContext null

        val scanClient = OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.MILLISECONDS)
            .readTimeout(300, TimeUnit.MILLISECONDS)
            .build()

        val foundUrl = java.util.concurrent.atomic.AtomicReference<String?>(null)

        coroutineScope {
            val jobs = mutableListOf<kotlinx.coroutines.Deferred<Unit>>()
            for (subnet in subnets) {
                for (i in 1..254) {
                    if (foundUrl.get() != null) break
                    val hostIp = "$subnet$i"
                    val job = async {
                        if (foundUrl.get() != null) return@async
                        try {
                            val request = Request.Builder()
                                .url("http://$hostIp:8080/api/status")
                                .build()
                            scanClient.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    val body = response.body?.string() ?: ""
                                    if (body.contains("status") || response.code == 200) {
                                        foundUrl.compareAndSet(null, "http://$hostIp:8080/")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    jobs.add(job)
                }
            }

            for (job in jobs) {
                job.join()
                if (foundUrl.get() != null) {
                    coroutineContext.cancelChildren()
                    break
                }
            }
        }
        foundUrl.get()
    }

    private fun startPeriodicConnectionMonitor() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                kotlinx.coroutines.delay(5000)
                val currentState = _discoveryState.value
                val currentUrl = _serverUrl.value

                if (!currentUrl.isNullOrBlank()) {
                    val isAlive = pingServer(currentUrl)
                    if (isAlive) {
                        if (currentState != "found") {
                            withContext(Dispatchers.Main) {
                                _discoveryState.value = "found"
                            }
                        }
                    } else {
                        if (currentState == "found") {
                            withContext(Dispatchers.Main) {
                                _discoveryState.value = "failed"
                            }
                        }
                        // Only try lightweight UDP discovery in background
                        val udpUrl = discoverServerViaUdp()
                        if (udpUrl != null && udpUrl != currentUrl) {
                            withContext(Dispatchers.Main) {
                                setServerUrl(udpUrl)
                                _discoveredServerIp.value = udpUrl
                                _discoveryState.value = "found"
                            }
                        }
                    }
                } else {
                    val udpUrl = discoverServerViaUdp()
                    if (udpUrl != null) {
                        withContext(Dispatchers.Main) {
                            setServerUrl(udpUrl)
                            _discoveredServerIp.value = udpUrl
                            _discoveryState.value = "found"
                        }
                    }
                }
            }
        }
    }

    private suspend fun pingServer(url: String): Boolean {
        return try {
            val scanClient = OkHttpClient.Builder()
                .connectTimeout(3000, TimeUnit.MILLISECONDS)
                .readTimeout(3000, TimeUnit.MILLISECONDS)
                .build()
            val request = Request.Builder()
                .url(if (url.endsWith("/")) "${url}api/status" else "$url/api/status")
                .build()
            scanClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    fun scanLocalFiles(extension: String): List<File> {
        val filesList = mutableListOf<File>()
        try {
            val downloadsDir = File("/storage/emulated/0/Download")
            if (downloadsDir.exists() && downloadsDir.isDirectory) {
                downloadsDir.listFiles()?.filter { it.isFile && it.name.endsWith(extension, ignoreCase = true) }?.let {
                    filesList.addAll(it)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        try {
            getApplication<Application>().getExternalFilesDir(null)?.let { extDir ->
                extDir.listFiles()?.filter { it.isFile && it.name.endsWith(extension, ignoreCase = true) }?.let {
                    filesList.addAll(it)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return filesList
    }

    suspend fun isCurrentDeviceBanned(): Boolean {
        return false
    }

    fun banHardware(value: String, banType: String) {
        viewModelScope.launch {
            userDao.insertBan(BannedHardware(value, banType))
        }
    }

    fun unbanHardware(value: String) {
        viewModelScope.launch {
            userDao.deleteBan(BannedHardware(value))
        }
    }

    fun importServiceKeyFromJson(file: File, callback: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = file.readText()
                val json = org.json.JSONObject(content)
                val serverUrl = json.optString("server_url", "")
                val serviceKey = json.optString("service_key", "")
                val emailSuffix = json.optString("email_suffix", "")
                
                if (serverUrl.isEmpty() || serviceKey.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        callback(false, "Invalid service key file format.")
                    }
                    return@launch
                }
                
                withContext(Dispatchers.Main) {
                    setServerUrl(serverUrl)
                    setServiceKey(serviceKey)
                    if (emailSuffix.isNotEmpty()) {
                        setEmailSuffix(emailSuffix)
                    }
                    callback(true, "Imported Service Key successfully and connected to $serverUrl")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false, "Failed to parse JSON service key: ${e.localizedMessage}")
                }
            }
        }
    }

    fun exportAccountsToAf(file: File, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val usersList = _allUsers.value
                val moshi = com.squareup.moshi.Moshi.Builder().build()
                val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, User::class.java)
                val adapter = moshi.adapter<List<User>>(listType)
                val jsonStr = adapter.toJson(usersList)
                file.writeText(jsonStr, Charsets.UTF_8)
                withContext(Dispatchers.Main) {
                    onResult(true, "Accounts exported successfully to ${file.name}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Export failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun exportSingleUserFullAf(user: User, file: File, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiService = clientManager.getService()
                
                // 1. Download User Cloud Files
                val filesList = try {
                    apiService.getFiles(user.id)
                } catch (e: Exception) {
                    emptyList()
                }
                val userFiles = mutableListOf<Map<String, Any>>()
                filesList.forEach { fileResponse ->
                    try {
                        val responseBody = apiService.downloadFile(user.id, fileResponse.name)
                        val bytes = responseBody.bytes()
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        userFiles.add(mapOf(
                            "fileName" to fileResponse.name,
                            "fileSize" to fileResponse.size,
                            "contentBase64" to base64
                        ))
                    } catch (e: Exception) {
                        // ignore file download errors
                    }
                }
                
                // 2. Fetch Chat Conversations
                val chats = mutableListOf<Map<String, Any>>()
                val otherUsers = _allUsers.value.filter { it.email.lowercase() != user.email.lowercase() }
                otherUsers.forEach { other ->
                    try {
                        val localMsgs = messageDao.getChatMessagesList(user.email, other.email)
                        val serverMsgs = try {
                            apiService.getMessages(user.email, other.email)
                        } catch (e: Exception) {
                            emptyList()
                        }
                        val allMsgs = (localMsgs + serverMsgs.map { networkMsg ->
                            Message(
                                id = networkMsg.id,
                                senderEmail = networkMsg.senderEmail,
                                receiverEmail = networkMsg.receiverEmail,
                                text = networkMsg.text,
                                timestamp = networkMsg.timestamp
                            )
                        }).distinctBy { it.id }
                        
                        if (allMsgs.isNotEmpty()) {
                            val msgMaps = allMsgs.map { m ->
                                mapOf(
                                    "id" to m.id,
                                    "senderEmail" to m.senderEmail,
                                    "receiverEmail" to m.receiverEmail,
                                    "text" to m.text,
                                    "timestamp" to m.timestamp
                                )
                            }
                            chats.add(mapOf(
                                "partnerEmail" to other.email,
                                "messages" to msgMaps
                            ))
                        }
                    } catch (e: Exception) {
                        // ignore chat retrieval errors
                    }
                }
                
                // 3. User Profile Information
                val userProfile = mapOf(
                    "email" to user.email,
                    "passwordHash" to user.passwordHash,
                    "firstName" to user.firstName,
                    "lastName" to user.lastName,
                    "birthDate" to user.birthDate,
                    "gender" to user.gender,
                    "avatarColor" to user.avatarColor,
                    "phoneNumber" to user.phoneNumber,
                    "recoveryEmail" to user.recoveryEmail,
                    "ipAddress" to user.ipAddress,
                    "macAddress" to user.macAddress,
                    "keyProtect" to user.keyProtect,
                    "dataQuotaMb" to user.dataQuotaMb
                )
                
                // 4. Blocklist
                val blocklist = _blockedUsers.value.toList()
                
                // Combine into master backup map
                val backupMap = mapOf(
                    "fileFormat" to "NetAuthAccountBackup",
                    "version" to "1.0",
                    "exportTime" to System.currentTimeMillis(),
                    "userProfile" to userProfile,
                    "blocklist" to blocklist,
                    "userFiles" to userFiles,
                    "chats" to chats
                )
                
                val moshi = com.squareup.moshi.Moshi.Builder().build()
                val adapter = moshi.adapter(Any::class.java)
                val jsonStr = adapter.toJson(backupMap)
                file.writeText(jsonStr, Charsets.UTF_8)
                
                withContext(Dispatchers.Main) {
                    onResult(true, "Full account backup for ${user.email} exported successfully to ${file.name}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Export failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun exportAllChatsOfUserToChat(userEmail: String, file: File, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch all other users
                val otherUsers = _allUsers.value.filter { it.email.lowercase() != userEmail.lowercase() }
                val allUserMsgs = mutableListOf<Message>()
                otherUsers.forEach { other ->
                    try {
                        val messages = messageDao.getChatMessagesList(userEmail, other.email)
                        allUserMsgs.addAll(messages)
                    } catch (e: Exception) {
                        // ignore individual failures
                    }
                }
                
                // Remove potential duplicate messages
                val distinctMsgs = allUserMsgs.distinctBy { it.id }
                
                // Find primary conversation partner
                val interlocutors = distinctMsgs.flatMap { listOf(it.senderEmail, it.receiverEmail) }
                    .distinct()
                    .filter { it.lowercase() != userEmail.lowercase() }
                val primaryPartner = interlocutors.firstOrNull() ?: "Friend"
                
                val chatTranscript = mapOf(
                    "fileFormat" to "NetAuthChatTranscript",
                    "version" to "1.0",
                    "participants" to listOf(userEmail, primaryPartner),
                    "messageCount" to distinctMsgs.size,
                    "messages" to distinctMsgs.map { m ->
                        mapOf(
                            "id" to m.id,
                            "senderEmail" to m.senderEmail,
                            "receiverEmail" to m.receiverEmail,
                            "text" to m.text,
                            "timestamp" to m.timestamp
                        )
                    }
                )
                
                val moshi = com.squareup.moshi.Moshi.Builder().build()
                val adapter = moshi.adapter(Any::class.java)
                val jsonStr = adapter.toJson(chatTranscript)
                file.writeText(jsonStr, Charsets.UTF_8)
                
                withContext(Dispatchers.Main) {
                    onResult(true, "All chat transcripts for $userEmail exported successfully to ${file.name}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Export chats failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun importAccountsFromAf(file: File, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonStr = file.readText(Charsets.UTF_8)
                val moshi = com.squareup.moshi.Moshi.Builder().build()
                val adapter = moshi.adapter(Any::class.java)
                val parsed = adapter.fromJson(jsonStr)
                
                val apiService = clientManager.getService()
                
                if (parsed is Map<*, *> && parsed["fileFormat"] == "NetAuthAccountBackup") {
                    val profile = parsed["userProfile"] as? Map<*, *> ?: throw Exception("Profile data not found in backup")
                    val email = profile["email"] as? String ?: ""
                    val passwordHash = profile["passwordHash"] as? String ?: ""
                    val firstName = profile["firstName"] as? String ?: ""
                    val lastName = profile["lastName"] as? String ?: ""
                    val birthDate = profile["birthDate"] as? String ?: ""
                    val gender = profile["gender"] as? String ?: ""
                    val avatarColor = (profile["avatarColor"] as? Double)?.toInt() ?: (profile["avatarColor"] as? Int) ?: 0
                    val phoneNumber = profile["phoneNumber"] as? String ?: ""
                    val recoveryEmail = profile["recoveryEmail"] as? String ?: ""
                    val ipAddress = profile["ipAddress"] as? String ?: ""
                    val macAddress = profile["macAddress"] as? String ?: ""
                    val keyProtect = profile["keyProtect"] as? String ?: ""
                    val dataQuotaMb = (profile["dataQuotaMb"] as? Double)?.toInt() ?: (profile["dataQuotaMb"] as? Int) ?: 200
                    
                    // 1. Register User on the active Database Partition
                    try {
                        apiService.register(NetworkRegisterRequest(
                            email = email,
                            passwordHash = passwordHash,
                            firstName = firstName,
                            lastName = lastName,
                            birthDate = birthDate,
                            gender = gender,
                            avatarColor = avatarColor,
                            phoneNumber = phoneNumber,
                            recoveryEmail = recoveryEmail,
                            ipAddress = ipAddress,
                            macAddress = macAddress,
                            keyProtect = keyProtect,
                            dataQuotaMb = dataQuotaMb
                        ))
                    } catch (e: Exception) {
                        // User might exist, continue with restoring assets
                    }
                    
                    refreshServerUsers()
                    val registeredUser = _allUsers.value.find { it.email.lowercase() == email.lowercase() }
                    val targetUserId = registeredUser?.id ?: 1
                    
                    // 2. Restore local blocklist
                    val blocklist = parsed["blocklist"] as? List<*> ?: emptyList<Any>()
                    blocklist.forEach { item ->
                        if (item is String) {
                            withContext(Dispatchers.Main) {
                                blockUser(item)
                            }
                        }
                    }
                    
                    // 3. Restore cloud files
                    val userFiles = parsed["userFiles"] as? List<*> ?: emptyList<Any>()
                    userFiles.forEach { item ->
                        if (item is Map<*, *>) {
                            val fileName = item["fileName"] as? String ?: ""
                            val contentBase64 = item["contentBase64"] as? String ?: ""
                            if (fileName.isNotEmpty() && contentBase64.isNotEmpty()) {
                                try {
                                    apiService.uploadFile(targetUserId, NetworkUploadFileRequest(fileName = fileName, content = contentBase64))
                                } catch (e: Exception) {
                                    // ignore upload failures
                                }
                            }
                        }
                    }
                    
                    // 4. Restore chat messaging transcripts
                    val chatsList = parsed["chats"] as? List<*> ?: emptyList<Any>()
                    chatsList.forEach { chatItem ->
                        if (chatItem is Map<*, *>) {
                            val messages = chatItem["messages"] as? List<*> ?: emptyList<Any>()
                            messages.forEach { msgItem ->
                                if (msgItem is Map<*, *>) {
                                    val id = (msgItem["id"] as? Double)?.toInt() ?: (msgItem["id"] as? Int) ?: 0
                                    val senderEmail = msgItem["senderEmail"] as? String ?: ""
                                    val receiverEmail = msgItem["receiverEmail"] as? String ?: ""
                                    val text = msgItem["text"] as? String ?: ""
                                    val timestamp = (msgItem["timestamp"] as? Double)?.toLong() ?: (msgItem["timestamp"] as? Long) ?: System.currentTimeMillis()
                                    
                                    val messageObj = Message(
                                        id = id,
                                        senderEmail = senderEmail,
                                        receiverEmail = receiverEmail,
                                        text = text,
                                        timestamp = timestamp
                                    )
                                    messageDao.insertMessage(messageObj)
                                    
                                    try {
                                        apiService.sendMessage(SendMessageRequest(
                                            senderEmail = senderEmail,
                                            receiverEmail = receiverEmail,
                                            text = text
                                        ))
                                    } catch (e: Exception) {
                                        // ignore message upload failure
                                    }
                                }
                            }
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        logout()
                        refreshServerUsers()
                        onResult(true, "Full account $email imported and restored successfully from ${file.name}!")
                    }
                } else {
                    // Fallback to legacy List<User> import
                    val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, User::class.java)
                    val listAdapter = moshi.adapter<List<User>>(listType)
                    val usersList = listAdapter.fromJson(jsonStr) ?: emptyList()
                    
                    val apiServiceLeg = clientManager.getService()
                    var importedCount = 0
                    usersList.forEach { user ->
                        try {
                            apiServiceLeg.register(NetworkRegisterRequest(
                                email = user.email,
                                passwordHash = user.passwordHash,
                                firstName = user.firstName,
                                lastName = user.lastName,
                                birthDate = user.birthDate,
                                gender = user.gender,
                                avatarColor = user.avatarColor,
                                phoneNumber = user.phoneNumber,
                                recoveryEmail = user.recoveryEmail,
                                ipAddress = user.ipAddress,
                                macAddress = user.macAddress,
                                keyProtect = user.keyProtect,
                                dataQuotaMb = user.dataQuotaMb
                            ))
                            importedCount++
                        } catch (e: Exception) {
                            // ignore duplicate registration
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        logout()
                        refreshServerUsers()
                        onResult(true, "Imported $importedCount of ${usersList.size} accounts to server from ${file.name}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Import failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun exportChatToChat(partnerEmail: String, file: File, onResult: (Boolean, String) -> Unit) {
        val user = _loggedInUser.value
        if (user == null) {
            onResult(false, "Not logged in")
            return
        }
        exportUserChatToChat(user.email, partnerEmail, file, onResult)
    }

    fun exportUserChatToChat(userEmail: String, partnerEmail: String, file: File, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val messagesList = messageDao.getChatMessagesList(userEmail, partnerEmail)
                val moshi = com.squareup.moshi.Moshi.Builder().build()
                val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, Message::class.java)
                val adapter = moshi.adapter<List<Message>>(listType)
                val jsonStr = adapter.toJson(messagesList)
                file.writeText(jsonStr, Charsets.UTF_8)
                withContext(Dispatchers.Main) {
                    onResult(true, "Chat exported successfully to ${file.name}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Export failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun importChatFromChat(file: File, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonStr = file.readText(Charsets.UTF_8)
                val moshi = com.squareup.moshi.Moshi.Builder().build()
                val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, Message::class.java)
                val adapter = moshi.adapter<List<Message>>(listType)
                val messagesList = adapter.fromJson(jsonStr) ?: emptyList()
                
                messagesList.forEach { message ->
                    messageDao.insertMessage(message)
                }
                
                withContext(Dispatchers.Main) {
                    onResult(true, "Imported ${messagesList.size} messages successfully from ${file.name}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Import failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun clearRemoteDatabase(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val api = clientManager.getService()
                val response = api.clearDatabase()
                logout() // Log out since all user profiles on server are deleted
                onResult(true, response.message ?: "Database partition cleared successfully")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Failed to clear database partition")
            }
        }
    }
}
