# Socially - Modern Android Social Media App

A feature-rich social media application built with modern Android development practices, MVVM architecture, and Firebase backend.

---

## Table of Contents

1. [Features Overview](#features-overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Feature Implementation Details](#feature-implementation-details)
   - [Splash Screen](#1-splash-screen)
   - [Authentication](#2-authentication-loginregister)
   - [Home Feed](#3-home-feed)
   - [Create Post](#4-create-post)
   - [Profile Management](#5-profile-management)
   - [Search & Follow](#6-search--follow)
   - [Stories](#7-stories)
   - [Post Details & Comments](#8-post-details--comments)
   - [Notifications](#9-notifications)
5. [Data Layer](#data-layer)
6. [Why These Technologies?](#why-these-technologies)
7. [Setup & Installation](#setup--installation)

---

## Features Overview

Socially is a full-featured social media platform with:

- **User Authentication**: Email/Password and Google Sign-In
- **Posts**: Multi-image/video posts with captions, hashtags, and mentions
- **Stories**: 24-hour ephemeral content with viewer tracking
- **Social Interactions**: Like, comment, follow/unfollow
- **Profile Management**: Editable profile with avatar cropping
- **Search**: Real-time user search with debouncing
- **Notifications**: Real-time activity notifications
- **Media Upload**: Large file support (up to 100MB) with progress tracking
- **Offline-First**: Room database for session persistence
- **Caching**: In-memory caching for optimal performance

---

## Architecture

### MVVM (Model-View-ViewModel) Pattern

```
┌─────────────────────────────────────────────────────────────┐
│                       Presentation Layer                     │
│  ┌──────────────┐        ┌──────────────┐                   │
│  │  Fragment/   │◄──────►│  ViewModel   │                   │
│  │  Activity    │        │  (StateFlow) │                   │
│  └──────────────┘        └──────────────┘                   │
└─────────────────────────────┬───────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                        Domain Layer                          │
│  ┌──────────────┐        ┌──────────────┐                   │
│  │  Repository  │◄──────►│   Models     │                   │
│  │  Interface   │        │   (Domain)   │                   │
│  └──────────────┘        └──────────────┘                   │
└─────────────────────────────┬───────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                         Data Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  Repository  │  │ Room Database│  │   Firebase   │      │
│  │  Impl        │  │   + Cache    │  │  (Firestore) │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### Project Structure

```
app/src/main/java/com/apollo/socially/
│
├── data/                          # Data Layer
│   ├── cache/                     # In-memory caching
│   │   ├── ProfileCache.kt        # User profile caching
│   │   └── OtherProfileCache.kt   # Other users' profile caching
│   ├── local/
│   │   └── database/              # Room Database
│   │       ├── AppDatabase.kt     # Database singleton
│   │       ├── UserSessionDao.kt  # DAO for session queries
│   │       └── UserSessionEntity.kt # Entity for user sessions
│   ├── remote/
│   │   └── cloudinary/            # Cloudinary CDN integration
│   │       ├── CloudinaryUploader.kt # Upload implementation
│   │       └── CloudinaryConfig.kt   # API configuration
│   ├── repository/                # Repository implementations
│   │   ├── AuthRepositoryImpl.kt
│   │   ├── SessionRepositoryImpl.kt
│   │   ├── PostRepository.kt
│   │   ├── UserRepository.kt
│   │   ├── SearchRepository.kt
│   │   ├── FollowRepository.kt
│   │   ├── StoryRepository.kt
│   │   └── NotificationRepository.kt
│   └── upload/                    # Background upload services
│       ├── PostUploadService.kt   # Foreground service for posts
│       └── StoryUploadService.kt  # Foreground service for stories
│
├── domain/                        # Domain Layer
│   ├── model/                     # Business models
│   │   ├── User.kt
│   │   ├── Post.kt
│   │   ├── Story.kt
│   │   ├── Comment.kt
│   │   └── Notification.kt
│   └── repository/                # Repository interfaces
│       ├── IAuthRepository.kt
│       └── ISessionRepository.kt
│
├── ui/                            # Presentation Layer (Feature-based)
│   ├── splash/                    # Splash screen
│   ├── auth/                      # Authentication
│   │   ├── login/
│   │   └── register/
│   ├── home/                      # Home feed
│   ├── create/                    # Post creation
│   ├── profile/                   # User profile
│   ├── search/                    # User search
│   ├── post/                      # Post details
│   ├── story/                     # Stories viewer
│   ├── messages/                  # Messages (placeholder)
│   └── favorites/                 # Favorites (placeholder)
│
└── utils/                         # Utilities
    ├── MediaLoader.kt             # Load media from device
    ├── MediaPermissionHelper.kt   # Runtime permissions
    ├── CloudinaryUtils.kt         # CDN helpers
    └── HashMentionTextWatcher.kt  # Hashtag/mention styling
```

---

## Technology Stack

### Core Technologies

- **Language**: Kotlin
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 36
- **Build System**: Gradle (Kotlin DSL)

### Libraries & Frameworks

| Category | Library | Version | Purpose |
|----------|---------|---------|---------|
| **Architecture** | Lifecycle ViewModel KTX | 2.6.1 | ViewModel with StateFlow |
| **Database** | Room | 2.6.1 | Local persistence |
| **Backend** | Firebase Auth | 22.1.1 | Authentication |
| **Backend** | Firebase Firestore | 24.8.1 | NoSQL cloud database |
| **Concurrency** | Kotlin Coroutines | 1.6.4 | Asynchronous programming |
| **Navigation** | Navigation Component | 2.7.5 | Fragment navigation |
| **Image Loading** | Glide | 4.16.0 | Image caching & loading |
| **Image Cropping** | uCrop | 2.2.8 | Image cropping UI |
| **Video Player** | Media3 ExoPlayer | 1.3.1 | Video playback |
| **UI Components** | Material Design | - | Modern UI components |
| **UI Components** | ViewPager2 | 1.0.0 | Swipeable image galleries |
| **UI Components** | Lottie | 6.1.0 | Animations |
| **Circular Image** | CircleImageView | 3.1.0 | Profile avatars |
| **Sign-In** | Google Sign-In | 20.7.0 | OAuth authentication |

---

## Feature Implementation Details

### 1. Splash Screen

**Location**: `ui/splash/`

**Purpose**: 
- App initialization
- Session validation
- Navigation routing (login vs main app)

**Implementation**:

```kotlin
// SplashViewModel.kt
class SplashViewModel(
    private val sessionRepository: ISessionRepository
) : ViewModel() {
    
    private val _navigationState = MutableStateFlow<SplashNavigationState>(Loading)
    val navigationState: StateFlow<SplashNavigationState> = _navigationState.asStateFlow()
    
    init {
        checkLoginStatus()
    }
    
    private fun checkLoginStatus() {
        viewModelScope.launch {
            delay(3000) // Show splash for 3 seconds
            
            // Check Room database for saved session
            val isLoggedIn = sessionRepository.isUserLoggedIn()
            
            _navigationState.value = if (isLoggedIn) {
                NavigateToMain
            } else {
                NavigateToLogin
            }
        }
    }
}
```

**Key Features**:
- Uses Room database to check session persistence
- StateFlow for reactive UI updates
- Sealed classes for type-safe navigation states

---

### 2. Authentication (Login/Register)

**Location**: `ui/auth/login/`, `ui/auth/register/`

**Features**:
- Email/Password authentication
- Google Sign-In
- Username or email login
- Input validation
- Session persistence to Room database

**Implementation**:

#### Login Flow

```kotlin
// LoginViewModel.kt
fun login(emailOrUsername: String, password: String) {
    viewModelScope.launch {
        _loginState.value = LoginState.Loading
        try {
            val result = if (isEmail(emailOrUsername)) {
                repository.signInWithEmail(emailOrUsername, password)
            } else {
                repository.signInWithUsername(emailOrUsername, password)
            }
            
            val uid = result.user?.uid ?: throw Exception("User ID not found")
            val user = repository.getUserFromFirestore(uid)
            
            _loginState.value = LoginState.Success(user)
        } catch (e: Exception) {
            _loginState.value = LoginState.Error(e.message ?: "Login failed")
        }
    }
}
```

#### Session Persistence

After successful login, the user session is saved to Room database:

```kotlin
// SessionRepositoryImpl.kt
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
```

**Why This Approach?**:
- Room provides offline-first authentication
- User stays logged in even without internet
- Fast app startup (no network call needed on splash)

---

### 3. Home Feed

**Location**: `ui/home/`

**Features**:
- Paginated post feed
- Pull-to-refresh
- Infinite scroll (load more)
- Like posts
- View comments
- Unread notification indicator
- Story viewer integration

**Implementation**:

```kotlin
// HomeViewModel.kt
sealed class UiState {
    object Loading : UiState()
    data class Success(
        val posts: List<PostModel>,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false
    ) : UiState()
    data class Error(val message: String) : UiState()
}

private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState
```

**Pagination**:
- Uses Firestore cursor-based pagination
- Loads 8 posts per page
- Tracks last document for "load more"

**StateFlow Updates**:
```kotlin
fun loadMore() {
    val currentState = _uiState.value
    if (currentState is UiState.Success && currentState.isLoadingMore) return
    if (currentState is UiState.Success && !currentState.hasMore) return
    
    _uiState.value = currentState.copy(isLoadingMore = true)
    
    viewModelScope.launch {
        // Fetch next page...
    }
}
```

---

### 4. Create Post

**Location**: `ui/create/`

**Flow**: Picker → Caption → Background Upload

#### Step 1: Media Picker (`CreatePostPickerFragment.kt`)

**Features**:
- Load images and videos from device
- Camera integration
- Single or multi-select mode
- Grid layout with thumbnails
- Runtime permission handling

**Implementation**:

```kotlin
// MediaLoader.kt - Loads device media
suspend fun loadAll(context: Context): List<MediaItem> = withContext(Dispatchers.IO) {
    val items = mutableListOf<MediaItem>()
    
    // Load images
    val imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    context.contentResolver.query(imageUri, projection, null, null, sortOrder)?.use { cursor ->
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val uri = ContentUris.withAppendedId(imageUri, id)
            items.add(MediaItem(id, uri, MediaType.IMAGE))
        }
    }
    
    // Load videos
    // ... similar logic for videos
    
    items.sortedByDescending { it.id } // Sort by date
}
```

#### Step 2: Caption Input (`CreatePostCaptionFragment.kt`)

**Features**:
- Multi-image preview carousel
- Caption input with character count (2200 max)
- Hashtag and mention styling
- File size validation

#### Step 3: Background Upload (`PostUploadService.kt`)

**Why Foreground Service?**
- Uploads continue even if user navigates away
- Android requires foreground service for long-running tasks
- Shows persistent notification with progress

**Implementation**:

```kotlin
class PostUploadService : Service() {
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("Preparing upload…", 0))
        
        val uris = intent?.getParcelableArrayListExtra<Uri>(EXTRA_URIS)
        val types = intent.getStringArrayListExtra(EXTRA_TYPES)
        val caption = intent.getStringExtra(EXTRA_CAPTION) ?: ""
        
        scope.launch {
            runUpload(uris, types, caption)
            stopSelf()
        }
        
        return START_NOT_STICKY
    }
    
    private suspend fun runUpload(uris: List<Uri>, types: List<String>, caption: String) {
        val uploadedUrls = mutableListOf<String>()
        
        uris.forEachIndexed { index, uri ->
            val result = CloudinaryUploader.uploadImage(this, uri, folder) { progress ->
                updateNotification("Uploading ${index + 1}/${uris.size} · $progress%", progress)
            }
            
            when (result) {
                is UploadResult.Success -> uploadedUrls.add(result.url)
                is UploadResult.Error -> {
                    // Handle error
                    return
                }
            }
        }
        
        // Save to Firestore
        val post = hashMapOf(
            "userId" to uid,
            "mediaUrls" to uploadedUrls,
            "caption" to caption,
            "createdAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("posts").add(post).await()
    }
}
```

**Upload Progress Tracking**:
- Uses `UploadStateHolder` (StateFlow) to track upload state
- UI can observe state even after navigating away
- Supports retry functionality

---

### 5. Profile Management

**Location**: `ui/profile/`

**Features**:
- View own profile
- View other users' profiles
- Edit profile (display name, username, bio, avatar)
- Profile picture cropping (circular)
- Post grid view
- Follow/unfollow
- Followers/following lists
- Settings

#### Profile Caching

**Why Caching?**
- Reduces Firestore reads (saves costs)
- Instant profile loads
- Better user experience

**Implementation**:

```kotlin
// ProfileCache.kt
object ProfileCache {
    private var cachedUser: User? = null
    private var cachedPosts: MutableList<Post> = mutableListOf()
    private var lastCacheTime: Long = 0
    
    private const val CACHE_VALIDITY_MS = 10 * 60 * 1000L // 10 minutes
    
    fun getUserCache(userId: String): User? {
        if (!isCacheValid()) return null
        return cachedUser
    }
    
    fun cacheUserData(userId: String, user: User, posts: List<Post>) {
        cachedUser = user
        cachedPosts = posts.toMutableList()
        lastCacheTime = System.currentTimeMillis()
    }
    
    private fun isCacheValid(): Boolean {
        return (System.currentTimeMillis() - lastCacheTime) < CACHE_VALIDITY_MS
    }
}
```

**Cache Strategy**:
1. Check cache first
2. If cache valid, show cached data immediately
3. Fetch fresh data in background
4. Update cache if data changed
5. Invalidate cache on user actions (edit profile, new post, etc.)

#### Image Cropping

**Library**: uCrop (Yalantis)

**Why uCrop?**
- Material Design UI
- Circular crop support
- Gesture controls (pinch to zoom)
- Compression settings
- Works with Uri (no file copy needed)

**Implementation**:

```kotlin
// EditProfileFragment.kt
private fun launchCropper(sourceUri: Uri) {
    val destUri = Uri.fromFile(File(cacheDir, "avatar_${System.currentTimeMillis()}.jpg"))
    
    val options = UCrop.Options().apply {
        setCircleDimmedLayer(true)  // Circular preview
        setShowCropGrid(false)
        setCompressionQuality(90)
    }
    
    val intent = UCrop.of(sourceUri, destUri)
        .withAspectRatio(1f, 1f)    // Square crop
        .withMaxResultSize(800, 800)
        .withOptions(options)
        .getIntent(requireContext())
    
    cropLauncher.launch(intent)
}
```

---

### 6. Search & Follow

**Location**: `ui/search/`

**Features**:
- Real-time user search
- Search debouncing (800ms)
- Paginated results
- Follow/unfollow from search
- Username and display name matching

**Search Debouncing**:

**Why Debouncing?**
- Reduces Firestore queries (saves costs)
- Better performance (waits for user to stop typing)
- Prevents UI jank from rapid updates

**Implementation**:

```kotlin
// SearchViewModel.kt
private var searchJob: Job? = null

fun search(query: String) {
    searchJob?.cancel() // Cancel previous search
    
    if (query.trim().isEmpty()) {
        _uiState.value = UiState.Idle
        return
    }
    
    searchJob = viewModelScope.launch {
        _uiState.value = UiState.Searching
        delay(800) // Wait 800ms after user stops typing
        
        performSearch(query)
    }
}
```

**Follow System**:
- Uses `FollowEventBus` (SharedFlow) to broadcast follow events
- Other screens observe events and update their UI
- Updates follower/following counts across app

```kotlin
// FollowEventBus.kt
object FollowEventBus {
    private val _events = MutableSharedFlow<FollowEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<FollowEvent> = _events.asSharedFlow()
    
    suspend fun emit(event: FollowEvent) = _events.emit(event)
}

// ProfileViewModel observes events
viewModelScope.launch {
    FollowEventBus.events.collect { event ->
        // Update UI when someone is followed/unfollowed
    }
}
```

---

### 7. Stories

**Location**: `ui/story/`

**Features**:
- Create 24-hour stories
- View stories with auto-advance
- Story viewer tracking
- Delete own stories
- Background upload

**Story Viewer**:
- ViewPager2 for horizontal user switching
- Auto-advance timer (5 seconds per story)
- Progress bars for multiple stories
- Pause on touch hold

**Implementation**:

```kotlin
// StoryViewModel.kt
fun loadForUser(userId: String) {
    viewModelScope.launch {
        storyRepository.getActiveStoriesWithUsers()
            .onSuccess { groups ->
                val startIndex = groups.indexOfFirst { it.user.uid == userId }
                _uiState.value = UiState.Ready(groups, startIndex)
            }
    }
}

fun markStoryViewed(storyId: String) {
    viewModelScope.launch {
        storyRepository.markViewed(storyId)
    }
}
```

---

### 8. Post Details & Comments

**Location**: `ui/post/`

**Features**:
- Full post view
- Like with debounce
- Paginated comments
- Add comments
- Reply notifications

**Like Debouncing**:

**Why Debounce Likes?**
- Prevents spam clicking
- Reduces Firestore writes
- Better server performance

**Implementation**:

```kotlin
// PostDetailViewModel.kt
private var likeDebounceJob: Job? = null
private var isLikeInProgress = false

fun toggleLike() {
    if (isLikeInProgress) return
    
    // Optimistic update
    val wasLiked = _isLiked.value
    _isLiked.value = !wasLiked
    _likeCount.value += if (!wasLiked) 1 else -1
    
    likeDebounceJob?.cancel()
    likeDebounceJob = viewModelScope.launch {
        isLikeInProgress = true
        delay(300) // Wait 300ms
        
        postRepository.toggleLike(postId, wasLiked)
            .onFailure {
                // Revert on failure
                _isLiked.value = wasLiked
                _likeCount.value += if (wasLiked) 1 else -1
            }
        
        isLikeInProgress = false
    }
}
```

---

### 9. Notifications

**Location**: `ui/home/NotificationsFragment.kt`

**Types**:
- Follow notifications
- Like notifications
- Comment notifications
- Reply notifications

**Features**:
- Real-time updates
- Mark as read
- Navigate to relevant content
- Unread indicator

---

## Data Layer

### 📊 Storage Architecture Overview

Your app uses **TWO separate storage systems** for different purposes:

```
┌──────────────────────────────────────────────────────────────┐
│  PERSISTENT STORAGE (Survives app restart)                   │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ Room Database - SQLite                                 │  │
│  │ • user_session table (ONLY)                            │  │
│  │ • Purpose: Keep user logged in                         │  │
│  │ • Data: uid, email, username, profileImageUrl          │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  IN-MEMORY CACHE (Lost when app closes)                      │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ ProfileCache.kt - Kotlin Object                        │  │
│  │ • Purpose: Cache profiles to avoid Firestore reads    │  │
│  │ • Validity: 10 minutes                                 │  │
│  │ • Data: User profiles + their posts                    │  │
│  ├────────────────────────────────────────────────────────┤  │
│  │ OtherProfileCache.kt - Kotlin Object                   │  │
│  │ • Purpose: Cache other users' profiles                 │  │
│  │ • Validity: 10 minutes                                 │  │
│  │ • Data: User profile + posts + follow state            │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

---

### Room Database (Session Persistence ONLY)

**Purpose**: Store user login session to keep user logged in

**⚠️ Important**: Room DB is used **ONLY for sessions**, NOT for caching profiles or posts

**Why Room?**
- Type-safe SQL queries
- Compile-time verification
- Survives app restart
- Migration support

**What's Stored**:
- ✅ User session data (login state)
- ❌ NOT posts
- ❌ NOT profiles
- ❌ NOT comments
- ❌ NOT notifications

**Database Structure**:

```kotlin
@Database(
    entities = [UserSessionEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userSessionDao(): UserSessionDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "socially_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
```

**Entity**:

```kotlin
@Entity(tableName = "user_session")
data class UserSessionEntity(
    @PrimaryKey(autoGenerate = false)
    val uid: String,
    val email: String,
    val displayName: String,
    val username: String,
    val bio: String = "",
    val profileImageUrl: String? = null,
    val isLoggedIn: Boolean = true,
    val lastLoginTimestamp: Long = System.currentTimeMillis()
)
```

**DAO**:

```kotlin
@Dao
interface UserSessionDao {
    @Query("SELECT * FROM user_session WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getLoggedInUser(): UserSessionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSession(session: UserSessionEntity)
    
    @Query("DELETE FROM user_session")
    suspend fun clearAllSessions()
    
    @Query("SELECT EXISTS(SELECT 1 FROM user_session WHERE isLoggedIn = 1 LIMIT 1)")
    suspend fun isUserLoggedIn(): Boolean
}
```

---

### In-Memory Cache (Profiles & Posts - NOT Room DB!)

**Purpose**: Temporarily cache user profiles and posts to avoid repeated Firestore reads

**⚠️ Critical**: Caching uses **plain Kotlin objects**, NOT Room Database

**Why NOT Room DB for Caching?**
- ❌ Don't need persistence (cache expires in 10 minutes anyway)
- ❌ Room DB adds complexity (DAO, entities, migrations)
- ❌ Profile data changes frequently (better to re-fetch than sync)
- ✅ In-memory is FASTER (no disk I/O)
- ✅ Simpler code (just Kotlin variables)
- ✅ Auto-cleaned when app closes

**Implementation**:

```kotlin
// ProfileCache.kt - Just a Kotlin singleton object
object ProfileCache {
    // ⭐ Simple in-memory variables (NOT database!)
    private var cachedUser: User? = null
    private var cachedPosts: MutableList<Post> = mutableListOf()
    private var lastCacheTime: Long = 0
    private var cachedUserId: String? = null
    
    private const val CACHE_VALIDITY_MS = 10 * 60 * 1000L // 10 minutes
    
    fun getUserCache(userId: String): User? {
        if (cachedUserId != userId) return null
        if (!isCacheValid()) return null
        return cachedUser  // ⭐ Returns in-memory object
    }
    
    fun cacheUserData(userId: String, user: User, posts: List<Post>) {
        cachedUserId = userId
        cachedUser = user  // ⭐ Stores in RAM, not disk
        cachedPosts = posts.toMutableList()
        lastCacheTime = System.currentTimeMillis()
    }
    
    private fun isCacheValid(): Boolean {
        return (System.currentTimeMillis() - lastCacheTime) < CACHE_VALIDITY_MS
    }
}
```

**What Gets Cached**:
- ✅ User profiles (display name, username, bio, avatar)
- ✅ User posts (for profile grid view)
- ✅ Follow state (in OtherProfileCache)
- ⏰ Valid for 10 minutes only
- 🗑️ Lost when app closes

---

### 📊 Room DB vs In-Memory Cache Comparison

| Aspect | Room Database | In-Memory Cache |
|--------|---------------|-----------------|
| **Purpose** | Session persistence | Profile/post caching |
| **Storage** | SQLite (disk) | RAM (variables) |
| **Technology** | Room + SQLite | Plain Kotlin objects |
| **Survives restart?** | ✅ Yes | ❌ No |
| **Speed** | Fast (disk I/O) | Faster (RAM) |
| **Data stored** | User session only | Profiles + posts |
| **Validity** | Permanent | 10 minutes |
| **Size limit** | Large (disk space) | Small (RAM) |
| **Implementation** | Entities, DAOs, Database | `var` and `mutableListOf()` |
| **Used for** | Keep user logged in | Avoid Firestore reads |
| **Code complexity** | High (Room annotations) | Low (simple objects) |

---

### 🔄 Data Flow Example

```kotlin
┌─────────────────────────────────────────────────────────────┐
│ USER LOGS IN                                                 │
└──────────────────────┬──────────────────────────────────────┘
                       ▼
        LoginViewModel.login()
                       ▼
        SessionRepository.saveUserSession()
                       ▼
        ⭐ SAVES TO ROOM DB (SQLite on disk)
                       ▼
        user_session table: { uid, email, username, ... }

┌─────────────────────────────────────────────────────────────┐
│ APP RESTARTS (User opens app again)                         │
└──────────────────────┬──────────────────────────────────────┘
                       ▼
        SplashViewModel.checkLoginStatus()
                       ▼
        SessionRepository.isUserLoggedIn()
                       ▼
        ⭐ READS FROM ROOM DB
                       ▼
        Found session! → Navigate to MainContainerActivity ✅

┌─────────────────────────────────────────────────────────────┐
│ USER VIEWS PROFILE                                           │
└──────────────────────┬──────────────────────────────────────┘
                       ▼
        ProfileViewModel.loadUser()
                       ▼
        Check ProfileCache.hasCacheForUser(userId)
                       ▼
        ⭐ IN-MEMORY CHECK (just a Kotlin variable)
                       ▼
        ┌─────────────┴─────────────┐
        │                           │
   Cache Hit?                  Cache Miss?
        │                           │
        ▼                           ▼
   Return cached data        Fetch from Firestore
   (instant!)                       │
        │                           ▼
        │                    Store in ProfileCache
        │                    (in-memory variable)
        │                           │
        └─────────────┬─────────────┘
                      ▼
              Show profile to user

┌─────────────────────────────────────────────────────────────┐
│ APP CLOSES                                                   │
└──────────────────────┬──────────────────────────────────────┘
                       ▼
        Room DB persists ✅
        (session still saved in SQLite)
                       ▼
        ProfileCache lost ❌
        (all variables cleared from RAM)
                       ▼
        Next time: Re-fetch profiles from Firestore
```

---

### 💡 Why This Architecture Makes Sense

**Session Data** (Room DB):
```
✅ Must survive app restart
✅ Rarely changes (only on login/logout)
✅ Small data (one user record)
✅ Critical (user must stay logged in)
→ Perfect for SQLite persistence
```

**Profile/Post Data** (In-Memory Cache):
```
✅ Changes frequently (new posts, edits, follows)
✅ Large data (many users, many posts)
✅ Can re-fetch quickly (Firestore is fast)
✅ Temporary caching is enough
✅ 10-minute cache reduces Firestore costs
→ Perfect for in-memory caching
```

---

### Firebase Firestore

**Collections**:

- `users`: User profiles
- `posts`: User posts with media
- `stories`: 24-hour stories
- `comments`: Post comments
- `notifications`: User notifications
- `followers`: Follow relationships

**Security Rules**: Defined in `firestore.rules`

### Cloudinary CDN

**Purpose**: Media storage and optimization

**Features**:
- Image/video upload
- Automatic optimization
- Responsive delivery
- Transformation API

**Upload Implementation**:

```kotlin
// CloudinaryUploader.kt
suspend fun uploadImage(
    context: Context,
    uri: Uri,
    folder: String,
    onProgress: ((Int) -> Unit)?
): UploadResult = withContext(Dispatchers.IO) {
    // Check file size
    val fileSize = getFileSize(context, uri)
    if (fileSize > MAX_FILE_SIZE) {
        return@withContext UploadResult.Error(Exception("File too large"))
    }
    
    // Generate signature for secure upload
    val signature = generateSignature(params)
    
    // Stream upload with progress tracking
    uploadStreaming(context, uri, uploadUrl, resourceType, folder, fileSize, onProgress)
}
```

**Why Cloudinary?**
- Handles large files (up to 100MB)
- Automatic format optimization (WebP, AVIF)
- Responsive images (multiple sizes)
- Video transcoding
- CDN delivery (fast worldwide)

---

## Why These Technologies?

### StateFlow vs LiveData

**We chose StateFlow over LiveData because:**

| Feature | StateFlow | LiveData |
|---------|-----------|----------|
| **Coroutine Support** | ✅ Native | ❌ Requires wrappers |
| **Lifecycle Awareness** | ❌ Manual | ✅ Automatic |
| **Initial Value** | ✅ Required | ❌ Nullable |
| **Thread Safety** | ✅ Thread-safe | ✅ Thread-safe |
| **Operators** | ✅ Rich Flow operators | ❌ Limited |
| **Multi-platform** | ✅ Kotlin Multiplatform | ❌ Android only |
| **Type Safety** | ✅ Strong typing | ⚠️ Nullable |

**Example**:

```kotlin
// StateFlow - Clean coroutine integration
private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

viewModelScope.launch {
    repository.getData()
        .onSuccess { data ->
            _uiState.value = UiState.Success(data)
        }
}

// Fragment observes
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.uiState.collect { state ->
        when (state) {
            is UiState.Loading -> showLoading()
            is UiState.Success -> showData(state.data)
        }
    }
}
```

**Lifecycle Safety**:
- We use `viewLifecycleOwner.lifecycleScope` to automatically cancel collection when view is destroyed
- This prevents memory leaks and crashes

### Sealed Classes for States

**Why Sealed Classes?**

```kotlin
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}
```

**Benefits**:
- ✅ Type-safe state management
- ✅ Exhaustive when expressions
- ✅ Compiler-enforced handling
- ✅ Clear state transitions
- ✅ No invalid states

### Repository Pattern

**Why Repository?**
- Abstracts data sources (Firestore, Room, Cache)
- Single source of truth
- Testable (can mock repositories)
- Separation of concerns

```kotlin
// Domain layer - Interface
interface IAuthRepository {
    suspend fun signInWithEmail(email: String, password: String): AuthResult
    suspend fun getUserFromFirestore(uid: String): User?
}

// Data layer - Implementation
class AuthRepositoryImpl : IAuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    override suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return auth.signInWithEmailAndPassword(email, password).await()
    }
}
```

### FragmentManager & Navigation Component

**Why Navigation Component?**
- Type-safe navigation with SafeArgs
- Deep linking support
- Backstack management
- Animation support
- Bottom navigation integration

**Example**:

```kotlin
// Navigate with arguments
findNavController().navigate(
    R.id.action_home_to_profile,
    bundleOf("userId" to userId)
)

// Navigate with NavOptions (clear backstack)
findNavController().navigate(
    R.id.navigation_home,
    null,
    NavOptions.Builder()
        .setPopUpTo(R.id.nav_graph_main, false)
        .build()
)
```

### Kotlin Coroutines

**Why Coroutines?**
- Lightweight threads
- Structured concurrency
- Built-in cancellation
- Sequential async code
- Exception handling

**Example**:

```kotlin
viewModelScope.launch {
    try {
        _uiState.value = UiState.Loading
        
        val user = userRepository.getUser(userId)
        val posts = postRepository.getUserPosts(userId)
        
        _uiState.value = UiState.Success(user, posts)
    } catch (e: Exception) {
        _uiState.value = UiState.Error(e.message ?: "Unknown error")
    }
}
```

**Auto-cancellation**: When ViewModel is cleared, all coroutines in `viewModelScope` are automatically cancelled.

### Glide for Image Loading

**Why Glide?**
- Memory efficient (bitmap pooling)
- Automatic caching (memory + disk)
- GIF support
- Transformation support
- Lifecycle-aware

**Example**:

```kotlin
Glide.with(fragment)
    .load(imageUrl)
    .placeholder(R.drawable.placeholder)
    .error(R.drawable.error)
    .centerCrop()
    .into(imageView)
```

---

## 🎯 When to Use UiState Pattern?

### What is UiState?

**UiState** is a sealed class that represents all possible states of your UI. It's a **single source of truth** for what the UI should display.

### Why Use UiState?

| Without UiState | With UiState |
|----------------|--------------|
| Multiple LiveData/StateFlow variables | Single StateFlow of UiState |
| `isLoading`, `error`, `data`, `hasMore` | One sealed class with all states |
| Complex null checks | Type-safe when expressions |
| Hard to track all states | Exhaustive state handling |
| Race conditions possible | Guaranteed single state |

---

### ✅ When to Use UiState

#### 1. **Screens with Multiple States**

**Example from HomeViewModel.kt:**

```kotlin
sealed class UiState {
    object Loading : UiState()
    data class Success(
        val posts: List<PostModel>,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false
    ) : UiState()
    data class Error(val message: String) : UiState()
}

private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState
```

**Why this is better:**

```kotlin
// ❌ WITHOUT UiState - Multiple variables, complex logic
private val _isLoading = MutableStateFlow(false)
private val _posts = MutableStateFlow<List<PostModel>>(emptyList())
private val _error = MutableStateFlow<String?>(null)
private val _hasMore = MutableStateFlow(false)
private val _isLoadingMore = MutableStateFlow(false)

// Fragment must check all variables
if (_isLoading.value) showLoading()
if (_error.value != null) showError(_error.value!!)
if (_posts.value.isNotEmpty()) showPosts(_posts.value)

// ✅ WITH UiState - Single source of truth
when (state) {
    is UiState.Loading -> showLoading()
    is UiState.Success -> {
        showPosts(state.posts)
        if (state.isLoadingMore) showLoadingMore()
    }
    is UiState.Error -> showError(state.message)
}
```

---

#### 2. **Async Operations with Loading States**

**Example from NotificationsViewModel.kt:**

```kotlin
sealed class UiState {
    object Loading : UiState()
    data class Success(
        val todayNotifications: List<Notification>,
        val yesterdayNotifications: List<Notification>,
        val last7DaysNotifications: List<Notification>
    ) : UiState()
    data class Error(val message: String) : UiState()
}

fun loadNotifications() {
    viewModelScope.launch {
        _uiState.value = UiState.Loading  // Show loading
        
        notificationRepository.getUserNotifications()
            .onSuccess { notifications ->
                // Group notifications
                val today = notifications.filter { /* ... */ }
                val yesterday = notifications.filter { /* ... */ }
                val last7 = notifications.filter { /* ... */ }
                
                _uiState.value = UiState.Success(today, yesterday, last7)
            }
            .onFailure { error ->
                _uiState.value = UiState.Error(error.message ?: "Failed")
            }
    }
}
```

**Benefits:**
- ✅ UI always knows current state
- ✅ Can't show loading AND error simultaneously
- ✅ Compiler ensures all states are handled
- ✅ Clear state transitions (Loading → Success/Error)

---

#### 3. **Forms with Validation**

**Example from LoginViewModel.kt:**

```kotlin
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}

private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

fun login(emailOrUsername: String, password: String) {
    // Validate
    if (!validateInput(emailOrUsername, password)) {
        return  // Validation sets Error state
    }

    viewModelScope.launch {
        _loginState.value = LoginState.Loading  // Show loading
        
        try {
            val result = repository.signInWithEmail(emailOrUsername, password)
            val user = repository.getUserFromFirestore(result.user?.uid!!)
            
            _loginState.value = LoginState.Success(user)  // Navigate
        } catch (e: Exception) {
            _loginState.value = LoginState.Error(e.message ?: "Login failed")
        }
    }
}
```

**Fragment observes:**

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.loginState.collect { state ->
        when (state) {
            is LoginState.Idle -> {
                // Show empty form
                hideLoading()
                hideError()
            }
            is LoginState.Loading -> {
                // Show loading spinner
                showLoading()
                disableButton()
            }
            is LoginState.Success -> {
                // Navigate to main screen
                navigateToMain(state.user)
            }
            is LoginState.Error -> {
                // Show error message
                hideLoading()
                showError(state.message)
                enableButton()
            }
        }
    }
}
```

---

#### 4. **Pagination States**

**Example from SearchViewModel.kt:**

```kotlin
sealed class UiState {
    object Idle : UiState()  // No search performed yet
    object Searching : UiState()  // Initial search
    data class Success(
        val users: List<UserWithFollowState>,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false
    ) : UiState()
    data class Error(val message: String) : UiState()
}
```

**Why multiple states?**
- **Idle**: Show empty state or placeholder
- **Searching**: Show loading for initial search
- **Success with isLoadingMore**: Show results + loading at bottom
- **Success without hasMore**: Hide "Load More" button
- **Error**: Show error message

---

### ❌ When NOT to Use UiState

#### 1. **Simple Boolean Flags**

```kotlin
// ✅ GOOD - Just a simple flag
private val _isFollowing = MutableStateFlow(false)
val isFollowing: StateFlow<Boolean> = _isFollowing

// ❌ OVERKILL - Don't need UiState for this
sealed class FollowState {
    object Following : FollowState()
    object NotFollowing : FollowState()
}
```

#### 2. **Single Data Stream**

```kotlin
// ✅ GOOD - Just exposing data
private val _username = MutableStateFlow("")
val username: StateFlow<String> = _username

// ❌ OVERKILL
sealed class UsernameState {
    data class Loaded(val name: String) : UsernameState()
}
```

#### 3. **No Loading or Error States**

```kotlin
// ✅ GOOD - Direct data exposure, no async loading
val currentUser: StateFlow<User> = userRepository.currentUserFlow
```

---

## 🔄 StateFlow vs LiveData - Deep Dive

### Quick Comparison

| Feature | StateFlow | LiveData |
|---------|-----------|----------|
| **Coroutine Native** | ✅ Built for coroutines | ❌ Needs `liveData {}` builder |
| **Initial Value** | ✅ Required (non-nullable) | ❌ Nullable, no initial value |
| **Lifecycle Aware** | ❌ Manual setup | ✅ Automatic |
| **Cold vs Hot** | 🔥 Hot (always active) | ❄️ Cold (only when observed) |
| **Thread Safety** | ✅ Thread-safe | ✅ Thread-safe |
| **Operators** | ✅ Rich Flow API | ❌ Limited transformations |
| **Multiplatform** | ✅ Kotlin Multiplatform | ❌ Android only |
| **Backpressure** | ✅ Built-in | ❌ No support |
| **Testing** | ✅ Easy to test | ⚠️ Requires test extensions |

---

### Why We Chose StateFlow

#### 1. **Coroutine Integration**

**StateFlow - Natural coroutine syntax:**

```kotlin
// ViewModel
class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun loadFeed() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            val posts = postRepository.getPosts()  // suspend function
            
            _uiState.value = UiState.Success(posts)
        }
    }
}

// Fragment - Clean collection
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.uiState.collect { state ->
        // Handle state
    }
}
```

**LiveData - Requires wrapper:**

```kotlin
// ViewModel
class HomeViewModel : ViewModel() {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState
    
    fun loadFeed() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            val posts = postRepository.getPosts()
            
            _uiState.postValue(UiState.Success(posts))  // Must use postValue from coroutine
        }
    }
}

// Fragment - Need to observe
viewModel.uiState.observe(viewLifecycleOwner) { state ->
    // Handle state
}
```

---

#### 2. **Guaranteed Initial Value**

**StateFlow:**

```kotlin
// ✅ Must provide initial value - UI always has a state
private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// Fragment can safely collect without null checks
viewModel.uiState.collect { state ->
    when (state) {  // No null check needed
        is UiState.Loading -> showLoading()
        is UiState.Success -> showData(state.data)
    }
}
```

**LiveData:**

```kotlin
// ❌ Initial value is null
private val _uiState = MutableLiveData<UiState>()
val uiState: LiveData<UiState> = _uiState

// Fragment must handle null
viewModel.uiState.observe(viewLifecycleOwner) { state ->
    if (state == null) return@observe  // Null check required
    
    when (state) {
        is UiState.Loading -> showLoading()
        is UiState.Success -> showData(state.data)
    }
}
```

---

#### 3. **Flow Operators**

**StateFlow - Rich transformation API:**

```kotlin
// Combine multiple StateFlows
val combinedState = combine(
    postsFlow,
    storiesFlow,
    notificationsFlow
) { posts, stories, notifications ->
    HomeState(posts, stories, notifications)
}

// Transform data
val usernames = usersFlow
    .map { users -> users.map { it.username } }
    .filter { names -> names.isNotEmpty() }
    .distinctUntilChanged()

// Debounce search
val searchResults = searchQueryFlow
    .debounce(800)  // Wait 800ms after user stops typing
    .filter { it.isNotBlank() }
    .flatMapLatest { query -> searchRepository.search(query) }
```

**LiveData - Limited transformations:**

```kotlin
// Basic transformations only
val usernames = users.map { userList ->
    userList.map { it.username }
}

// Combining is verbose
val combined = MediatorLiveData<HomeState>().apply {
    addSource(posts) { postsList ->
        value = HomeState(postsList, stories.value, notifications.value)
    }
    addSource(stories) { storiesList ->
        value = HomeState(posts.value, storiesList, notifications.value)
    }
    // ... more boilerplate
}
```

---

#### 4. **Lifecycle Management**

**StateFlow - Manual but clear:**

```kotlin
// Fragment
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    
    // ✅ Use viewLifecycleOwner.lifecycleScope
    // Automatically cancels when view is destroyed
    viewLifecycleOwner.lifecycleScope.launch {
        viewModel.uiState.collect { state ->
            updateUI(state)
        }
    }
}

// ✅ Can use repeatOnLifecycle for fine control
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        // Only collects when STARTED or RESUMED
        // Cancels when STOPPED
        viewModel.uiState.collect { state ->
            updateUI(state)
        }
    }
}
```

**LiveData - Automatic but less control:**

```kotlin
// Fragment
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    
    // ✅ Automatic lifecycle handling
    viewModel.uiState.observe(viewLifecycleOwner) { state ->
        updateUI(state)
    }
    
    // ❌ But less control over when to observe
    // Always observes when STARTED, can't change
}
```

---

#### 5. **Testing**

**StateFlow - Easy to test:**

```kotlin
@Test
fun `test login success`() = runTest {
    // Arrange
    val viewModel = LoginViewModel(fakeRepository)
    val states = mutableListOf<LoginState>()
    
    // Collect states
    val job = launch {
        viewModel.loginState.toList(states)
    }
    
    // Act
    viewModel.login("test@example.com", "password")
    
    // Assert
    assertEquals(LoginState.Idle, states[0])
    assertEquals(LoginState.Loading, states[1])
    assertTrue(states[2] is LoginState.Success)
    
    job.cancel()
}
```

**LiveData - Requires test utilities:**

```kotlin
@Test
fun `test login success`() {
    // Need InstantTaskExecutorRule
    val viewModel = LoginViewModel(fakeRepository)
    val observer = mock<Observer<LoginState>>()
    
    viewModel.loginState.observeForever(observer)
    
    viewModel.login("test@example.com", "password")
    
    verify(observer).onChanged(LoginState.Loading)
    verify(observer).onChanged(argThat { it is LoginState.Success })
}
```

---

### Real-World Example: Search Debouncing

**With StateFlow (Used in SearchViewModel.kt):**

```kotlin
class SearchViewModel : ViewModel() {
    
