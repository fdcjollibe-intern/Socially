package com.apollo.socially.data.upload

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object StoryUploadStateHolder {

    sealed class State {
        object Idle : State()
        data class Uploading(val percent: Int) : State()
        object Success : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    fun setState(state: State) { _state.value = state }
    fun reset() { _state.value = State.Idle }
}