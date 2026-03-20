package com.apollo.socially.data.upload

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object UploadStateHolder {

    sealed class UploadState {
        object Idle : UploadState()
        data class Uploading(val percent: Int, val current: Int, val total: Int) : UploadState()
        data class Success(val postId: String) : UploadState()
        data class Error(val message: String) : UploadState()
    }

    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state: StateFlow<UploadState> = _state
    
    private var lastUploadIntent: Intent? = null

    fun setState(state: UploadState) { _state.value = state }
    
    fun reset() { 
        _state.value = UploadState.Idle 
    }
    
    fun setLastUploadIntent(intent: Intent) {
        lastUploadIntent = intent
    }
    
    fun getLastUploadIntent(): Intent? = lastUploadIntent
    
    fun clearUploadIntent() {
        lastUploadIntent = null
    }
}