    private val searchQuery = MutableStateFlow("")
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        // ✅ Use Flow operators for debouncing
        viewModelScope.launch {
            searchQuery
                .debounce(800)  // Wait 800ms
                .filter { it.trim().isNotEmpty() }
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query)
                }
        }
    }
    
    fun search(query: String) {
        searchQuery.value = query  // Just update the flow
    }
    
    private suspend fun performSearch(query: String) {
        _uiState.value = UiState.Searching
        
        searchRepository.searchUsers(query)
            .onSuccess { users ->
                _uiState.value = UiState.Success(users)
            }
            .onFailure { error ->
                _uiState.value = UiState.Error(error.message ?: "Failed")
            }
    }
}
```

**With LiveData (More complex):**

```kotlin
class SearchViewModel : ViewModel() {
    
    private val searchQuery = MutableLiveData<String>()
    
    // Need to use switchMap + custom debouncing
    val searchResults = searchQuery.switchMap { query ->
        liveData {
            delay(800)  // Manual debounce
            emit(searchRepository.searchUsers(query))
        }
    }
    
    fun search(query: String) {
        searchQuery.value = query
    }
}
```

---

### Best Practices from Socially App

#### 1. **Private Mutable, Public Immutable**

```kotlin
// ✅ CORRECT - Encapsulation
class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // UI can only read, not modify
}

