@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.User
import com.example.data.Message
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

// Sealed class for Navigation routes
sealed class Screen(val route: String) {
    object AccountChooser : Screen("account_chooser")
    object SignIn : Screen("sign_in")
    object RegisterName : Screen("register_name")
    object RegisterBirthGender : Screen("register_birth_gender")
    object RegisterEmail : Screen("register_email")
    object RegisterPassword : Screen("register_password")
    object RegisterContact : Screen("register_contact")
    object RegisterTerms : Screen("register_terms")
    object Dashboard : Screen("dashboard")
}

@Composable
fun GoogleHeader(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String = ""
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // NetAuth stylized modern logo
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Hub,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "NetAuth",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun ConnectionSettingsDialog(
    viewModel: AccountViewModel,
    onDismiss: () -> Unit
) {
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val emailSuffix by viewModel.emailSuffix.collectAsStateWithLifecycle()
    val serviceKey by viewModel.serviceKey.collectAsStateWithLifecycle()
    val activeDatabase by viewModel.activeDatabase.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf(serverUrl) }
    var suffixInput by remember { mutableStateOf(emailSuffix) }
    var serviceKeyInput by remember { mutableStateOf(serviceKey) }
    var dbInput by remember { mutableStateOf(activeDatabase) }
    var newDbNameInput by remember { mutableStateOf("") }

    var pingStatus by remember { mutableStateOf<String?>(null) }
    var isPingSuccessful by remember { mutableStateOf<Boolean?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(0) }
    var importStatus by remember { mutableStateOf("") }
    var manualBanInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Scan downloads folder for .af files
    var afFiles by remember { mutableStateOf(emptyList<java.io.File>()) }
    LaunchedEffect(Unit) {
        afFiles = viewModel.scanLocalFiles(".af")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Client & Database Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Connection", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Rounded.Wifi, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Databases", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Rounded.Storage, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Bans", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            Text(
                                text = "Configure connectivity settings to connect to your remote database or website.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = urlInput,
                                onValueChange = { 
                                    urlInput = it
                                    viewModel.setServerUrl(it)
                                },
                                label = { Text("Server URL / IP") },
                                placeholder = { Text("http://192.168.1.100:8080/") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = suffixInput,
                                onValueChange = { 
                                    suffixInput = it
                                    viewModel.setEmailSuffix(it)
                                },
                                label = { Text("Email Suffix / Domain") },
                                placeholder = { Text("@netauth.lan") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = serviceKeyInput,
                                onValueChange = { 
                                    serviceKeyInput = it
                                    viewModel.setServiceKey(it)
                                },
                                label = { Text("X-Service-Key (API Key)") },
                                placeholder = { Text("my_secure_secret_key") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Button(
                                onClick = {
                                    isChecking = true
                                    pingStatus = "Pinging server..."
                                    viewModel.checkServerStatus { success, message ->
                                        isChecking = false
                                        isPingSuccessful = success
                                        pingStatus = message
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isChecking,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isChecking) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onSecondary)
                                } else {
                                    Text("Test WiFi Server Connection")
                                }
                            }

                            pingStatus?.let { status ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isPingSuccessful == true) 
                                            Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isPingSuccessful == true) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                                            contentDescription = null,
                                            tint = if (isPingSuccessful == true) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = status,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isPingSuccessful == true) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            Text(
                                text = "Database Partition Configuration",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            OutlinedTextField(
                                value = dbInput,
                                onValueChange = { 
                                    dbInput = it
                                    viewModel.setActiveDatabase(it)
                                },
                                label = { Text("X-Database-Name (Active Partition)") },
                                placeholder = { Text("default") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            var availableDbs by remember { mutableStateOf(viewModel.getAvailableDatabases()) }

                            Text(
                                text = "Available Local/Server Partitions:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newDbNameInput,
                                    onValueChange = { newDbNameInput = it },
                                    label = { Text("New Partition") },
                                    placeholder = { Text("e.g. database_2") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Button(
                                    onClick = {
                                        if (newDbNameInput.isNotBlank()) {
                                            viewModel.createNewDatabase(newDbNameInput)
                                            dbInput = viewModel.activeDatabase.value
                                            newDbNameInput = ""
                                            availableDbs = viewModel.getAvailableDatabases()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text("Create")
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                availableDbs.forEach { db ->
                                    val isSelected = db.lowercase() == dbInput.lowercase()
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            dbInput = db
                                            viewModel.setActiveDatabase(db)
                                        },
                                        label = { Text(db) },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text("Offline Profiles Backups (.af)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                            if (importStatus.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(importStatus, modifier = Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val downloadsDir = java.io.File("/storage/emulated/0/Download")
                                        if (!downloadsDir.exists()) downloadsDir.mkdirs()
                                        val file = java.io.File(downloadsDir, "netauth_backup.af")
                                        viewModel.exportAccountsToAf(file) { success, msg ->
                                            importStatus = msg
                                            if (success) {
                                                android.widget.Toast.makeText(context, "Exported successfully to Downloads!", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export .af", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        val scan = viewModel.scanLocalFiles(".af")
                                        if (scan.isNotEmpty()) {
                                            viewModel.importAccountsFromAf(scan.first()) { success, msg ->
                                                importStatus = msg
                                                if (success) {
                                                    android.widget.Toast.makeText(context, "Imported ${scan.first().name} successfully!", android.widget.Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        } else {
                                            importStatus = "No .af backup file detected in Download folder."
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Import .af", fontSize = 11.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    val scan = viewModel.scanLocalFiles(".json")
                                    val serviceFile = scan.find { it.name.contains("service") || it.name.contains("servise") }
                                    if (serviceFile != null) {
                                        viewModel.importServiceKeyFromJson(serviceFile) { success, msg ->
                                            importStatus = msg
                                        }
                                    } else {
                                        importStatus = "No netauth-service.json found in Downloads."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors()
                            ) {
                                Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import netauth-service.json")
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text("Server Maintenance (Обслуживание сервера)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)

                            Button(
                                onClick = {
                                    viewModel.clearRemoteDatabase { success, msg ->
                                        importStatus = msg
                                        if (success) {
                                            android.widget.Toast.makeText(context, "Server Partition Wiped & Logged Out!", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Wipe Active Server Partition (Стереть активную БД)", fontSize = 11.sp)
                            }
                        }
                        2 -> {
                            Text("Hardware Ban Control (Бан по железу)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text("Fully blacklist network identifiers (IP and MAC) to block specific hardware from registering or logging in.", style = MaterialTheme.typography.bodySmall)

                            OutlinedTextField(
                                value = manualBanInput,
                                onValueChange = { manualBanInput = it },
                                label = { Text("IP or MAC Address") },
                                placeholder = { Text("e.g. 192.168.49.50") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (manualBanInput.isNotBlank()) {
                                            viewModel.banHardware(manualBanInput.trim(), "IP")
                                            manualBanInput = ""
                                            android.widget.Toast.makeText(context, "Banned IP Address", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Ban IP", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        if (manualBanInput.isNotBlank()) {
                                            viewModel.banHardware(manualBanInput.trim(), "MAC")
                                            manualBanInput = ""
                                            android.widget.Toast.makeText(context, "Banned MAC Address", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Ban MAC", fontSize = 11.sp)
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            val bans by viewModel.allBannedHardware.collectAsStateWithLifecycle(initialValue = emptyList())
                            Text("Active Hardware Bans (${bans.size}):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                            if (bans.isEmpty()) {
                                Text("No hardware bans registered.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            } else {
                                bans.forEach { ban ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ban.hardwareValue, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                                            Text("Type: ${ban.banType}", style = MaterialTheme.typography.labelSmall)
                                        }
                                        IconButton(onClick = { viewModel.unbanHardware(ban.hardwareValue) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Unban", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.setActiveDatabase(dbInput)
                    onDismiss()
                }
            ) {
                Text("Save & Close")
            }
        }
    )
}

@Composable
fun GoogleCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(24.dp),
        border = CardDefaults.outlinedCardBorder(),
        content = content
    )
}

fun copyToClipboard(context: android.content.Context, text: String, label: String = "Email") {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    android.widget.Toast.makeText(context, "Copied: $text", android.widget.Toast.LENGTH_SHORT).show()
}

@Composable
fun LanguageSwitcher(viewModel: AccountViewModel, modifier: Modifier = Modifier) {
    val lang by viewModel.language.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    val languages = listOf("en" to "English", "uk" to "Українська", "ru" to "Русский")

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag("lang_selector_button"),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = languages.find { it.first == lang }?.second ?: "English",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            languages.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        viewModel.setLanguage(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

// 1. Account Chooser Screen (Clean, Client-Only Login Selection)
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AccountChooserScreen(
    viewModel: AccountViewModel,
    onNavigate: (Screen) -> Unit
) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val discoveryState by viewModel.discoveryState.collectAsStateWithLifecycle()
    val serverUrl by viewModel.serverUrl.collectAsStateWithLifecycle()
    val lang by viewModel.language.collectAsStateWithLifecycle()
    val isScanningServers by viewModel.isScanningServers.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.scanForAllNearbyServers()
    }

    // Action Dialog States for Selected Account
    var selectedActionUser by remember { mutableStateOf<User?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showAddServerDialog by remember { mutableStateOf(false) }
    var newServerUrlInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = viewModel.t("select_account") ?: "Select Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    LanguageSwitcher(viewModel = viewModel)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                GoogleHeader(
                    title = viewModel.t("signin"),
                    subtitle = viewModel.t("select_account")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Auto-Discovery Banner
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (discoveryState) {
                                "searching" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                "found" -> Color(0xFFE8F5E9)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = when (discoveryState) {
                                "searching" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                "found" -> Color(0xFF81C784)
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            when (discoveryState) {
                                "searching" -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = viewModel.t("auto_discovering"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = viewModel.t("scanning_wifi"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                "found" -> {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudDone,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = viewModel.t("connected_local_cloud"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1B5E20)
                                        )
                                        Text(
                                            text = serverUrl,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = viewModel.t("db_disconnected"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = viewModel.t("db_disconnected_desc"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.startAutoDiscovery() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "Scan network",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Saved/Known Servers List
                val savedServers by viewModel.savedServers.collectAsStateWithLifecycle()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (lang == "ru") "Серверы / Подключения:" else "Saved Connections:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    viewModel.scanForAllNearbyServers { count ->
                                        val msg = if (lang == "ru") {
                                            "Найдено серверов: $count"
                                        } else {
                                            "Found $count servers"
                                        }
                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isScanningServers,
                                modifier = Modifier.size(24.dp)
                            ) {
                                if (isScanningServers) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Search Nearby Servers",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { showAddServerDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add Server",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        savedServers.forEach { url ->
                            val isActive = url == serverUrl
                            val cleanUrl = url.removePrefix("http://").removePrefix("https://").removeSuffix("/")
                            FilterChip(
                                selected = isActive,
                                onClick = {
                                    viewModel.setServerUrl(url)
                                },
                                label = { Text(cleanUrl, fontSize = 11.sp, maxLines = 1) },
                                trailingIcon = if (!isActive && url != com.example.data.NetAuthClientManager.DEFAULT_SERVER_URL) {
                                    {
                                        IconButton(
                                            onClick = { viewModel.removeSavedServer(url) },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Delete",
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                } else null
                            )
                        }
                    }
                }

                GoogleCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        if (discoveryState != "found") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = viewModel.t("server_offline_desc"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (users.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountBox,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (discoveryState == "found") viewModel.t("no_accounts_server") else viewModel.t("no_cached_accounts"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = viewModel.t("select_account"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "Hold for actions / Удерживайте",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            users.take(3).forEach { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .combinedClickable(
                                            enabled = discoveryState == "found",
                                            onLongClick = { selectedActionUser = user },
                                            onClick = {
                                                viewModel.selectLoginUser(user)
                                                onNavigate(Screen.SignIn)
                                            }
                                        )
                                        .alpha(if (discoveryState == "found") 1f else 0.5f)
                                        .padding(12.dp)
                                        .testTag("account_item_${user.email}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val resolvedColor = if (user.avatarColor == -12543232 || user.avatarColor == 0) {
                                        val index = kotlin.math.abs(user.email.hashCode()) % viewModel.avatarColors.size
                                        Color(viewModel.avatarColors[index])
                                    } else {
                                        Color(user.avatarColor)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(resolvedColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.firstName.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${user.firstName} ${user.lastName}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = user.email,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = { copyToClipboard(context, user.email) },
                                                modifier = Modifier.size(18.dp),
                                                enabled = discoveryState == "found"
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ContentCopy,
                                                    contentDescription = "Copy Email",
                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = if (discoveryState == "found") 0.7f else 0.3f),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    // Options menu button
                                    IconButton(
                                        onClick = { selectedActionUser = user },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MoreVert,
                                            contentDescription = "Account Actions",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.setLoginEmail("")
                                viewModel.setLoginPassword("")
                                onNavigate(Screen.SignIn)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("use_another_account_button"),
                            colors = ButtonDefaults.outlinedButtonColors(),
                            enabled = true
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(viewModel.t("signin"))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val isLimitReached = users.size >= 3
                        Button(
                            onClick = { onNavigate(Screen.RegisterName) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_account_button"),
                            enabled = !isLimitReached
                        ) {
                            Text(if (isLimitReached) viewModel.t("register_limit_btn") ?: "Register (Max 3 reached)" else viewModel.t("register"))
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        ConnectionSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettings = false }
        )
    }

    if (showAddServerDialog) {
        AlertDialog(
            onDismissRequest = { showAddServerDialog = false },
            title = { Text(if (lang == "ru") "Добавить сервер" else "Add Server Connection") },
            text = {
                OutlinedTextField(
                    value = newServerUrlInput,
                    onValueChange = { newServerUrlInput = it },
                    label = { Text("Server URL / IP") },
                    placeholder = { Text("http://192.168.1.100:8080") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newServerUrlInput.isNotBlank()) {
                            var formatted = newServerUrlInput.trim()
                            if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
                                formatted = "http://$formatted"
                            }
                            if (!formatted.endsWith("/")) {
                                formatted = "$formatted/"
                            }
                            viewModel.addSavedServer(formatted)
                            viewModel.setServerUrl(formatted)
                            showAddServerDialog = false
                            newServerUrlInput = ""
                        }
                    }
                ) {
                    Text(if (lang == "ru") "Добавить" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddServerDialog = false }) {
                    Text(if (lang == "ru") "Отмена" else "Cancel")
                }
            }
        )
    }

    // Action Menu Dialog for selected user - Only "Delete Cached Profile"
    if (selectedActionUser != null) {
        val user = selectedActionUser!!
        AlertDialog(
            onDismissRequest = { selectedActionUser = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val resolvedColor = if (user.avatarColor == -12543232 || user.avatarColor == 0) {
                        val index = kotlin.math.abs(user.email.hashCode()) % viewModel.avatarColors.size
                        Color(viewModel.avatarColors[index])
                    } else {
                        Color(user.avatarColor)
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(resolvedColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.firstName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Actions: ${user.firstName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Delete local profile cache for account: ${user.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Button(
                        onClick = {
                            selectedActionUser = null
                            viewModel.removeLocalAccountCache(user)
                            android.widget.Toast.makeText(context, "Removed profile and cleaned local database cache.", android.widget.Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Cached Profile / Удалить")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedActionUser = null }) {
                    Text("Close / Отмена")
                }
            }
        )
    }
}

// 2. Sign In Screen (Password check)
@Composable
fun SignInScreen(
    viewModel: AccountViewModel,
    onNavigate: (Screen) -> Unit,
    onLoginSuccess: () -> Unit
) {
    val email by viewModel.loginEmail.collectAsStateWithLifecycle()
    val password by viewModel.loginPassword.collectAsStateWithLifecycle()
    val error by viewModel.loginError.collectAsStateWithLifecycle()
    val lang by viewModel.language.collectAsStateWithLifecycle()
    val requireKeyProtect by viewModel.requireKeyProtect.collectAsStateWithLifecycle()
    val keyProtect by viewModel.loginKeyProtect.collectAsStateWithLifecycle()
    val discoveryState by viewModel.discoveryState.collectAsStateWithLifecycle()

    var passwordVisible by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Dialog state
    var resetStep by remember { mutableIntStateOf(1) } // 1: input phone, 2: input code, 3: input new pass
    var inputPhone by remember { mutableStateOf("") }
    var inputCode by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }
    var dialogSuccess by remember { mutableStateOf<String?>(null) }

    val generatedSmsCode by viewModel.resetGeneratedCode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GoogleHeader(
                title = viewModel.t("signin"),
                subtitle = viewModel.t("welcome_subtitle")
            )

            Spacer(modifier = Modifier.height(24.dp))

            GoogleCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { viewModel.setLoginEmail(it) },
                        label = { Text(viewModel.t("enter_email")) },
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { viewModel.setLoginPassword(it) },
                        label = { Text(viewModel.t("enter_password") ?: "Enter password") },
                        leadingIcon = { Icon(Icons.Rounded.VpnKey, contentDescription = null) },
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (requireKeyProtect) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = keyProtect,
                            onValueChange = { viewModel.setLoginKeyProtect(it) },
                            label = { Text("Key Protect") },
                            leadingIcon = { Icon(Icons.Rounded.Security, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("login_key_protect_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            inputPhone = ""
                            inputCode = ""
                            newPass = ""
                            confirmPass = ""
                            dialogError = null
                            dialogSuccess = null
                            resetStep = 1
                            showResetDialog = true
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(viewModel.t("forgot_password"))
                    }

                    if (error != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (error!!.contains("Connection", ignoreCase = true) || error!!.contains("timeout", ignoreCase = true) || error!!.contains("refused", ignoreCase = true) || error!!.contains("unreachable", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = viewModel.t("connection_tip"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { onNavigate(Screen.AccountChooser) }) {
                            Text(viewModel.t("back") ?: "Back")
                        }

                        Button(
                            onClick = {
                                viewModel.performLogin {
                                    onLoginSuccess()
                                }
                            },
                            enabled = true,
                            modifier = Modifier.testTag("login_submit_button")
                        ) {
                            Text(viewModel.t("next") ?: "Next")
                        }
                    }
                }
            }
        }

    }

    // Reset Password Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(viewModel.t("reset_title")) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (dialogError != null) {
                        Text(dialogError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (dialogSuccess != null) {
                        Text(dialogSuccess!!, color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodyMedium)
                    }

                    when (resetStep) {
                        1 -> {
                            Text(viewModel.t("reset_enter_phone"), style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = inputPhone,
                                onValueChange = { inputPhone = it },
                                label = { Text(viewModel.t("phone_field")) },
                                leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                        }
                        2 -> {
                            Text(viewModel.t("reset_enter_code"), style = MaterialTheme.typography.bodyMedium)

                            // Visual SMS message box simulation
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Sms, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Simulated SMS Notification", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        Text("NetAuth Code: ${generatedSmsCode ?: ""}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = inputCode,
                                onValueChange = { inputCode = it },
                                label = { Text(viewModel.t("code_field")) },
                                leadingIcon = { Icon(Icons.Rounded.LockOpen, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        3 -> {
                            Text(viewModel.t("reset_enter_new_pass"), style = MaterialTheme.typography.bodyMedium)
                            OutlinedTextField(
                                value = newPass,
                                onValueChange = { newPass = it },
                                label = { Text("New Password") },
                                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = PasswordVisualTransformation()
                            )
                            OutlinedTextField(
                                value = confirmPass,
                                onValueChange = { confirmPass = it },
                                label = { Text("Confirm New Password") },
                                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = PasswordVisualTransformation()
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        dialogError = null
                        dialogSuccess = null
                        when (resetStep) {
                            1 -> {
                                if (inputPhone.trim().isEmpty()) {
                                    dialogError = "Please enter phone number"
                                } else {
                                    viewModel.sendResetSmsCode(inputPhone) { code ->
                                        resetStep = 2
                                        dialogSuccess = "Code sent via simulated SMS"
                                    }
                                }
                            }
                            2 -> {
                                if (viewModel.verifySmsCode(inputCode)) {
                                    resetStep = 3
                                    dialogSuccess = "Code verified successfully"
                                } else {
                                    dialogError = "Incorrect 6-digit confirmation code"
                                }
                            }
                            3 -> {
                                if (newPass.length < 6) {
                                    dialogError = "Password must be at least 6 characters"
                                } else if (newPass != confirmPass) {
                                    dialogError = "Passwords do not match"
                                } else {
                                    viewModel.performPasswordReset(newPass) {
                                        showResetDialog = false
                                        dialogSuccess = "Password reset successfully!"
                                        android.widget.Toast.makeText(context, "Password changed. You can now login.", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        when (resetStep) {
                            1 -> "Send Code"
                            2 -> "Verify"
                            else -> "Reset Password"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// 3. Register Name Screen
@Composable
fun RegisterNameScreen(
    viewModel: AccountViewModel,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    val firstName by viewModel.regFirstName.collectAsStateWithLifecycle()
    val lastName by viewModel.regLastName.collectAsStateWithLifecycle()
    val error by viewModel.regError.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GoogleHeader(
            title = viewModel.t("register"),
            subtitle = viewModel.t("first_name_label")
        )

        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(progress = { 0.15f }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.height(20.dp))

        GoogleCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { viewModel.setRegNames(it, lastName) },
                    label = { Text(viewModel.t("first_name_label")) },
                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_first_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { viewModel.setRegNames(firstName, it) },
                    label = { Text(viewModel.t("last_name_label")) },
                    leadingIcon = { Icon(Icons.Rounded.PersonOutline, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_last_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Text(viewModel.t("signin"))
                    }

                    Button(
                        onClick = {
                            if (viewModel.validateNames()) {
                                viewModel.generateSuggestions()
                                onNavigate(Screen.RegisterBirthGender)
                            }
                        },
                        modifier = Modifier.testTag("reg_name_next_button")
                    ) {
                        Text(viewModel.t("next"))
                    }
                }
            }
        }
    }
}

// 4. Register Birth & Gender Screen
@Composable
fun RegisterBirthGenderScreen(
    viewModel: AccountViewModel,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    val day by viewModel.regBirthDay.collectAsStateWithLifecycle()
    val month by viewModel.regBirthMonth.collectAsStateWithLifecycle()
    val year by viewModel.regBirthYear.collectAsStateWithLifecycle()
    val gender by viewModel.regGender.collectAsStateWithLifecycle()
    val error by viewModel.regError.collectAsStateWithLifecycle()

    val months = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val genders = listOf("Female", "Male", "Rather not say", "Custom")

    var monthExpanded by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GoogleHeader(
            title = viewModel.t("personal_info"),
            subtitle = viewModel.t("birth_date")
        )

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(progress = { 0.3f }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.height(20.dp))

        GoogleCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(viewModel.t("birthday"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Day
                    OutlinedTextField(
                        value = day,
                        onValueChange = { viewModel.setRegBirthInfo(it, month, year, gender) },
                        label = { Text(viewModel.t("day")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reg_birth_day"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Month Dropdown
                    Box(modifier = Modifier.weight(1.5f)) {
                        OutlinedButton(
                            onClick = { monthExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("reg_birth_month_dropdown"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(month, color = MaterialTheme.colorScheme.onSurface)
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        DropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false }
                        ) {
                            months.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m) },
                                    onClick = {
                                        viewModel.setRegBirthInfo(day, m, year, gender)
                                        monthExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Year
                    OutlinedTextField(
                        value = year,
                        onValueChange = { viewModel.setRegBirthInfo(day, month, it, gender) },
                        label = { Text(viewModel.t("year")) },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("reg_birth_year"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(viewModel.t("gender"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { genderExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("reg_gender_dropdown"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(gender, color = MaterialTheme.colorScheme.onSurface)
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    DropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false }
                    ) {
                        genders.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g) },
                                onClick = {
                                    viewModel.setRegBirthInfo(day, month, year, g)
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Text(viewModel.t("back"))
                    }

                    Button(
                        onClick = {
                            if (viewModel.validateBirthInfo()) {
                                onNavigate(Screen.RegisterEmail)
                            }
                        },
                        modifier = Modifier.testTag("reg_birth_next_button")
                    ) {
                        Text(viewModel.t("next"))
                    }
                }
            }
        }
    }
}

// 5. Register Email Screen
@Composable
fun RegisterEmailScreen(
    viewModel: AccountViewModel,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    val suggestions by viewModel.emailSuggestions.collectAsStateWithLifecycle()
    val selectedOption by viewModel.regEmailOption.collectAsStateWithLifecycle()
    val customEmail by viewModel.regCustomEmail.collectAsStateWithLifecycle()
    val error by viewModel.regError.collectAsStateWithLifecycle()

    val emailSuffix by viewModel.emailSuffix.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GoogleHeader(
            title = viewModel.t("choose_address_title"),
            subtitle = viewModel.t("choose_address_desc")
        )

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(progress = { 0.5f }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.height(20.dp))

        GoogleCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // List of suggested emails
                suggestions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setRegEmailOption(option) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOption == option),
                            onClick = { viewModel.setRegEmailOption(option) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(option, style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                // Custom email option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.setRegEmailOption("custom") }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedOption == "custom"),
                        onClick = { viewModel.setRegEmailOption("custom") }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(viewModel.t("custom_email_desc"), style = MaterialTheme.typography.bodyLarge)
                }

                AnimatedVisibility(visible = selectedOption == "custom") {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = customEmail,
                            onValueChange = { viewModel.setRegCustomEmail(it) },
                            label = { Text(viewModel.t("custom_email_label")) },
                            placeholder = { Text("username$emailSuffix") },
                            leadingIcon = { Icon(Icons.Rounded.AlternateEmail, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reg_custom_email_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Text(viewModel.t("back"))
                    }

                    Button(
                        onClick = {
                            if (viewModel.validateEmailStep()) {
                                onNavigate(Screen.RegisterPassword)
                            }
                        },
                        modifier = Modifier.testTag("reg_email_next_button")
                    ) {
                        Text(viewModel.t("next"))
                    }
                }
            }
        }
    }
}

// 6. Register Password Screen
@Composable
fun RegisterPasswordScreen(
    viewModel: AccountViewModel,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    val password by viewModel.regPassword.collectAsStateWithLifecycle()
    val confirmPassword by viewModel.regConfirmPassword.collectAsStateWithLifecycle()
    val error by viewModel.regError.collectAsStateWithLifecycle()

    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GoogleHeader(
            title = viewModel.t("new_password_req"),
            subtitle = ""
        )

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(progress = { 0.65f }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.height(20.dp))

        GoogleCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { viewModel.setRegPassword(it, confirmPassword) },
                    label = { Text(viewModel.t("enter_password")) },
                    leadingIcon = { Icon(Icons.Rounded.VpnKey, contentDescription = null) },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_password_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { viewModel.setRegPassword(password, it) },
                    label = { Text(viewModel.t("confirm")) },
                    leadingIcon = { Icon(Icons.Rounded.VpnKey, contentDescription = null) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_confirm_password_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Text(viewModel.t("back"))
                    }

                    Button(
                        onClick = {
                            if (viewModel.validatePasswordStep()) {
                                onNavigate(Screen.RegisterContact)
                            }
                        },
                        modifier = Modifier.testTag("reg_password_next_button")
                    ) {
                        Text(viewModel.t("next"))
                    }
                }
            }
        }
    }
}

// 7. Register Contact Info Screen (Phone & Recovery Email)
@Composable
fun RegisterContactScreen(
    viewModel: AccountViewModel,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit
) {
    val phone by viewModel.regPhoneNumber.collectAsStateWithLifecycle()
    val recovery by viewModel.regRecoveryEmail.collectAsStateWithLifecycle()
    val error by viewModel.regError.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GoogleHeader(
            title = viewModel.t("security_recommendations"),
            subtitle = ""
        )

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(progress = { 0.8f }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.height(20.dp))

        GoogleCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { viewModel.setRegContactInfo(it, recovery) },
                    label = { Text(viewModel.t("phone_optional")) },
                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                    placeholder = { Text("+1 555-0100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_phone_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = recovery,
                    onValueChange = { viewModel.setRegContactInfo(phone, it) },
                    label = { Text(viewModel.t("recovery_email_optional")) },
                    leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                    placeholder = { Text("recovery@example.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_recovery_email_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onNavigate(Screen.RegisterTerms) }) {
                        Text(viewModel.t("skip"))
                    }

                    Row {
                        TextButton(onClick = onBack) {
                            Text(viewModel.t("back"))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (recovery.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(recovery).matches()) {
                                    viewModel.setRegContactInfo(phone, recovery)
                                }
                                onNavigate(Screen.RegisterTerms)
                            },
                            modifier = Modifier.testTag("reg_contact_next_button")
                        ) {
                            Text(viewModel.t("next"))
                        }
                    }
                }
            }
        }
    }
}

// 8. Register Terms & Privacy Screen
@Composable
fun RegisterTermsScreen(
    viewModel: AccountViewModel,
    onNavigate: (Screen) -> Unit,
    onBack: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val error by viewModel.regError.collectAsStateWithLifecycle()
    val discoveryState by viewModel.discoveryState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GoogleHeader(
            title = viewModel.t("privacy_terms_title"),
            subtitle = viewModel.t("privacy_terms_desc")
        )

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(progress = { 1.0f }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.height(20.dp))

        GoogleCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = viewModel.t("agreement_title"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = viewModel.t("agreement_body"),
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp
                        )
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (error!!.contains("Connection", ignoreCase = true) || error!!.contains("timeout", ignoreCase = true) || error!!.contains("refused", ignoreCase = true) || error!!.contains("unreachable", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = viewModel.t("connection_tip"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Text(viewModel.t("back"))
                    }

                    Button(
                        onClick = {
                            viewModel.performRegistration {
                                onRegisterSuccess()
                            }
                        },
                        enabled = true,
                        modifier = Modifier.testTag("reg_terms_agree_button")
                    ) {
                        Text(viewModel.t("i_agree"))
                    }
                }
            }
        }
    }
}

// 9. Main Google Dashboard Screen
@Composable
fun DashboardScreen(
    viewModel: AccountViewModel,
    onSignOut: () -> Unit
) {
    val user by viewModel.loggedInUser.collectAsStateWithLifecycle()
    val lang by viewModel.language.collectAsStateWithLifecycle()
    var activeTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(user) {
        if (user == null) {
            onSignOut()
        }
    }

    // Guard if user is null
    if (user == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }
    val u = user!!

    val currentContext = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val resolvedColor = if (u.avatarColor == -12543232 || u.avatarColor == 0) {
                            val index = kotlin.math.abs(u.email.hashCode()) % viewModel.avatarColors.size
                            Color(viewModel.avatarColors[index])
                        } else {
                            Color(u.avatarColor)
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(resolvedColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = u.firstName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NetAuth Account",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.logout(); onSignOut() },
                        modifier = Modifier.testTag("dashboard_signout_button")
                    ) {
                        Icon(Icons.Rounded.ExitToApp, contentDescription = "Sign out")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    modifier = Modifier.testTag("tab_home")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Rounded.Person, contentDescription = "Personal Info") },
                    label = { Text(viewModel.t("tab_profile")) },
                    modifier = Modifier.testTag("tab_personal_info")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Rounded.Security, contentDescription = "Security") },
                    label = { Text(viewModel.t("tab_security")) },
                    modifier = Modifier.testTag("tab_security")
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Rounded.Chat, contentDescription = viewModel.t("tab_messages")) },
                    label = { Text(viewModel.t("tab_messages")) },
                    modifier = Modifier.testTag("tab_messages")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                0 -> DashboardHomeTab(user = user!!, viewModel = viewModel, onNavigateToTab = { activeTab = it })
                1 -> DashboardPersonalInfoTab(user = user!!, viewModel = viewModel)
                2 -> DashboardSecurityTab(user = user!!, viewModel = viewModel, onSignOut = onSignOut)
                3 -> DashboardMessagesTab(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DashboardHomeTab(
    user: User,
    viewModel: AccountViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val files by viewModel.userFiles.collectAsStateWithLifecycle()
    var showStorageDialog by remember { mutableStateOf(false) }

    val totalSize = files.sumOf { it.size }
    val totalSizeStr = formatSize(totalSize)
    val quotaMb = if (user.dataQuotaMb > 0) user.dataQuotaMb else 512
    val maxBytes = quotaMb * 1024 * 1024L
    val usePct = (totalSize.toFloat() / maxBytes.toFloat()).coerceIn(0f, 1f)
    val pctStr = String.format("%.4f%%", usePct * 100)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Big avatar
        val resolvedColor = if (user.avatarColor == -12543232 || user.avatarColor == 0) {
            val index = kotlin.math.abs(user.email.hashCode()) % viewModel.avatarColors.size
            Color(viewModel.avatarColors[index])
        } else {
            Color(user.avatarColor)
        }
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(resolvedColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.firstName.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome, ${user.firstName}!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Info card 1
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { onNavigateToTab(1) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = Color(0xFF1A73E8),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = viewModel.t("personal_info"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = viewModel.t("personal_info_desc"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null)
            }
        }

        // Info card 2
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { onNavigateToTab(2) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = Color(0xFFEA4335),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = viewModel.t("security_recommendations"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = viewModel.t("security_desc"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null)
            }
        }

        // Cloud Server Storage Indicator (512 MB allocated per account)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { showStorageDialog = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudQueue,
                        contentDescription = null,
                        tint = Color(0xFF34A853),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = viewModel.t("cloud_storage"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = viewModel.t("cloud_storage_desc"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { usePct.coerceAtLeast(0.001f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF34A853),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$totalSizeStr of $quotaMb MB used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$pctStr used",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF34A853)
                    )
                }
            }
        }
        
        if (showStorageDialog) {
            ServerStorageManagerDialog(viewModel = viewModel, onDismiss = { showStorageDialog = false })
        }
    }
}

@Composable
fun DashboardPersonalInfoTab(
    user: User,
    viewModel: AccountViewModel
) {
    var editMode by remember { mutableStateOf(false) }

    var draftFirstName by remember(user) { mutableStateOf(user.firstName) }
    var draftLastName by remember(user) { mutableStateOf(user.lastName) }
    var draftBirthDate by remember(user) { mutableStateOf(user.birthDate) }
    var draftGender by remember(user) { mutableStateOf(user.gender) }
    var draftPhone by remember(user) { mutableStateOf(user.phoneNumber) }
    var draftRecovery by remember(user) { mutableStateOf(user.recoveryEmail) }

    var updateError by remember { mutableStateOf<String?>(null) }
    var updateSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Personal info",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium
            )

            Button(
                onClick = {
                    if (editMode) {
                        // Save changes
                        viewModel.updateProfile(
                            firstName = draftFirstName,
                            lastName = draftLastName,
                            birthDate = draftBirthDate,
                            gender = draftGender,
                            phoneNumber = draftPhone,
                            recoveryEmail = draftRecovery,
                            onSuccess = {
                                editMode = false
                                updateSuccess = true
                                updateError = null
                            },
                            onError = {
                                updateError = it
                                updateSuccess = false
                            }
                        )
                    } else {
                        editMode = true
                        updateSuccess = false
                    }
                },
                modifier = Modifier.testTag("edit_profile_toggle_button")
            ) {
                Icon(
                    imageVector = if (editMode) Icons.Rounded.Save else Icons.Rounded.Edit,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (editMode) "Save" else "Edit")
            }
        }

        Text(
            text = "Info about you and your preferences in NetAuth Client services",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (updateSuccess) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = "Profile updated successfully!",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        if (updateError != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = updateError!!,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        GoogleCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Basic info",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (editMode) {
                    OutlinedTextField(
                        value = draftFirstName,
                        onValueChange = { draftFirstName = it },
                        label = { Text("First name") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("edit_first_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = draftLastName,
                        onValueChange = { draftLastName = it },
                        label = { Text("Last name") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("edit_last_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = draftBirthDate,
                        onValueChange = { draftBirthDate = it },
                        label = { Text("Birthday (YYYY-Month-DD)") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = draftGender,
                        onValueChange = { draftGender = it },
                        label = { Text("Gender") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    InfoRow(label = "NAME", value = "${user.firstName} ${user.lastName}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(label = "BIRTHDAY", value = user.birthDate)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(label = "GENDER", value = user.gender)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GoogleCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Contact info",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                InfoRow(label = "EMAIL", value = user.email)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                if (editMode) {
                    OutlinedTextField(
                        value = draftPhone,
                        onValueChange = { draftPhone = it },
                        label = { Text("Phone number") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = draftRecovery,
                        onValueChange = { draftRecovery = it },
                        label = { Text("Recovery email") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    InfoRow(label = "PHONE", value = user.phoneNumber.ifEmpty { "Not specified" })
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    InfoRow(label = "RECOVERY EMAIL", value = user.recoveryEmail.ifEmpty { "Not specified" })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        GoogleCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Device Access Security",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                InfoRow(label = "KEY PROTECT", value = user.keyProtect.ifEmpty { "Not Generated" })
                Text(
                    text = "Use this key to authorize access from a new device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(label = "MAC ADDRESS", value = user.macAddress.ifEmpty { "Unknown" })
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(label = "LAST IP ADDRESS", value = user.ipAddress.ifEmpty { "Unknown" })
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                InfoRow(label = "DATA QUOTA", value = "${user.dataQuotaMb} MB")
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(2f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun DashboardSecurityTab(
    user: User,
    viewModel: AccountViewModel,
    onSignOut: () -> Unit
) {
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var newPassword by remember { mutableStateOf("") }
    var actionError by remember { mutableStateOf<String?>(null) }
    var actionSuccess by remember { mutableStateOf<String?>(null) }

    // Passcode lock states
    val passcode by viewModel.appPasscode.collectAsStateWithLifecycle()
    val lang by viewModel.language.collectAsStateWithLifecycle()
    var showPasscodeSetup by remember { mutableStateOf(false) }
    var setupPasscodeInput by remember { mutableStateOf("") }
    var setupPasscodeConfirm by remember { mutableStateOf("") }
    var setupPasscodeError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = viewModel.t("tab_security") ?: "Security",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (actionSuccess != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = actionSuccess!!,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        GoogleCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Signing in to NetAuth",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showPasswordDialog = true; actionSuccess = null; actionError = null }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Password", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("Change local database password safely", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App Passcode Protection
        GoogleCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = viewModel.t("passcode_title"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewModel.t("enable_passcode"),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (passcode.isNotEmpty()) "Locked with active passcode" else "No passcode lock set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = passcode.isNotEmpty(),
                        onCheckedChange = { checked ->
                            if (checked) {
                                setupPasscodeInput = ""
                                setupPasscodeConfirm = ""
                                setupPasscodeError = null
                                showPasscodeSetup = true
                            } else {
                                viewModel.setAppPasscode("")
                                actionSuccess = "App passcode lock disabled"
                            }
                        }
                    )
                }

                if (passcode.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            setupPasscodeInput = ""
                            setupPasscodeConfirm = ""
                            setupPasscodeError = null
                            showPasscodeSetup = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(viewModel.t("change_passcode"))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GoogleCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Danger zone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDeleteDialog = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Delete your local account", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                        Text("Irreversibly erase your details from local SQLite storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Change Password Dialog
        if (showPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showPasswordDialog = false },
                title = { Text("Change Password") },
                text = {
                    Column {
                        Text("Enter a new strong password (minimum 6 characters):", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password") },
                            modifier = Modifier.fillMaxWidth().testTag("new_password_dialog_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (actionError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(actionError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updatePassword(
                                newPass = newPassword,
                                onSuccess = {
                                    showPasswordDialog = false
                                    actionSuccess = "Password updated successfully!"
                                    newPassword = ""
                                    actionError = null
                                },
                                onError = {
                                    actionError = it
                                }
                            )
                        },
                        modifier = Modifier.testTag("dialog_password_confirm_button")
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPasswordDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // App Passcode Setup Dialog
        if (showPasscodeSetup) {
            AlertDialog(
                onDismissRequest = { showPasscodeSetup = false },
                title = { Text(viewModel.t("passcode_title") ?: "Setup Passcode") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Set a 4-digit numeric passcode to protect your application access.")
                        
                        OutlinedTextField(
                            value = setupPasscodeInput,
                            onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 4) setupPasscodeInput = it },
                            label = { Text("4-digit Passcode") },
                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = setupPasscodeConfirm,
                            onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 4) setupPasscodeConfirm = it },
                            label = { Text("Confirm Passcode") },
                            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (setupPasscodeError != null) {
                            Text(setupPasscodeError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (setupPasscodeInput.length != 4) {
                                setupPasscodeError = "Passcode must be exactly 4 digits"
                            } else if (setupPasscodeInput != setupPasscodeConfirm) {
                                setupPasscodeError = "Passcodes do not match"
                            } else {
                                viewModel.setAppPasscode(setupPasscodeInput)
                                showPasscodeSetup = false
                                actionSuccess = "App passcode updated successfully!"
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPasscodeSetup = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Permanent Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Account permanently?") },
                text = { Text("Are you sure you want to permanently delete your account? This will erase your profile, files, and chat messages from both the server database and this device local database. This action is irreversible.") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            viewModel.deleteAccount {
                                showDeleteDialog = false
                                onSignOut()
                            }
                        }
                    ) {
                        Text("Yes, Delete permanently")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// 10. Dashboard Messenger Tab
@Composable
fun DashboardMessagesTab(
    viewModel: AccountViewModel
) {
    val user by viewModel.loggedInUser.collectAsStateWithLifecycle()
    if (user == null) return

    val chatPartners by viewModel.getChatPartnersFlow().collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedPartner by remember { mutableStateOf<String?>(null) }
    var showNewChatDialog by remember { mutableStateOf(false) }
    var newChatEmail by remember { mutableStateOf("") }
    var newChatError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    if (selectedPartner == null) {
        // Chat List View
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = viewModel.t("tab_messages") ?: "Messages",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (chatPartners.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "No active conversations",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Tap the button below to start messaging.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chatPartners) { partner ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPartner = partner },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = partner.take(1).uppercase(),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = partner,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Tap to chat",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteChat(partner)
                                            android.widget.Toast.makeText(context, "Chat deleted", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Delete chat",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        newChatEmail = ""
                        newChatError = null
                        showNewChatDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.AddComment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Message")
                }
            }
        }
    } else {
        // Chat Thread View
        val messages by viewModel.getMessagesForPartner(selectedPartner!!).collectAsStateWithLifecycle(initialValue = emptyList())
        var textToSend by remember { mutableStateOf("") }
        val listState = rememberLazyListState()

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedPartner = null }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedPartner!!.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedPartner!!,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Active Conversation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                var showChatMenu by remember { mutableStateOf(false) }
                val blockedUsers by viewModel.blockedUsers.collectAsStateWithLifecycle()
                val isBlocked = blockedUsers.contains(selectedPartner!!.trim().lowercase())

                Box {
                    IconButton(onClick = { showChatMenu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More chat options")
                    }
                    DropdownMenu(
                        expanded = showChatMenu,
                        onDismissRequest = { showChatMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isBlocked) "Unblock user" else "Block user") },
                            onClick = {
                                if (isBlocked) {
                                    viewModel.unblockUser(selectedPartner!!)
                                } else {
                                    viewModel.blockUser(selectedPartner!!)
                                    selectedPartner = null
                                }
                                showChatMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    if (isBlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                                    contentDescription = null
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Delete local chat") },
                            onClick = {
                                viewModel.deleteChat(selectedPartner!!)
                                selectedPartner = null
                                showChatMenu = false
                            },
                            leadingIcon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            HorizontalDivider()

            // Message Bubble list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val isMine = message.senderEmail == user!!.email
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Card(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMine) 16.dp else 4.dp,
                                bottomEnd = if (isMine) 4.dp else 16.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = message.text,
                                    color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val formattedTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(message.timestamp))
                                    Text(
                                        text = formattedTime,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteMessage(message.id) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete message",
                                            tint = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = textToSend,
                    onValueChange = { textToSend = it },
                    placeholder = { Text("Write message...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (textToSend.trim().isNotEmpty()) {
                            viewModel.sendMessage(
                                recipientEmail = selectedPartner!!,
                                text = textToSend.trim(),
                                onResult = { success, error ->
                                    if (!success) {
                                        android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                            textToSend = ""
                        }
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }

    // New Chat dialog
    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text("New Message") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Enter the registered NetAuth email address of the person you want to message:")
                    OutlinedTextField(
                        value = newChatEmail,
                        onValueChange = { newChatEmail = it },
                        label = { Text("Recipient Email") },
                        leadingIcon = { Icon(Icons.Rounded.AlternateEmail, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (newChatError != null) {
                        Text(newChatError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newChatEmail.trim().isEmpty()) {
                            newChatError = "Please enter an email"
                        } else if (newChatEmail.trim().lowercase() == user!!.email.lowercase()) {
                            newChatError = "You cannot send messages to yourself"
                        } else {
                            viewModel.sendMessage(
                                recipientEmail = newChatEmail.trim(),
                                text = "Hello! Let's chat.",
                                onResult = { success, error ->
                                    if (!success) {
                                        newChatError = error
                                    } else {
                                        selectedPartner = newChatEmail.trim()
                                        showNewChatDialog = false
                                    }
                                }
                            )
                        }
                    }
                ) {
                    Text("Start Conversation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// 11. App Passcode Lock Overlay
@Composable
fun PasscodeLockScreen(
    viewModel: AccountViewModel,
    onUnlock: () -> Unit
) {
    val passcode by viewModel.appPasscode.collectAsStateWithLifecycle()
    val lang by viewModel.language.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = viewModel.t("passcode_enter") ?: "Enter Passcode",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Dots representing entered code length
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val filled = index < input.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMsg != null) {
            Text(
                text = errorMsg!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Custom Numeric Keyboard
        val buttons = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "Delete")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            buttons.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    row.forEach { label ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    errorMsg = null
                                    when (label) {
                                        "C" -> input = ""
                                        "Delete" -> if (input.isNotEmpty()) {
                                            input = input.dropLast(1)
                                        }
                                        else -> {
                                            if (input.length < 4) {
                                                input += label
                                                if (input.length == 4) {
                                                    if (input == passcode) {
                                                        onUnlock()
                                                    } else {
                                                        errorMsg = viewModel.t("passcode_error") ?: "Incorrect Passcode"
                                                        input = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (label == "Delete") {
                                Icon(Icons.Rounded.Backspace, contentDescription = "Delete")
                            } else {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServerStorageManagerDialog(
    viewModel: AccountViewModel,
    onDismiss: () -> Unit
) {
    val files by viewModel.userFiles.collectAsStateWithLifecycle()
    val isLoading by viewModel.isStorageLoading.collectAsStateWithLifecycle()
    
    var showUploadDialog by remember { mutableStateOf(false) }
    var uploadName by remember { mutableStateOf("") }
    var uploadContent by remember { mutableStateOf("") }
    
    var selectedFileContent by remember { mutableStateOf<String?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadUserFiles()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CloudQueue,
                    contentDescription = null,
                    tint = Color(0xFF34A853),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cloud Folder Files")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "These files are physically stored on the server inside your account directory. Swipe, view, or manage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF34A853))
                    }
                } else if (files.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No files uploaded yet. Create your first text file below!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(files) { file ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isDownloading = true
                                        coroutineScope.launch {
                                            try {
                                                val user = viewModel.loggedInUser.value ?: return@launch
                                                val client = okhttp3.OkHttpClient()
                                                val serverUrl = viewModel.serverUrl.value
                                                val requestUrl = if (serverUrl.endsWith("/")) {
                                                    "${serverUrl}api/users/${user.id}/storage/${java.net.URLEncoder.encode(file.name, "UTF-8")}"
                                                } else {
                                                    "$serverUrl/api/users/${user.id}/storage/${java.net.URLEncoder.encode(file.name, "UTF-8")}"
                                                }
                                                val req = okhttp3.Request.Builder()
                                                    .url(requestUrl)
                                                    .addHeader("X-Database-Name", viewModel.activeDatabase.value)
                                                    .build()
                                                withContext(Dispatchers.IO) {
                                                    client.newCall(req).execute().use { response ->
                                                        if (response.isSuccessful) {
                                                            val body = response.body?.string() ?: ""
                                                            withContext(Dispatchers.Main) {
                                                                selectedFileName = file.name
                                                                selectedFileContent = body
                                                            }
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                // ignore
                                            } finally {
                                                isDownloading = false
                                            }
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Article,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${formatSize(file.size)} • ${formatDate(file.updatedAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteUserFile(file.name) { success, msg ->
                                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Delete File",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
                Button(
                    onClick = { showUploadDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853))
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload File")
                }
            }
        }
    )

    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = { showUploadDialog = false },
            title = { Text("Upload Text File") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uploadName,
                        onValueChange = { uploadName = it },
                        label = { Text("File Name (e.g., config.json)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uploadContent,
                        onValueChange = { uploadContent = it },
                        label = { Text("File Content") },
                        modifier = Modifier.fillMaxWidth().height(140.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (uploadName.trim().isNotEmpty()) {
                            viewModel.uploadUserFile(uploadName.trim(), uploadContent) { success, msg ->
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                if (success) {
                                    showUploadDialog = false
                                    uploadName = ""
                                    uploadContent = ""
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853))
                ) {
                    Text("Upload")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (selectedFileContent != null) {
        AlertDialog(
            onDismissRequest = { selectedFileContent = null; selectedFileName = null },
            title = { Text(selectedFileName ?: "File Content") },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = selectedFileContent ?: "",
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { selectedFileContent = null; selectedFileName = null }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

fun formatDate(millis: Long): String {
    val date = java.util.Date(millis)
    val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
    return sdf.format(date)
}
