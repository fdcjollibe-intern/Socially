package com.apollo.socially.utils

import com.apollo.socially.model.MediaItem

/**
 * In-memory post draft store.
 * Everything here vanishes when the process dies — intentional for this UX stage.
 */
object PostDraftStore {

    data class Draft(
        val mediaItems: List<MediaItem>,
        val caption: String
    )

    private var currentDraft: Draft? = null

    fun saveDraft(mediaItems: List<MediaItem>, caption: String) {
        currentDraft = Draft(mediaItems, caption)
    }

    fun getDraft(): Draft? = currentDraft

    fun clearDraft() {
        currentDraft = null
    }
}