// ❌ WRONG - UI can modify state
class HomeViewModel : ViewModel() {
    val uiState = MutableStateFlow<UiState>(UiState.Loading)
    
    // Fragment could do: viewModel.uiState.value = UiState.Success(...)
}
```

#### 2. **Use viewLifecycleOwner.lifecycleScope in Fragments**

```kotlin
// ✅ CORRECT - Cancels when view destroyed
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewModel.uiState.collect { state ->
            updateUI(state)
        }
    }
}

// ❌ WRONG - Leaks memory, keeps collecting after view destroyed
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    lifecycleScope.launch {  // ❌ Uses fragment lifecycle, not view
        viewModel.uiState.collect { state ->
            updateUI(state)
        }
    }
}
```

#### 3. **Single StateFlow for Screen State**

```kotlin
// ✅ GOOD - Single source of truth
sealed class UiState {
    object Loading : UiState()
    data class Success(
        val posts: List<Post>,
        val hasMore: Boolean,
        val isRefreshing: Boolean
    ) : UiState()
    data class Error(val message: String) : UiState()
}

private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
val uiState: StateFlow<UiState> = _uiState

// ❌ BAD - Multiple StateFlows hard to coordinate
private val _posts = MutableStateFlow<List<Post>>(emptyList())
private val _isLoading = MutableStateFlow(false)
private val _error = MutableStateFlow<String?>(null)
private val _hasMore = MutableStateFlow(false)
```

#### 4. **Separate StateFlows for Independent Features**

```kotlin
// ✅ GOOD - Separate concerns
class HomeViewModel : ViewModel() {
    // Main UI state
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // Independent feature - notification badge
    private val _hasUnreadNotifications = MutableStateFlow(false)
    val hasUnreadNotifications: StateFlow<Boolean> = _hasUnreadNotifications
    
