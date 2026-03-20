package com.apollo.socially.data.cache

import com.apollo.socially.domain.model.Post
import com.apollo.socially.domain.model.User

/**
 * In-memory cache for profile data to avoid unnecessary reloads
 */
object ProfileCache {
    
    private var cachedUser: User? = null
    private var cachedPosts: MutableList<Post> = mutableListOf()
    private var lastCacheTime: Long = 0
    private var cachedUserId: String? = null
    
    // Cache valid for 10 minutes
    private const val CACHE_VALIDITY_MS = 10 * 60 * 1000L
    
    fun getUserCache(userId: String): User? {
        if (cachedUserId != userId) return null
        if (!isCacheValid()) return null
        return cachedUser
    }
    
    fun getPostsCache(userId: String): List<Post>? {
        if (cachedUserId != userId) return null
        if (!isCacheValid()) return null
        return cachedPosts.toList()
    }
    
    fun cacheUserData(userId: String, user: User, posts: List<Post>) {
        cachedUserId = userId
        cachedUser = user
        cachedPosts = posts.toMutableList()
        lastCacheTime = System.currentTimeMillis()
    }
    
    fun addPostToCache(userId: String, post: Post) {
        if (cachedUserId == userId) {
            // Add to beginning since posts are sorted by date DESC
            cachedPosts.add(0, post)
            lastCacheTime = System.currentTimeMillis()
        }
    }
    
    fun getNewPostCount(userId: String, currentCount: Int): Int {
        if (cachedUserId != userId) return 0
        val cachedCount = cachedPosts.size
        return maxOf(0, currentCount - cachedCount)
    }
    
    fun removePostFromCache(userId: String, postId: String) {
        if (cachedUserId == userId) {
            cachedPosts.removeAll { it.id == postId }
            lastCacheTime = System.currentTimeMillis()
        }
    }
    
    fun updateUserInCache(userId: String, user: User) {
        if (cachedUserId == userId) {
            cachedUser = user
            lastCacheTime = System.currentTimeMillis()
        }
    }
    
    fun invalidateCache() {
        cachedUser = null
        cachedPosts.clear()
        lastCacheTime = 0
        cachedUserId = null
    }
    
    fun invalidateCacheForUser(userId: String) {
        if (cachedUserId == userId) {
            invalidateCache()
        }
    }
    
    private fun isCacheValid(): Boolean {
        val now = System.currentTimeMillis()
        return (now - lastCacheTime) < CACHE_VALIDITY_MS
    }
    
    fun hasCacheForUser(userId: String): Boolean {
        return cachedUserId == userId 
            && cachedUser != null 
            && isCacheValid()
    }
}

