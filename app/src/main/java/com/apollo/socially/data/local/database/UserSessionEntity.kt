package com.apollo.socially.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_session")
data class UserSessionEntity(
    @PrimaryKey(autoGenerate = false)
    val uid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val profileImageUrl: String?,
    val isLoggedIn: Boolean = true,
    val lastLoginTimestamp: Long = System.currentTimeMillis()
)