    // Independent feature - upload progress
    private val _uploadProgress = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadProgress: StateFlow<UploadState> = _uploadProgress
}
```

---

## 🎓 Summary

### Use UiState When:
- ✅ Screen has multiple states (Loading, Success, Error)
- ✅ Async operations with loading indicators
- ✅ Forms with validation
- ✅ Pagination with "load more" states
- ✅ Complex UI that depends on multiple conditions

### Don't Use UiState When:
- ❌ Simple boolean flags
- ❌ Single data streams without loading
- ❌ No error or loading states needed

### Use StateFlow Over LiveData Because:
- ✅ **Coroutine native** - No wrapper needed
- ✅ **Initial value required** - No null checks
- ✅ **Rich operators** - debounce, combine, filter, map
- ✅ **Better testing** - No special test rules
- ✅ **Multiplatform** - Works in KMM projects
- ✅ **Type safety** - Non-nullable by default

### Code Pattern Used Throughout Socially:

```kotlin
// ViewModel
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: Data) : UiState()
    data class Error(val message: String) : UiState()
}

class MyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val data = repository.getData()
                _uiState.value = UiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

// Fragment
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.uiState.collect { state ->
        when (state) {
            is UiState.Loading -> showLoading()
            is UiState.Success -> showData(state.data)
            is UiState.Error -> showError(state.message)
        }
    }
}
```

**This pattern is used in every screen of the Socially app for consistent, predictable state management!** 🎉

---

## Caching Strategy

### Two-Level Caching

1. **Memory Cache** (ProfileCache, OtherProfileCache)
   - In-memory objects
   - Fast access (no I/O)
   - 10-minute validity
   - Invalidated on user actions

2. **Room Database Cache**
   - Persistent storage
   - Survives app restart
   - Used for session data

### Cache Invalidation

**When to Invalidate?**
- User edits profile → invalidate profile cache
- User creates post → invalidate profile cache (post count changed)
- User follows/unfollows → invalidate both caches
- Cache expiry (10 minutes)

**Implementation**:

```kotlin
// After uploading post
ProfileCache.invalidateCacheForUser(userId)

