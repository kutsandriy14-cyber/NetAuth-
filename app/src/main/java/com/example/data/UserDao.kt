package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE firstName = :firstName AND lastName = :lastName LIMIT 1")
    suspend fun getUserByFirstAndLastName(firstName: String, lastName: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)

    @Query("SELECT * FROM banned_hardware ORDER BY bannedAt DESC")
    fun getAllBannedHardware(): Flow<List<BannedHardware>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBan(ban: BannedHardware)

    @Delete
    suspend fun deleteBan(ban: BannedHardware)

    @Query("SELECT EXISTS(SELECT 1 FROM banned_hardware WHERE hardwareValue = :value LIMIT 1)")
    suspend fun isHardwareBanned(value: String): Boolean
}
