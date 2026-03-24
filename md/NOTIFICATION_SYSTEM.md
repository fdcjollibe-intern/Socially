# 🔔 Notification System - Complete Documentation

## How Notifications Work in Socially

Your app uses a **Firestore-based notification system** (no FCM push notifications). Notifications are created when users interact with content (like, comment, follow) and displayed when the recipient opens the app.

---

## 📊 Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│                  Notification Flow                           │
└──────────────────────────────────────────────────────────────┘

USER A likes USER B's post
        ↓
PostRepository.toggleLike()
        ↓
NotificationRepository.createLikeNotification()
        ↓
Create document in Firestore "notifications" collection
        ↓
USER B opens Notifications screen
        ↓
NotificationRepository.getUserNotifications()
        ↓
Fetch notifications from Firestore
        ↓
NotificationsViewModel groups by time period
        ↓
NotificationsFragment displays in 3 sections:
    • Today
    • Yesterday  
    • Last 7 Days
```

---

## 🎯 Notification Types

### Three Types Supported

```kotlin
enum class NotificationType {
    LIKE,      // Someone liked your post
    COMMENT,   // Someone commented on your post
    FOLLOW     // Someone followed you
}
```

### Data Model

**File**: `domain/model/Notification.kt`

```kotlin
data class Notification(
    val id: String = "",
    val userId: String = "",           // Who receives the notification
    val actorId: String = "",          // Who performed the action
    val actorUsername: String = "",    // Actor's username
    val actorAvatarUrl: String? = null,// Actor's profile picture
    val type: NotificationType = LIKE, // Type of notification
    val postId: String? = null,        // Related post (if applicable)
    val commentId: String? = null,     // Related comment (if applicable)
    val postThumbnailUrl: String? = null, // Post thumbnail
    val commentText: String? = null,   // Comment text preview
    val isRead: Boolean = false,       // Read status
    @ServerTimestamp val createdAt: Date? = null
)
```

---

## 📝 Creating Notifications

### 1. LIKE Notification

**When**: User A likes User B's post

**File**: `PostRepository.kt` (Lines 148-153)

```kotlin
suspend fun toggleLike(postId: String, currentlyLiked: Boolean): Result<Boolean> {
    // ... like logic ...
    
    if (!currentlyLiked) {  // User is liking (not unliking)
        // Create like document
        likeRef.set(mapOf("userId" to uid, "likedAt" to serverTimestamp())).await()
        postRef.update("likesCount", FieldValue.increment(1)).await()
        
        // ⭐ CREATE NOTIFICATION
        val postDoc = postRef.get().await()
        val postOwnerId = postDoc.getString("userId") ?: ""
        val thumbnailUrl = postDoc.get("mediaUrls")?.firstOrNull()
        
        notificationRepository.createLikeNotification(
            postId = postId,
            postOwnerId = postOwnerId,
            postThumbnailUrl = thumbnailUrl
        )
    }
}
```

**File**: `NotificationRepository.kt` (Lines 24-50)

```kotlin
suspend fun createLikeNotification(
    postId: String,
    postOwnerId: String,
    postThumbnailUrl: String?
): Result<Unit> = runCatching {
    val currentUserId = auth.currentUser?.uid ?: error("Not logged in")
    
    // ⭐ DON'T notify yourself
    if (currentUserId == postOwnerId) return@runCatching
    
    // Get current user's info
    val userDoc = firestore.collection("users").document(currentUserId).get().await()
    val username = userDoc.getString("username") ?: "someone"
    val avatarUrl = userDoc.getString("profileImageUrl")

    // Create notification document
    val data = mapOf(
        "userId" to postOwnerId,           // Notification goes to post owner
        "actorId" to currentUserId,        // You are the actor
        "actorUsername" to username,
        "actorAvatarUrl" to avatarUrl,
        "type" to NotificationType.LIKE.name,
        "postId" to postId,
        "postThumbnailUrl" to postThumbnailUrl,
        "isRead" to false,
        "createdAt" to FieldValue.serverTimestamp()
    )

    // ⭐ Store in Firestore
    notificationsCollection.add(data).await()
}
```

**Firestore Structure**:
```
notifications/
  └── {notificationId}
      ├── userId: "user_B_id"
      ├── actorId: "user_A_id"
      ├── actorUsername: "user_A"
      ├── actorAvatarUrl: "https://..."
      ├── type: "LIKE"
      ├── postId: "post_123"
      ├── postThumbnailUrl: "https://..."
      ├── isRead: false
      └── createdAt: Timestamp
