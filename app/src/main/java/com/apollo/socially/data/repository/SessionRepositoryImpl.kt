package com.apollo.socially.data.repository

import com.apollo.socially.data.local.database.UserSessionDao
import com.apollo.socially.data.local.database.UserSessionEntity
import com.apollo.socially.domain.model.User
import com.apollo.socially.domain.repository.ISessionRepository

class SessionRepositoryImpl(
    private val userSessionDao: UserSessionDao
) : ISessionRepository {

    override suspend fun saveUserSession(user: User) {
        val session = UserSessionEntity(
            uid = user.uid,
            email = user.email,
            displayName = user.displayName,
            username = user.username,
            bio = user.bio,
            profileImageUrl = user.profileImageUrl,
            profileCoverUrl = user.profileCoverUrl,
            isLoggedIn = true,
            lastLoginTimestamp = System.currentTimeMillis()
        )
        userSessionDao.insertUserSession(session)
    }

    override suspend fun getUserSession(): UserSessionEntity? {
        return userSessionDao.getLoggedInUser()
    }

    override suspend fun isUserLoggedIn(): Boolean {
        return userSessionDao.isUserLoggedIn()
    }

    override suspend fun clearSession() {
        userSessionDao.clearAllSessions()
    }
}
