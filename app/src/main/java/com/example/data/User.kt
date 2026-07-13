package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val email: String,
    val passwordHash: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String,
    val gender: String,
    val avatarColor: Int,
    val phoneNumber: String = "",
    val recoveryEmail: String = "",
    val ipAddress: String = "",
    val macAddress: String = "",
    val keyProtect: String = "",
    val dataQuotaMb: Int = 200,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "banned_hardware")
data class BannedHardware(
    @PrimaryKey val hardwareValue: String,
    val banType: String = "IP", // "IP" or "MAC"
    val bannedAt: Long = System.currentTimeMillis()
)

