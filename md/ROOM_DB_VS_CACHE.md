# Room DB vs In-Memory Cache - Quick Reference

## ❓ Your Question

**"Did my Room DB as my sessions only? Like caching is for the profile? Or is caching from the Room DB also?"**

## ✅ Answer

**YES!** Room DB is **ONLY** for sessions. Caching is **separate** (in-memory, NOT Room DB).

---

## 📊 Two Completely Different Systems

```
┌─────────────────────────────────────────────────────────────┐
│ 1. ROOM DATABASE (SQLite - Persistent Storage)              │
├─────────────────────────────────────────────────────────────┤
│ Purpose:  Keep user logged in                               │
│ Storage:  SQLite database on disk                           │
│ Files:    AppDatabase.kt, UserSessionDao.kt,                │
│           UserSessionEntity.kt                               │
│ Table:    user_session (ONLY one table)                     │
│ Data:     uid, email, username, profileImageUrl             │
│ Lifetime: Survives app restart ✅                            │
│ Usage:    Login/logout ONLY                                 │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 2. IN-MEMORY CACHE (Kotlin Objects - Temporary)             │
├─────────────────────────────────────────────────────────────┤
│ Purpose:  Avoid repeated Firestore reads                    │
│ Storage:  RAM (just Kotlin variables)                       │
│ Files:    ProfileCache.kt, OtherProfileCache.kt             │
│ Data:     User profiles, posts, follow state                │
│ Lifetime: Lost when app closes ❌                            │
│ Validity: 10 minutes                                         │
│ Usage:    Profile viewing, post lists                       │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔍 Code Comparison

### Room Database (Sessions)

```kotlin
// AppDatabase.kt
@Database(
    entities = [UserSessionEntity::class],  // ⭐ ONLY sessions
    version = 3
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userSessionDao(): UserSessionDao
}

// UserSessionEntity.kt
@Entity(tableName = "user_session")  // ⭐ SQLite table
data class UserSessionEntity(
    @PrimaryKey val uid: String,
    val email: String,
    val username: String,
    val profileImageUrl: String?,
    val isLoggedIn: Boolean = true
)

// UserSessionDao.kt
@Dao
interface UserSessionDao {
    @Query("SELECT * FROM user_session WHERE isLoggedIn = 1")
    suspend fun getLoggedInUser(): UserSessionEntity?  // ⭐ SQL query
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSession(session: UserSessionEntity)
}

// Usage: Save to Room DB
sessionRepository.saveUserSession(user)
  → userSessionDao.insertUserSession(session)
  → SQLite INSERT INTO user_session ...
```

---

### In-Memory Cache (Profiles)

```kotlin
// ProfileCache.kt
object ProfileCache {  // ⭐ Just a Kotlin object
    
    // ⭐ Simple in-memory variables (NOT database!)
    private var cachedUser: User? = null
    private var cachedPosts: MutableList<Post> = mutableListOf()
    private var lastCacheTime: Long = 0
    
    // No @Query, no SQL, no Room annotations!
    fun getUserCache(userId: String): User? {
        if (!isCacheValid()) return null
        return cachedUser  // ⭐ Just returns a variable
    }
    
    fun cacheUserData(userId: String, user: User, posts: List<Post>) {
        cachedUser = user  // ⭐ Just assigns to a variable
        cachedPosts = posts.toMutableList()
        lastCacheTime = System.currentTimeMillis()
    }
}

// Usage: Store in memory
ProfileCache.cacheUserData(userId, user, posts)
  → Just stores in Kotlin variables
  → No database involved
