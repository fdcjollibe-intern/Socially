# 🎥 Video Autoplay System - Complete Documentation

## Overview

Your Socially app has an **intelligent video autoplay system** that automatically plays/pauses videos based on their visibility in the feed - similar to Instagram, TikTok, and Twitter.

---

## 📍 Location of Autoplay Logic

The autoplay system consists of **3 main components**:

```
┌────────────────────────────────────────────────────────────┐
│              VIDEO AUTOPLAY ARCHITECTURE                   │
└────────────────────────────────────────────────────────────┘

1. VideoFocusManager.kt
   └─ Tracks which post is in center of screen
   └─ Pauses/resumes videos on scroll
   └─ Lifecycle-aware (pauses on app background)

2. PostCardAdapter.kt
   └─ RecyclerView adapter for post cards
   └─ Exposes pauseVideo() / resumeVideo() methods
   └─ Manages video player lifecycle

3. PostMediaPagerAdapter.kt
   └─ ViewPager2 adapter for multi-image posts
   └─ Contains ExoPlayer instances
   └─ Actual video playback implementation
```

---

## 🎯 How Autoplay Works

### Flow Diagram

```
User scrolls feed
        ↓
VideoFocusManager detects scroll
        ↓
Calculate which post is in center of screen
        ↓
┌─────────────────┴─────────────────┐
│                                   │
Post changed?                  Same post?
│                                   │
▼                                   ▼
Pause previous post           Resume current post
Resume new post              (in case RecyclerView recycled it)
        │                             │
        └─────────────┬───────────────┘
                      ▼
        PostCardAdapter.resumeVideo()
                      ▼
        PostMediaPagerAdapter.resumeActive()
                      ▼
        ExoPlayer.play()
                      ▼
        🎬 Video plays automatically!
```

---

## 🔧 Component 1: VideoFocusManager

**File**: `ui/post/VideoFocusManager.kt`

### Purpose
Monitors RecyclerView scroll and determines which post should play video.

### Key Features

#### 1. **Center-Based Detection**

```kotlin
fun updateFocus() {
    val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
    if (recyclerView.childCount == 0) return

    val rvCenter = recyclerView.height / 2  // Find center of screen
    var bestPosition = -1
    var bestDistance = Int.MAX_VALUE

    // Find post closest to center
    for (i in first..last) {
        val view = lm.findViewByPosition(i) ?: continue
        val viewCenter = (view.top + view.bottom) / 2
        val distance = Math.abs(viewCenter - rvCenter)
        
        if (distance < bestDistance) {
            bestDistance = distance
            bestPosition = i  // This post is closest to center
        }
    }
    
    // Resume video for closest post
    getHolder(bestPosition)?.resumeVideo()
}
```

**How it works:**
- Calculates center Y-position of RecyclerView
- Checks all visible posts
- Finds which post's center is closest to RecyclerView center
- Plays video for that post only

---

#### 2. **Scroll State Handling**

```kotlin
private val scrollListener = object : RecyclerView.OnScrollListener() {
    override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
        when (newState) {
            RecyclerView.SCROLL_STATE_IDLE -> updateFocus()  // User stopped scrolling
            RecyclerView.SCROLL_STATE_DRAGGING -> pauseAll() // User is scrolling
        }
    }
}
```

**States:**
- **IDLE**: User stopped scrolling → Find centered post and play video
- **DRAGGING**: User is actively scrolling → Pause all videos for performance
- **SETTLING**: Fling scroll (ignored, waits for IDLE)

---

#### 3. **Lifecycle Awareness**

```kotlin
class VideoFocusManager : DefaultLifecycleObserver {
    override fun onPause(owner: LifecycleOwner) = pauseAll()
    override fun onResume(owner: LifecycleOwner) = updateFocus()
    override fun onStop(owner: LifecycleOwner) = pauseAll()
    override fun onDestroy(owner: LifecycleOwner) = detach()
}
```

**Automatically handles:**
- ✅ App goes to background → Pause all videos
- ✅ App comes back → Resume centered video
- ✅ Fragment destroyed → Clean up listeners
- ✅ Screen off → Pause videos

---

#### 4. **RecyclerView Recycling Fix**

```kotlin
// ALWAYS resume current — fixes the "only first video plays" bug
// because RecyclerView may have recycled and rebound the holder
getHolder(currentPlayingPosition)?.resumeVideo()
```

**Why this is important:**
- RecyclerView recycles views as you scroll
- A recycled ViewHolder might have lost its playing state
- Always calling `resumeVideo()` ensures video plays even after recycling

