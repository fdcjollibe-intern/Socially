package com.apollo.socially.ui.post

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.apollo.socially.R
import com.apollo.socially.databinding.ItemPostImagePagerBinding
import com.apollo.socially.databinding.ItemPostVideoPagerBinding
import com.apollo.socially.model.PostModel
import com.apollo.socially.utils.CloudinaryUtils
import com.bumptech.glide.Glide

class PostMediaPagerAdapter(
    private val urls: List<String>,
    private var isMuted: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Key = pager position, Value = ExoPlayer
    private val playerPool = mutableMapOf<Int, ExoPlayer>()
    private var activePagerPosition = 0 // which page is currently shown
    private var isCardFocused = false   // whether this card has VideoFocusManager focus

    companion object {
        private const val TYPE_IMAGE = 0
        private const val TYPE_VIDEO = 1
        private val typeHelper = PostModel("", "", "", "")
    }

    override fun getItemViewType(position: Int): Int =
        if (typeHelper.isVideoUrl(urls[position])) TYPE_VIDEO else TYPE_IMAGE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_VIDEO) {
            VideoViewHolder(
                ItemPostVideoPagerBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        } else {
            ImageViewHolder(
                ItemPostImagePagerBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ImageViewHolder -> holder.bind(urls[position])
            is VideoViewHolder -> {
                // NEVER auto-play on bind — only show thumbnail
                // Playing is controlled externally by VideoFocusManager
                holder.bind(urls[position], position)
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is VideoViewHolder) holder.detachPlayer()
    }

    override fun getItemCount() = urls.size

    // Called when user swipes to a new page within THIS post's pager
    fun onPageSelected(pagerPosition: Int) {
        // Pause whatever was on the previous page
        val prevUrl = urls.getOrNull(activePagerPosition)
        if (prevUrl != null && typeHelper.isVideoUrl(prevUrl)) {
            playerPool[activePagerPosition]?.pause()
        }

        activePagerPosition = pagerPosition

        // If card is focused and new page is a video, play it
        val newUrl = urls.getOrNull(activePagerPosition)
        if (isCardFocused && newUrl != null && typeHelper.isVideoUrl(newUrl)) {
            val player = playerPool[activePagerPosition]
            if (player != null) {
                player.playWhenReady = true
                player.play()
            }
            // If null, bind() hasn't run yet — isCardFocused=true will handle it
        }
    }

    // Called by VideoFocusManager via PostCardAdapter.resumeVideo()
    fun resumeActive() {
        isCardFocused = true
        val isCurrentPageVideo = activePagerPosition < urls.size &&
                typeHelper.isVideoUrl(urls[activePagerPosition])
        if (!isCurrentPageVideo) return

        val player = playerPool[activePagerPosition]
        if (player != null) {
            player.playWhenReady = true
            player.play()
        }
    }

    // Called by VideoFocusManager via PostCardAdapter.pauseVideo()
    fun pauseAll() {
        isCardFocused = false
        playerPool.values.forEach { it.pause() }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        playerPool.values.forEach { it.volume = if (muted) 0f else 1f }
    }

    fun releaseAll() {
        isCardFocused = false
        playerPool.values.forEach { it.stop(); it.release() }
        playerPool.clear()
    }

    fun setActivePosition(pagerPosition: Int) = onPageSelected(pagerPosition)

    inner class ImageViewHolder(private val binding: ItemPostImagePagerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(url: String) {
            Glide.with(binding.root.context)
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.sample_photo)
                .into(binding.pagerImage)
        }
    }

    inner class VideoViewHolder(private val binding: ItemPostVideoPagerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var boundPosition = -1

        fun bind(url: String, position: Int) {
            val context = binding.root.context
            boundPosition = position

            // Load thumbnail
            val thumbnailUrl = CloudinaryUtils.videoToThumbnailUrl(url)
            Glide.with(context)
                .load(thumbnailUrl)
                .centerCrop()
                .placeholder(R.drawable.sample_photo)
                .into(binding.videoThumbnail)

            binding.videoThumbnail.visibility = View.VISIBLE
            binding.playerView.visibility = View.INVISIBLE

            // Get or create player — no listener here
            val player = playerPool.getOrPut(position) {
                ExoPlayer.Builder(context).build().apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    volume = if (isMuted) 0f else 1f
                    setMediaItem(MediaItem.fromUri(url))
                    prepare()
                    playWhenReady = false
                }
            }

            // Detach then reattach to reset surface
            binding.playerView.player = null
            binding.playerView.player = player

            // Attach a fresh listener to THIS binding every time
            // Remove previous listener first by using a tag approach
            binding.playerView.tag?.let { oldListener ->
                if (oldListener is Player.Listener) {
                    player.removeListener(oldListener)
                }
            }

            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (boundPosition != position) return // stale, ignore
                    binding.videoThumbnail.post {
                        if (isPlaying) {
                            binding.videoThumbnail.visibility = View.GONE
                            binding.playerView.visibility = View.VISIBLE
                        } else {
                            binding.videoThumbnail.visibility = View.VISIBLE
                        }
                    }
                }
            }

            // Store listener in tag so we can remove it next bind
            binding.playerView.tag = listener
            player.addListener(listener)

            // Play or pause based on focus state
            val isActivePage = position == activePagerPosition
            if (isCardFocused && isActivePage) {
                player.playWhenReady = true
            } else {
                player.pause()
                player.playWhenReady = false
            }
        }

        fun detachPlayer() {
            // Remove listener before detaching
            binding.playerView.tag?.let { oldListener ->
                if (oldListener is Player.Listener) {
                    binding.playerView.player?.removeListener(oldListener)
                }
            }
            binding.playerView.tag = null
            binding.playerView.player = null
        }
    }
}