// After editing profile
ProfileCache.updateUserInCache(userId, updatedUser)

// After follow/unfollow
OtherProfileCache.updateIsFollowing(userId, isFollowing)
```

---

## Setup & Installation

### Prerequisites

- Android Studio (latest version)
- JDK 11 or higher
- Android SDK 29+
- Firebase project

### Steps

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Socially
   ```

2. **Configure Firebase**
   - Create a Firebase project
   - Download `google-services.json`
   - Place in `app/` directory
   - Enable Firebase Authentication (Email/Password & Google)
   - Enable Firestore Database

3. **Configure Cloudinary**
   - Create a Cloudinary account
   - Update `CloudinaryConfig.kt` with your credentials:
     ```kotlin
     object CloudinaryConfig {
         const val CLOUD_NAME = "your_cloud_name"
         const val API_KEY = "your_api_key"
         const val API_SECRET = "your_api_secret"
     }
     ```

4. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```

5. **Grant Permissions**
   - Camera permission (for taking photos)
   - Storage permission (for accessing media)
   - Notification permission (Android 13+)

---

## Testing Checklist

See [TESTING_CHECKLIST.md](TESTING_CHECKLIST.md) for comprehensive testing guide.

---

## Documentation Files

- [ARCHITECTURE.md](ARCHITECTURE.md) - Detailed architecture documentation
- [LIKE_FEATURE_IMPLEMENTATION.md](LIKE_FEATURE_IMPLEMENTATION.md) - Like feature details
- [COMMENTS_NOTIFICATIONS_IMPLEMENTATION.md](COMMENTS_NOTIFICATIONS_IMPLEMENTATION.md) - Comments system
- [FOLLOW_FIXES_SUMMARY.md](FOLLOW_FIXES_SUMMARY.md) - Follow system implementation
- [SEARCH_FOLLOW_IMPLEMENTATION.md](SEARCH_FOLLOW_IMPLEMENTATION.md) - Search functionality
- [PROFILE_FEATURES_SUMMARY.md](PROFILE_FEATURES_SUMMARY.md) - Profile features
- [LARGE_FILE_UPLOAD_IMPLEMENTATION.md](LARGE_FILE_UPLOAD_IMPLEMENTATION.md) - Upload system
- [BUGFIXES_NOTIFICATIONS_LOGOUT.md](BUGFIXES_NOTIFICATIONS_LOGOUT.md) - Bug fixes log

---

## Key Learnings & Best Practices

### 1. Always Use StateFlow with Lifecycle-Aware Collection

```kotlin
// ✅ CORRECT - Lifecycle-aware
viewLifecycleOwner.lifecycleScope.launch {
    viewModel.uiState.collect { state ->
        // Handle state
    }
}