---

## 🔧 Component 2: PostCardAdapter

**File**: `ui/post/PostCardAdapter.kt`

### Purpose
RecyclerView adapter that displays post cards and manages video lifecycle.

### Video-Related Methods

#### 1. **resumeVideo()** - Play video

```kotlin
inner class PostViewHolder(binding: ItemPostCardBinding) : RecyclerView.ViewHolder(binding.root) {
    
    private var currentAdapter: PostMediaPagerAdapter? = null
    
    fun resumeVideo() {
        currentAdapter?.resumeActive()  // Delegate to media adapter
    }
}
```

**Called by**: `VideoFocusManager` when this post is centered

---

#### 2. **pauseVideo()** - Pause video

```kotlin
fun pauseVideo() {
    currentAdapter?.pauseAll()  // Pause all videos in this post
}
```

**Called by**: `VideoFocusManager` when scrolling or when post leaves center

---

#### 3. **releasePlayer()** - Clean up ExoPlayer

```kotlin
fun releasePlayer() {
    currentAdapter?.releaseAll()  // Release ExoPlayer instances
    currentAdapter = null
}
```

**Called by**: `onViewDetachedFromWindow()` when post leaves the screen

---

### RecyclerView Lifecycle Callbacks

```kotlin
override fun onViewRecycled(holder: PostViewHolder) {
    super.onViewRecycled(holder)
    holder.pauseVideo()  // Pause when view is recycled
}

override fun onViewDetachedFromWindow(holder: PostViewHolder) {
    super.onViewDetachedFromWindow(holder)
    holder.releasePlayer()  // Release when view leaves window
}
```

**Why both?**
- `onViewRecycled()`: View will be reused for another post (pause)
- `onViewDetachedFromWindow()`: View is gone (release player to free memory)

---

## 🔧 Component 3: PostMediaPagerAdapter

**File**: `ui/post/PostMediaPagerAdapter.kt`

### Purpose
ViewPager2 adapter for multi-image posts. Contains actual ExoPlayer instances.

### Key Features

#### 1. **ExoPlayer Pool**

```kotlin
// Key = pager position, Value = ExoPlayer
private val playerPool = mutableMapOf<Int, ExoPlayer>()
```

**Why a pool?**
- Each post can have multiple videos (ViewPager2)
- Each video needs its own ExoPlayer instance
- Map stores players by position (0, 1, 2, etc.)

---

#### 2. **resumeActive()** - Play current page video

```kotlin
fun resumeActive() {
    if (!isCardFocused) return  // Card not in focus
    
    val currentPosition = getCurrentPosition()
    val player = playerPool[currentPosition] ?: return
    
    if (!player.isPlaying) {
        player.play()  // ⭐ Start video playback
    }
}
```

**Called by**: `PostCardAdapter.resumeVideo()` when post is centered

---

#### 3. **pauseAll()** - Pause all videos

```kotlin
fun pauseAll() {
    playerPool.values.forEach { player ->
        if (player.isPlaying) {
            player.pause()  // ⭐ Pause video
        }
    }
}
```

**Called by**: `PostCardAdapter.pauseVideo()` when scrolling or leaving center

---

#### 4. **releaseAll()** - Clean up all players

```kotlin
fun releaseAll() {
    playerPool.values.forEach { it.release() }  // Free ExoPlayer resources
    playerPool.clear()
}
```

**Called by**: `PostCardAdapter.releasePlayer()` when view is destroyed

---

#### 5. **ExoPlayer Creation**

```kotlin
// Create ExoPlayer on demand
ExoPlayer.Builder(context).build().apply {
    setMediaItem(MediaItem.fromUri(videoUrl))
    repeatMode = Player.REPEAT_MODE_ALL  // Loop video
    volume = if (isMuted) 0f else 1f     // Mute state
    prepare()
    // Note: NOT calling play() here - controlled by VideoFocusManager
}
```

**Key points:**
- Created lazily (only when video page is viewed)
- Repeat mode enabled (loops video)
- Respects mute state
- Prepared but not playing (waits for focus)

---

## 🎬 Complete Autoplay Flow

### Scenario: User Opens Home Feed

