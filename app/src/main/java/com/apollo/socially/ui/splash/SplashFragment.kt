package com.apollo.socially.ui.splash

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.apollo.socially.R
import com.apollo.socially.data.local.database.AppDatabase
import com.apollo.socially.data.repository.SessionRepositoryImpl
import com.apollo.socially.ui.main.MainContainerActivity
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {
    
    private val viewModel: SplashViewModel by viewModels {
        val database = AppDatabase.getInstance(requireContext())
        val sessionRepository = SessionRepositoryImpl(database.userSessionDao())
        SplashViewModelFactory(sessionRepository)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeNavigationState()
    }
    
    private fun observeNavigationState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationState.collect { state ->
                    when (state) {
                        is SplashNavigationState.Loading -> {
                            // Stay on splash screen
                        }
                        is SplashNavigationState.NavigateToLogin -> {
                            findNavController().navigate(R.id.action_splash_to_login)
                        }
                        is SplashNavigationState.NavigateToMain -> {
                            navigateToMainContainer()
                        }
                    }
                }
            }
        }
    }
    
    private fun navigateToMainContainer() {
        val intent = Intent(requireContext(), MainContainerActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        requireActivity().finish()
    }
}

