package com.example.aplicatiegestiunefirma.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ChatMessage message);

    @Update
    void update(ChatMessage message);

    @Delete
    void delete(ChatMessage message);

    @Query("SELECT * FROM chat_messages WHERE employeeId = :employeeId ORDER BY timestamp ASC")
    List<ChatMessage> getMessagesByEmployee(int employeeId);

    @Query("SELECT * FROM chat_messages WHERE employeeId = :employeeId AND isRead = 0 AND isFromAdmin = 1")
    List<ChatMessage> getUnreadAdminMessages(int employeeId);

    @Query("SELECT DISTINCT employeeId, employeeName FROM chat_messages ORDER BY timestamp DESC")
    List<ConversationPreview> getConversations();

    @Query("UPDATE chat_messages SET isRead = 1 WHERE employeeId = :employeeId")
    void markAllAsRead(int employeeId);

    @Query("SELECT COUNT(*) FROM chat_messages WHERE isRead = 0 AND isFromAdmin = 0")
    int getUnreadCountForAdmin();
}
