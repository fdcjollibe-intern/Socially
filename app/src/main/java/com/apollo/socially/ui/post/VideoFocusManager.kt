package com.apollo.socially.ui.post

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VideoFocusManager(
    private val recyclerView: RecyclerView,
    private val getAdapter: () -> PostCardAdapter?
) : DefaultLifecycleObserver {

    private var currentPlayingPosition = -1
    private var isAttached = false

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
            when (newState) {
                RecyclerView.SCROLL_STATE_IDLE -> updateFocus()
                RecyclerView.SCROLL_STATE_DRAGGING -> pauseAll()
            }
        }

    }

    fun attach() {
        if (isAttached) return
        recyclerView.addOnScrollListener(scrollListener)
        isAttached = true
    }

    fun detach() {
        recyclerView.removeOnScrollListener(scrollListener)
        releaseAllVisible()
        isAttached = false
    }

    fun updateFocus() {
        val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
        if (recyclerView.childCount == 0) return

        val rvCenter = recyclerView.height / 2
        var bestPosition = -1
        var bestDistance = Int.MAX_VALUE

        val first = lm.findFirstVisibleItemPosition()
        val last = lm.findLastVisibleItemPosition()
        if (first == RecyclerView.NO_POSITION) return

        for (i in first..last) {
            val view = lm.findViewByPosition(i) ?: continue
            val viewCenter = (view.top + view.bottom) / 2
            val distance = Math.abs(viewCenter - rvCenter)
            if (distance < bestDistance) {
                bestDistance = distance
                bestPosition = i
            }
        }

        if (bestPosition == -1) return

        // Pause previous if changed
        if (bestPosition != currentPlayingPosition) {
            getHolder(currentPlayingPosition)?.pauseVideo()
            currentPlayingPosition = bestPosition
        }

        getHolder(currentPlayingPosition)?.resumeVideo()
    }

    fun pauseAll() {
        val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val first = lm.findFirstVisibleItemPosition()
        val last = lm.findLastVisibleItemPosition()
        if (first == RecyclerView.NO_POSITION) return
        for (i in first..last) {
            getHolder(i)?.pauseVideo()
        }
    }

    private fun releaseAllVisible() {
        val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return
        val first = lm.findFirstVisibleItemPosition()
        val last = lm.findLastVisibleItemPosition()
        if (first == RecyclerView.NO_POSITION) return
        for (i in first..last) {
            getHolder(i)?.releasePlayer()
        }
        currentPlayingPosition = -1
    }

    private fun getHolder(position: Int): PostCardAdapter.PostViewHolder? {
        if (position < 0) return null
        return recyclerView.findViewHolderForAdapterPosition(position)
                as? PostCardAdapter.PostViewHolder
    }

    override fun onPause(owner: LifecycleOwner) = pauseAll()
    override fun onResume(owner: LifecycleOwner) = updateFocus()
    override fun onStop(owner: LifecycleOwner) = pauseAll()
    override fun onDestroy(owner: LifecycleOwner) = detach()
}