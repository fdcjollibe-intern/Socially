package com.apollo.socially.domain.repository

import com.apollo.socially.data.local.database.UserSessionEntity
import com.apollo.socially.domain.model.User

interface ISessionRepository {
    suspend fun saveUserSession(user: User)
    suspend fun getUserSession(): UserSessionEntity?
    suspend fun isUserLoggedIn(): Boolean
    suspend fun clearSession()
}