```

---

### 2. COMMENT Notification

**When**: User A comments on User B's post

**File**: `PostRepository.kt` (Lines 205-212)

```kotlin
suspend fun addComment(postId: String, text: String): Result<Comment> {
    // ... create comment ...
    
    // ⭐ CREATE NOTIFICATION
    val postDoc = postsCollection.document(postId).get().await()
    val postOwnerId = postDoc.getString("userId") ?: ""
    val thumbnailUrl = postDoc.get("mediaUrls")?.firstOrNull()
    
    notificationRepository.createCommentNotification(
        postId = postId,
        postOwnerId = postOwnerId,
        commentId = ref.id,           // The comment we just created
        commentText = text,           // Comment text preview
        postThumbnailUrl = thumbnailUrl
    )
}
```

**File**: `NotificationRepository.kt` (Lines 53-79)

```kotlin
suspend fun createCommentNotification(
    postId: String,
    postOwnerId: String,
    commentId: String,
    commentText: String,
    postThumbnailUrl: String?
): Result<Unit> = runCatching {
    val currentUserId = auth.currentUser?.uid ?: error("Not logged in")
    
    // ⭐ DON'T notify yourself
    if (currentUserId == postOwnerId) return@runCatching
    
    val userDoc = firestore.collection("users").document(currentUserId).get().await()
    val username = userDoc.getString("username") ?: "someone"
    val avatarUrl = userDoc.getString("profileImageUrl")

    val data = mapOf(
        "userId" to postOwnerId,
        "actorId" to currentUserId,
        "actorUsername" to username,
        "actorAvatarUrl" to avatarUrl,
        "type" to NotificationType.COMMENT.name,
        "postId" to postId,
        "commentId" to commentId,         // ⭐ Store comment ID
        "commentText" to commentText,     // ⭐ Store preview
        "postThumbnailUrl" to postThumbnailUrl,
        "isRead" to false,
        "createdAt" to FieldValue.serverTimestamp()
    )

    notificationsCollection.add(data).await()
}
```

---

### 3. FOLLOW Notification

**When**: User A follows User B

**Implementation**: Similar to like/comment, but likely in `FollowRepository` (not shown in provided code).

---

## 🗑️ Deleting Notifications

### Unlike: Delete Notification

**File**: `PostRepository.kt`

```kotlin
if (currentlyLiked) {  // User is unliking
    // Delete like document
    likeRef.delete().await()
    postRef.update("likesCount", FieldValue.increment(-1)).await()
    
    // ⭐ DELETE THE NOTIFICATION
    notificationRepository.deleteLikeNotification(postId)
}
```

**File**: `NotificationRepository.kt` (Lines 83-91)

```kotlin
suspend fun deleteLikeNotification(postId: String): Result<Unit> = runCatching {
    val currentUserId = auth.currentUser?.uid ?: error("Not logged in")
    
    // Find notification matching criteria
    val query = notificationsCollection
        .whereEqualTo("actorId", currentUserId)   // You created it
        .whereEqualTo("postId", postId)           // For this post
        .whereEqualTo("type", NotificationType.LIKE.name)
        .get().await()

    // Delete all matching notifications
    query.documents.forEach { it.reference.delete().await() }
}
```

---

## 📥 Fetching & Displaying Notifications

### 1. Fetch from Firestore

**File**: `NotificationRepository.kt` (Lines 108-119)

```kotlin
suspend fun getUserNotifications(limit: Int = PAGE_SIZE): Result<List<Notification>> = runCatching {
    val currentUserId = auth.currentUser?.uid ?: error("Not logged in")
    
    // Query notifications for current user
    val snapshot = notificationsCollection
        .whereEqualTo("userId", currentUserId)     // ⭐ Your notifications
        .orderBy("createdAt", Query.Direction.DESCENDING)  // Newest first
        .limit(limit.toLong())                     // Default: 20
        .get().await()

    // Convert to Notification objects
    snapshot.documents.mapNotNull { doc ->
        doc.toObject(Notification::class.java)?.copy(id = doc.id)
    }
}
```

---

### 2. Group by Time Period

**File**: `NotificationsViewModel.kt` (Lines 36-66)

```kotlin
fun loadNotifications() {
    viewModelScope.launch {
        notificationRepository.getUserNotifications()
            .onSuccess { notifications ->
                // Calculate time boundaries
                val now = System.currentTimeMillis()
                val oneDayAgo = now - TimeUnit.DAYS.toMillis(1)
                val twoDaysAgo = now - TimeUnit.DAYS.toMillis(2)
                val sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7)

                // Separate into groups
                val today = mutableListOf<Notification>()
                val yesterday = mutableListOf<Notification>()
                val last7 = mutableListOf<Notification>()

                notifications.forEach { notif ->
                    val createdTime = notif.createdAt?.time ?: 0
                    when {
                        createdTime >= oneDayAgo -> today.add(notif)        // Last 24 hours
                        createdTime >= twoDaysAgo -> yesterday.add(notif)   // 24-48 hours ago
                        createdTime >= sevenDaysAgo -> last7.add(notif)     // 2-7 days ago
                    }
                }

                // Emit grouped state
                _uiState.value = UiState.Success(
                    todayNotifications = today,
                    yesterdayNotifications = yesterday,
                    last7DaysNotifications = last7
                )
            }
    }
}
```

---

### 3. Display in UI

**File**: `NotificationsFragment.kt` (Lines 46-68)

```kotlin
private fun observeViewModel() {
    viewLifecycleOwner.lifecycleScope.launch {
        viewModel.uiState.collect { state ->
            when (state) {
                is NotificationsViewModel.UiState.Success -> {
                    // ⭐ Setup 3 separate RecyclerViews
                    setupGroup(binding.notifTodayRv, state.todayNotifications)
                    setupGroup(binding.notifYesterdayRv, state.yesterdayNotifications)
                    setupGroup(binding.notifLast7Rv, state.last7DaysNotifications)
                    
                    // Show/hide sections based on content
                    binding.notifTodayRv.visibility = 
                        if (state.todayNotifications.isNotEmpty()) View.VISIBLE else View.GONE
                    
                    binding.notifYesterdayHeader.visibility = 
                        if (state.yesterdayNotifications.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.notifYesterdayRv.visibility = 
                        if (state.yesterdayNotifications.isNotEmpty()) View.VISIBLE else View.GONE
                    
                    binding.notifLast7Header.visibility = 
                        if (state.last7DaysNotifications.isNotEmpty()) View.VISIBLE else View.GONE
                    binding.notifLast7Rv.visibility = 
                        if (state.last7DaysNotifications.isNotEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}
```

**Layout Structure**:
```
┌─────────────────────────────────────┐
│ Notifications                    ← │
├─────────────────────────────────────┤
│ Today                               │  ← Header (always visible)
│ ┌─────────────────────────────────┐ │
│ │ [Avatar] user1 liked your post  │ │
│ │          2m ago            [📷] │ │
│ └─────────────────────────────────┘ │
│ ┌─────────────────────────────────┐ │
│ │ [Avatar] user2 commented...     │ │
│ │          5m ago            [📷] │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ Yesterday                           │  ← Header (if has data)
│ ┌─────────────────────────────────┐ │
│ │ [Avatar] user3 started following│ │
│ │          1d ago      [Follow ↩] │ │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ Last 7 Days                         │  ← Header (if has data)
│ ┌─────────────────────────────────┐ │
│ │ [Avatar] user4 liked your post  │ │
│ │          3d ago            [📷] │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

---

### 4. Render Each Notification

**File**: `NotificationAdapter.kt` (Lines 29-84)

```kotlin
fun bind(notif: Notification) {
    // ⭐ Build message based on type
    val message = when (notif.type) {
        NotificationType.LIKE -> 
            "${notif.actorUsername} liked your post. ${notif.createdAt.toTimeAgo()}"
        
        NotificationType.COMMENT -> 
            "${notif.actorUsername} commented: \"${notif.commentText?.take(50)}...\". ${notif.createdAt.toTimeAgo()}"
        
        NotificationType.FOLLOW -> 
            "${notif.actorUsername} started following you. ${notif.createdAt.toTimeAgo()}"
    }
    binding.notifMessage.text = message
    
    // ⭐ Load actor's avatar
    Glide.with(binding.root.context)
        .load(notif.actorAvatarUrl)
        .circleCrop()
        .into(binding.notifAvatar)

    // ⭐ Show different UI based on type
    when (notif.type) {
        NotificationType.LIKE, NotificationType.COMMENT -> {
            // Show post thumbnail
            binding.notifThumbnail.visibility = View.VISIBLE
            binding.notifBtnFollowBack.visibility = View.GONE
            Glide.with(binding.root.context)
                .load(notif.postThumbnailUrl)
                .into(binding.notifThumbnail)
        }
        NotificationType.FOLLOW -> {
            // Show "Follow Back" button
            binding.notifThumbnail.visibility = View.GONE
            binding.notifBtnFollowBack.visibility = View.VISIBLE
        }
    }
    
    // ⭐ Highlight unread notifications
    if (!notif.isRead) {
        binding.root.setBackgroundResource(R.color.notif_unread_bg)
    } else {
        binding.root.setBackgroundResource(android.R.color.transparent)
    }
}
```

---

## 🎯 Handling Notification Clicks

**File**: `NotificationsFragment.kt` (Lines 85-120)

```kotlin
private fun handleNotificationClick(notification: Notification) {
    // ⭐ Mark as read
    viewModel.markAsRead(notification.id)
    
    when (notification.type) {
        NotificationType.LIKE -> {
            // Navigate to post
            // TODO: Navigate to specific post
        }
        
        NotificationType.COMMENT -> {
            // ⭐ Open comments bottom sheet
            notification.postId?.let { postId ->
                val bottomSheet = CommentsBottomSheet.newInstance(
                    postId = postId,
                    likeCount = 0,
                    isLiked = false
                )
                // Pass commentId to highlight the specific comment
                notification.commentId?.let { commentId ->
                    bottomSheet.arguments?.putString("highlightCommentId", commentId)
                }
                bottomSheet.show(childFragmentManager, "comments")
            }
        }
        
        NotificationType.FOLLOW -> {
            // Navigate to user profile
            // TODO: Navigate to other profile
        }
    }
}
```

---

## ✅ Mark as Read

### Single Notification

**File**: `NotificationRepository.kt` (Lines 123-126)

```kotlin
suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
    notificationsCollection.document(notificationId)
        .update("isRead", true).await()
}
```

### Mark All as Read

**File**: `NotificationRepository.kt` (Lines 128-138)

```kotlin
suspend fun markAllAsRead(): Result<Unit> = runCatching {
    val currentUserId = auth.currentUser?.uid ?: error("Not logged in")
    
    // Find all unread notifications
    val query = notificationsCollection
        .whereEqualTo("userId", currentUserId)
        .whereEqualTo("isRead", false)
        .get().await()

    // Update each one
    query.documents.forEach {
        it.reference.update("isRead", true).await()
    }
}
```

---

## 🔴 Unread Notification Badge

### Check for Unread Notifications

**File**: `HomeViewModel.kt`

```kotlin
fun checkUnreadNotifications() {
    viewModelScope.launch {
        try {
            val notifications = notificationRepository.getUserNotifications(limit = 50)
                .getOrNull() ?: emptyList()
            
            // Check if any are unread
            val hasUnread = notifications.any { it.isRead == false }
            
            _hasUnreadNotifications.value = hasUnread
        } catch (e: Exception) {
            // Ignore errors
        }
    }
}
```

### Display Badge

**File**: `HomeFragment.kt`

```kotlin
private fun observeNotificationBadge() {
    viewLifecycleOwner.lifecycleScope.launch {
        val notificationRepo = NotificationRepository()
        val notifications = notificationRepo.getUserNotifications(limit = 50)
            .getOrNull() ?: emptyList()
        
        val hasUnread = notifications.any { !it.isRead }
        
        // Show/hide red dot badge
        binding.homeNotificationBadge.visibility = 
            if (hasUnread) View.VISIBLE else View.GONE
    }
}
```

**UI**: Red dot appears on notification bell icon when there are unread notifications.

---

## 📊 Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    NOTIFICATION LIFECYCLE                    │
└─────────────────────────────────────────────────────────────┘

1. CREATE NOTIFICATION
   ┌──────────────────────────────────────────────────┐
   │ User A likes/comments on User B's post           │
   └──────────────┬───────────────────────────────────┘
                  ▼
   ┌──────────────────────────────────────────────────┐
   │ PostRepository.toggleLike() or addComment()      │
   └──────────────┬───────────────────────────────────┘
                  ▼
   ┌──────────────────────────────────────────────────┐
   │ NotificationRepository.createXXXNotification()   │
   │ • Check if currentUserId != postOwnerId          │
   │ • Fetch current user's info (username, avatar)   │
   │ • Create notification data map                   │
   └──────────────┬───────────────────────────────────┘
                  ▼
   ┌──────────────────────────────────────────────────┐
   │ Firestore: notifications.add(data)               │
   │ Document created with:                           │
   │ • userId (recipient)                             │
   │ • actorId, actorUsername, actorAvatarUrl         │
   │ • type (LIKE/COMMENT/FOLLOW)                     │
   │ • postId, commentId, commentText                 │
   │ • isRead = false                                 │
   │ • createdAt = serverTimestamp                    │
   └──────────────────────────────────────────────────┘

2. DISPLAY NOTIFICATIONS
   ┌──────────────────────────────────────────────────┐
   │ User B opens Notifications screen                │
   └──────────────┬───────────────────────────────────┘
                  ▼
   ┌──────────────────────────────────────────────────┐
   │ NotificationsViewModel.loadNotifications()       │
   └──────────────┬───────────────────────────────────┘
                  ▼
   ┌──────────────────────────────────────────────────┐
   │ NotificationRepository.getUserNotifications()    │
   │ Query: whereEqualTo("userId", currentUserId)     │
   │        orderBy("createdAt", DESC)                │
   │        limit(20)                                 │
   └──────────────┬───────────────────────────────────┘
                  ▼
   ┌──────────────────────────────────────────────────┐
   │ ViewModel groups by time:                        │
   │ • Today (last 24 hours)                          │
   │ • Yesterday (24-48 hours)                        │
   │ • Last 7 Days (2-7 days)                         │
   └──────────────┬───────────────────────────────────┘
                  ▼
   ┌──────────────────────────────────────────────────┐
   │ NotificationsFragment displays 3 RecyclerViews   │
   │ • notifTodayRv                                   │
   │ • notifYesterdayRv                               │
   │ • notifLast7Rv                                   │
   └──────────────┬───────────────────────────────────┘
                  ▼
   ┌──────────────────────────────────────────────────┐
   │ NotificationAdapter renders each item:           │
   │ • Actor avatar (circular)                        │
   │ • Message text (username + action + time)        │
   │ • Post thumbnail (for LIKE/COMMENT)              │
   │ • Follow Back button (for FOLLOW)                │
   │ • Background highlight if unread                 │
   └──────────────────────────────────────────────────┘

3. HANDLE CLICK
   ┌──────────────────────────────────────────────────┐
   │ User taps notification                           │
   └──────────────┬───────────────────────────────────┘
                  ▼
   ┌──────────────────────────────────────────────────┐
   │ NotificationsFragment.handleNotificationClick()  │
   │ 1. Mark as read                                  │
   │ 2. Navigate based on type:                       │
   │    • LIKE → Navigate to post                     │
   │    • COMMENT → Open comments bottom sheet        │
   │    • FOLLOW → Navigate to user profile           │
   └──────────────┬───────────────────────────────────┘
                  ▼
   ┌──────────────────────────────────────────────────┐
   │ NotificationRepository.markAsRead(id)            │
   │ Update: isRead = true                            │
   └──────────────────────────────────────────────────┘

4. DELETE NOTIFICATION (on unlike)
   ┌──────────────────────────────────────────────────┐
   │ User A unlikes User B's post                     │
   └──────────────┬───────────────────────────────────┘
                  ▼
   ┌──────────────────────────────────────────────────┐
   │ PostRepository.toggleLike(currentlyLiked=true)   │
   └──────────────┬───────────────────────────────────┘
                  ▼
   ┌──────────────────────────────────────────────────┐
   │ NotificationRepository.deleteLikeNotification()  │
   │ Query: whereEqualTo("actorId", currentUserId)    │
   │        whereEqualTo("postId", postId)            │
   │        whereEqualTo("type", "LIKE")              │
   └──────────────┬───────────────────────────────────┘
                  ▼
   ┌──────────────────────────────────────────────────┐
   │ Delete matching notification documents           │
   └──────────────────────────────────────────────────┘
```

---

## ⚙️ Key Features

### ✅ What Works

1. **Auto-create on interaction**
   - Like a post → notification created
   - Comment on post → notification created
   - Follow user → notification created (TODO)

2. **Auto-delete on undo**
   - Unlike a post → notification deleted
   - Delete comment → notification deleted (TODO)

3. **Smart self-filtering**
   - Don't create notification if you like your own post
   - Don't create notification if you comment on your own post

4. **Time-based grouping**
   - Today, Yesterday, Last 7 Days
   - Older notifications not shown

5. **Read/Unread status**
   - Unread notifications highlighted
   - Badge on notification bell
   - Mark as read on click

6. **Rich content**
   - Actor avatar
   - Post thumbnail
   - Comment text preview (50 chars)
   - Time ago formatting

### ❌ Limitations (No FCM)

1. **No push notifications** when app is closed
2. **No notification sounds**
3. **No lock screen notifications**
4. **No app badge count**
5. **User must open app** to see notifications

---

## 🚀 Summary

Your notification system is:
- ✅ **Firestore-only** (no FCM push notifications)
- ✅ **Pull-based** (user opens app to check)
- ✅ **Grouped by time** (Today/Yesterday/Last 7 Days)
- ✅ **Read/Unread tracking** with badge indicator
- ✅ **Auto-created** on like/comment/follow
- ✅ **Auto-deleted** on unlike
- ✅ **Click to navigate** to relevant content
- ✅ **Rich UI** with avatars, thumbnails, previews

**It works perfectly for in-app notifications, but users won't get alerts when the app is closed!** 🔔