// ❌ WRONG - Leaks memory
lifecycleScope.launch {
    viewModel.uiState.collect { state ->
        // Continues collecting even when fragment is destroyed
    }
}
```

### 2. Use Sealed Classes for States

```kotlin
// ✅ Type-safe, exhaustive
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: List<Item>) : UiState()
    data class Error(val message: String) : UiState()
}

when (state) {
    is UiState.Loading -> showLoading()
    is UiState.Success -> showData(state.data)
    is UiState.Error -> showError(state.message)
    // Compiler enforces all cases
}
```

### 3. Debounce User Input

```kotlin
// Search, like, follow - all debounced to prevent spam
private var searchJob: Job? = null

fun search(query: String) {
    searchJob?.cancel()
    searchJob = viewModelScope.launch {
        delay(800)
        performSearch(query)
    }
}
```

### 4. Optimistic Updates with Rollback

```kotlin
// Update UI immediately, rollback on failure
fun toggleLike() {
    val wasLiked = _isLiked.value
    _isLiked.value = !wasLiked
    
    viewModelScope.launch {
        repository.toggleLike(postId)
            .onFailure {
                _isLiked.value = wasLiked // Rollback
            }
    }
}
```

### 5. Use Foreground Services for Long Operations

```kotlin
// Upload continues even if app is closed
class PostUploadService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, notification)
        // Long-running upload
        return START_NOT_STICKY
    }
}
```

### 6. Cache Aggressively, Invalidate Smartly

```kotlin
// Load from cache first, fetch in background
if (ProfileCache.hasCacheForUser(userId)) {
    val cachedData = ProfileCache.getUserCache(userId)
    _uiState.value = UiState.Success(cachedData)
    checkForUpdates(userId) // Background refresh
} else {
    fetchFreshData(userId)
}
```

---

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Follow MVVM architecture
2. Use StateFlow for reactive state
3. Write meaningful commit messages
4. Add comments for complex logic
5. Test thoroughly before submitting

---

## License

[Add your license here]

---

## Contact

[Add contact information here]

---

**Built with ❤️ using Kotlin and Modern Android Development practices**

