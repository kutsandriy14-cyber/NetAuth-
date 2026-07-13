package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderEmail: String,
    val receiverEmail: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE (senderEmail = :user1 AND receiverEmail = :user2) OR (senderEmail = :user2 AND receiverEmail = :user1) ORDER BY timestamp ASC")
    fun getChatMessages(user1: String, user2: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE (senderEmail = :user1 AND receiverEmail = :user2) OR (senderEmail = :user2 AND receiverEmail = :user1) ORDER BY timestamp ASC")
    suspend fun getChatMessagesList(user1: String, user2: String): List<Message>

    @Query("SELECT DISTINCT senderEmail FROM messages WHERE receiverEmail = :userEmail UNION SELECT DISTINCT receiverEmail FROM messages WHERE senderEmail = :userEmail")
    fun getChatPartners(userEmail: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("DELETE FROM messages WHERE (senderEmail = :user1 AND receiverEmail = :user2) OR (senderEmail = :user2 AND receiverEmail = :user1)")
    suspend fun deleteChat(user1: String, user2: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Int)
}
