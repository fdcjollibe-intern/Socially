package com.apollo.socially.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserSessionDao {
    
    @Query("SELECT * FROM user_session WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getLoggedInUser(): UserSessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSession(session: UserSessionEntity)
    
    @Query("UPDATE user_session SET isLoggedIn = 0 WHERE uid = :uid")
    suspend fun logout(uid: String)
    
    @Query("DELETE FROM user_session")
    suspend fun clearAllSessions()
    
    @Query("SELECT EXISTS(SELECT 1 FROM user_session WHERE isLoggedIn = 1 LIMIT 1)")
    suspend fun isUserLoggedIn(): Boolean
}