```

---

## 🎯 Key Differences

| Feature | Room Database | In-Memory Cache |
|---------|---------------|-----------------|
| **What is it?** | SQLite database | Kotlin object variables |
| **Files** | AppDatabase.kt, DAO, Entity | ProfileCache.kt |
| **Storage location** | Disk (/data/data/app/) | RAM (memory) |
| **Annotations** | @Database, @Entity, @Dao | None |
| **Queries** | SQL queries via @Query | Direct variable access |
| **Persists on restart?** | ✅ YES | ❌ NO |
| **Used for** | User sessions | Profiles & posts |
| **Data type** | UserSessionEntity | User, Post models |
| **Complexity** | High (Room setup) | Low (just variables) |

---

## 📝 Summary

### Room Database (Sessions Only)
```kotlin
✅ Purpose: Keep user logged in
✅ Tech: Room + SQLite
✅ Storage: Disk (persistent)
✅ Data: User session (uid, email, username)
✅ One table: user_session
✅ Survives restart: YES
✅ Used in: Login, splash screen
```

### In-Memory Cache (Profiles/Posts)
```kotlin
✅ Purpose: Avoid Firestore re-reads
✅ Tech: Plain Kotlin objects
✅ Storage: RAM (temporary)
✅ Data: Profiles, posts, follow state
✅ No database: Just variables
✅ Survives restart: NO (lost)
✅ Used in: Profile viewing, feed
✅ Validity: 10 minutes
```

---

## 🔄 Flow Example

### Login Flow (Room DB)
```
User logs in
    ↓
LoginViewModel.login()
    ↓
SessionRepositoryImpl.saveUserSession()
    ↓
userSessionDao.insertUserSession()
    ↓
⭐ ROOM DB: INSERT INTO user_session
    ↓
Session saved to SQLite ✅

App restarts
    ↓
SplashViewModel.checkLoginStatus()
    ↓
userSessionDao.isUserLoggedIn()
    ↓
⭐ ROOM DB: SELECT * FROM user_session
    ↓
User still logged in ✅
```

### Profile View Flow (In-Memory Cache)
```
User views profile
    ↓
ProfileViewModel.loadUser()
    ↓
Check ProfileCache.getUserCache()
    ↓
⭐ IN-MEMORY: Read cachedUser variable
    ↓
if (cached && valid) {
    Show cached data instantly ✅
} else {
    Fetch from Firestore
    Store in ProfileCache (in-memory)
}

App closes
    ↓
⭐ IN-MEMORY: All variables cleared
    ↓
Next time: Must re-fetch from Firestore
```

---

## 💡 Why Not Use Room DB for Caching?

**You could, but it's not optimal:**

### If You Used Room for Caching (Not Recommended)
```kotlin
// Would need:
@Entity(tableName = "cached_users")
data class CachedUserEntity(...)

@Entity(tableName = "cached_posts")
data class CachedPostEntity(...)

@Dao
interface CachedUserDao {
    @Query("SELECT * FROM cached_users WHERE userId = :id")
    suspend fun getUser(id: String): CachedUserEntity?
    
    @Query("DELETE FROM cached_users WHERE cacheTime < :expiry")
    suspend fun clearExpired(expiry: Long)
}

// Problems:
❌ Much more code (DAOs, entities, migrations)
❌ Slower (disk I/O vs RAM)
❌ Cache invalidation is complex
❌ Need to handle database migrations
❌ Profile data changes frequently
❌ Overkill for temporary caching
```

### Current In-Memory Approach (Optimal)
```kotlin
// Simple:
object ProfileCache {
    private var cachedUser: User? = null
    fun getUserCache() = cachedUser
}

// Advantages:
✅ 5 lines of code vs 50+
✅ Faster (RAM vs disk)
✅ Auto-cleared on app close
✅ No migrations needed
✅ Perfect for temporary caching
```

---

## 🎓 Final Answer

**Your understanding is correct!**

1. ✅ **Room DB** = Sessions ONLY
   - One table: `user_session`
   - Keeps user logged in
   - Persistent storage (SQLite)

2. ✅ **Caching** = In-memory ONLY (NOT Room DB)
   - Kotlin objects: `ProfileCache`, `OtherProfileCache`
   - Avoids Firestore re-reads
   - Temporary (10 minutes, lost on app close)

3. ✅ **They are completely separate systems**
   - Room DB does NOT cache profiles
   - Profile cache does NOT use Room DB

**Perfect architecture for your use case!** 🎉