```
1. HomeFragment.onViewCreated()
        ↓
2. Create VideoFocusManager
        ↓
   videoFocusManager = VideoFocusManager(
       recyclerView = binding.homeFeedRv,
       getAdapter = { postAdapter }
   )
        ↓
3. Attach to RecyclerView
        ↓
   videoFocusManager.attach()
        ↓
4. Add lifecycle observer
        ↓
   lifecycle.addObserver(videoFocusManager)
        ↓
5. Posts loaded into RecyclerView
        ↓
   postAdapter.submitList(state.posts)
        ↓
6. Update focus to find centered post
        ↓
   videoFocusManager.updateFocus()
        ↓
7. VideoFocusManager finds centered post (position 0)
        ↓
8. Resume video for position 0
        ↓
   getHolder(0)?.resumeVideo()
        ↓
9. PostCardAdapter delegates to media adapter
        ↓
   currentAdapter?.resumeActive()
        ↓
10. PostMediaPagerAdapter plays ExoPlayer
        ↓
    player.play()
        ↓
11. 🎬 First video autoplays!
```

---

### Scenario: User Scrolls Down

```
1. User scrolls feed
        ↓
2. ScrollListener detects DRAGGING
        ↓
   onScrollStateChanged(SCROLL_STATE_DRAGGING)
        ↓
3. Pause all videos for performance
        ↓
   pauseAll()
        ↓
4. User stops scrolling
        ↓
   onScrollStateChanged(SCROLL_STATE_IDLE)
        ↓
5. Find new centered post
        ↓
   updateFocus()
        ↓
6. Calculate which post is closest to center
        ↓
   Position 2 is now centered (was position 0)
        ↓
7. Pause old position (0)
        ↓
   getHolder(0)?.pauseVideo()
        ↓
8. Resume new position (2)
        ↓
   getHolder(2)?.resumeVideo()
        ↓
9. 🎬 New video autoplays!
```

---

### Scenario: User Leaves App

```
1. User presses home button
        ↓
2. Fragment lifecycle: onPause()
        ↓
3. VideoFocusManager.onPause() called (LifecycleObserver)
        ↓
   override fun onPause(owner: LifecycleOwner) = pauseAll()
        ↓
4. Pause all visible videos
        ↓
   for (i in first..last) {
       getHolder(i)?.pauseVideo()
   }
        ↓
5. 🎬 Videos paused (saves battery)
```

---

## 🎨 Setup in HomeFragment

**File**: `ui/home/HomeFragment.kt` (Lines 152-159)

```kotlin
// Setup video focus manager
videoFocusManager = VideoFocusManager(
    recyclerView = binding.homeFeedRv,
    getAdapter = { postAdapter }  // Lambda to get adapter dynamically
)

// Attach scroll listener
videoFocusManager.attach()

// Add lifecycle observer (pauses on background, resumes on foreground)
viewLifecycleOwner.lifecycle.addObserver(videoFocusManager)
```

### After Posts Load

```kotlin
// When new posts are loaded
viewModel.uiState.collect { state ->
    when (state) {
        is HomeViewModel.UiState.Success -> {
            postAdapter.submitList(state.posts)
            
            // ⭐ IMPORTANT: Update focus after posts load
            binding.homeFeedRv.post {
                videoFocusManager.updateFocus()
            }
        }
    }
}
```

**Why `post {}`?**
- RecyclerView needs time to layout items
- `post {}` waits for next frame
- Ensures views are measured before checking positions

---

## 📊 Autoplay States

### State Machine

```
┌─────────────┐
│   NO VIDEO  │  (Image-only post)
└─────────────┘

┌─────────────┐
│   PREPARED  │  (ExoPlayer created, buffering)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   PLAYING   │  (Video is playing - post is centered)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   PAUSED    │  (User scrolling or post not centered)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  RELEASED   │  (Post left screen, memory freed)
└─────────────┘
```

---

## 🎯 Key Design Decisions

### 1. **Why Center-Based Detection?**

```kotlin
val rvCenter = recyclerView.height / 2
val viewCenter = (view.top + view.bottom) / 2
val distance = Math.abs(viewCenter - rvCenter)
```

**Benefits:**
- ✅ Plays video that user is actually viewing
- ✅ Smooth transitions (no sudden starts/stops)
- ✅ Works with any post size
- ✅ Similar to Instagram/TikTok behavior

---

### 2. **Why Pause on Scroll?**

```kotlin
RecyclerView.SCROLL_STATE_DRAGGING -> pauseAll()
```

**Reasons:**
- ✅ Saves battery (no video decoding while scrolling)
- ✅ Better scroll performance (less CPU usage)
- ✅ Prevents jank during fast scrolling
- ✅ User isn't watching anyway (focused on scrolling)

---

### 3. **Why Player Pool Instead of Single Player?**

```kotlin
private val playerPool = mutableMapOf<Int, ExoPlayer>()
```

**Why not reuse one player?**
- ❌ Post can have multiple videos (ViewPager2)
- ❌ Switching video sources causes flicker
- ❌ Lose buffered data when switching
- ✅ Pool allows smooth transitions between pages
- ✅ Each video keeps its playback position

---

### 4. **Why Release on Detach?**

```kotlin
override fun onViewDetachedFromWindow(holder: PostViewHolder) {
    holder.releasePlayer()  // Free memory
}
```

**Prevents memory leaks:**
- ExoPlayer holds native resources
- Unreleased players consume memory
- Many posts in feed = many players
- Release when off-screen = better performance

---

## 🐛 Bug Fixes in Current Implementation

### Bug Fix 1: "Only First Video Plays"

**Problem**: After scrolling, videos wouldn't play

**Root Cause**: RecyclerView recycled ViewHolders, losing playing state

**Fix**:
```kotlin
// ALWAYS resume current — fixes recycling bug
getHolder(currentPlayingPosition)?.resumeVideo()
```

**Comment in code (Line 69)**:
```kotlin
// ALWAYS resume current — fixes the "only first video plays" bug
// because RecyclerView may have recycled and rebound the holder
```

---

### Bug Fix 2: "Videos Won't Resume After Scroll"

**Problem**: Videos paused during scroll wouldn't resume when idle

**Root Cause**: Removed `onScrolled()` pause (was too aggressive)

**Fix**:
```kotlin
// Removed onScrolled pause — was too aggressive and prevented resuming
```

**Comment in code (Line 21)**:
```kotlin
// Removed onScrolled pause — was too aggressive and prevented resuming
```

---

## 🎛️ Mute Feature

### Implementation

```kotlin
private var isMuted = false  // Global mute state in adapter

binding.postCardBtnMute.setOnClickListener {
    isMuted = !isMuted
    updateMuteButton()
    currentAdapter?.setMuted(isMuted)  // Apply to current video
}

// In PostMediaPagerAdapter
fun setMuted(muted: Boolean) {
    isMuted = muted
    playerPool.values.forEach { player ->
        player.volume = if (muted) 0f else 1f
    }
}
```

**Features:**
- Mute button visible only on video posts
- Persists across videos in same session
- Instant mute toggle (no restart needed)

---

## 📱 Usage in Other Screens

### ProfilePostFeedFragment

```kotlin
// Same autoplay system used in profile feed
val focusManager = VideoFocusManager(
    recyclerView = binding.profileFeedRv,
    getAdapter = { postAdapter }
)
focusManager.attach()
viewLifecycleOwner.lifecycle.addObserver(focusManager)
```

**Reusable across app:**
- ✅ Home feed
- ✅ Profile post feed
- ✅ Any RecyclerView with post cards

---

## 🚀 Performance Optimizations

### 1. **Lazy Player Creation**

Players only created when video page is viewed:
```kotlin
val player = playerPool.getOrPut(position) {
    ExoPlayer.Builder(context).build().apply { /* setup */ }
}
```

### 2. **Pause on Scroll**

Reduces CPU usage during scrolling

### 3. **Release on Detach**

Frees memory when posts leave screen

### 4. **Player Pooling**

Reuses players within same post (ViewPager2 swipes)

### 5. **Offscreen Page Limit**

```kotlin
binding.postCardViewPager.offscreenPageLimit = 1
```

Only keeps adjacent pages in memory

---

## 🎓 Summary

Your video autoplay system is:

### ✅ Features
- **Smart detection** - Plays video closest to screen center
- **Lifecycle-aware** - Pauses on background, resumes on foreground
- **Performance-optimized** - Pauses during scroll, releases off-screen
- **RecyclerView-safe** - Handles view recycling correctly
- **Multi-video support** - Works with ViewPager2 in posts
- **Mute toggle** - Global mute state persists
- **Battery-friendly** - Pauses when app in background

### 🏗️ Architecture
```
VideoFocusManager (Brain)
        ↓
PostCardAdapter (Middleman)
        ↓
PostMediaPagerAdapter (Player)
        ↓
ExoPlayer (Actual playback)
```

### 🎯 Behavior
- Videos autoplay when centered on screen
- Pause during scrolling
- Resume when scroll stops
- One video plays at a time
- Loops automatically
- Respects mute state
- Cleans up properly

**This is the same autoplay system used by Instagram, TikTok, and Twitter!** 🎉

