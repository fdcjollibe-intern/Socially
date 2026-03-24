package com.apollo.socially.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollo.socially.R
import com.apollo.socially.databinding.FragmentNotificationsBinding
import com.apollo.socially.domain.model.Notification
import com.apollo.socially.domain.model.NotificationType
import com.apollo.socially.ui.post.CommentsBottomSheet
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: NotificationsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.notifBtnBack.setOnClickListener { findNavController().popBackStack() }
        
        observeViewModel()
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                android.util.Log.d("NotificationsFragment", "State: $state")
                
                when (state) {
                    is NotificationsViewModel.UiState.Loading -> {
                        android.util.Log.d("NotificationsFragment", "Loading...")
                    }
                    is NotificationsViewModel.UiState.Success -> {
                        android.util.Log.d("NotificationsFragment", "Success - Today: ${state.todayNotifications.size}, Yesterday: ${state.yesterdayNotifications.size}, Last7: ${state.last7DaysNotifications.size}")
                        
                        setupGroup(binding.notifTodayRv, state.todayNotifications)
                        setupGroup(binding.notifYesterdayRv, state.yesterdayNotifications)
                        setupGroup(binding.notifLast7Rv, state.last7DaysNotifications)
                        
                        // Show/hide sections based on content
                        binding.notifTodayRv.visibility = if (state.todayNotifications.isNotEmpty()) View.VISIBLE else View.GONE
                        
                        binding.notifYesterdayHeader.visibility = if (state.yesterdayNotifications.isNotEmpty()) View.VISIBLE else View.GONE
                        binding.notifYesterdayRv.visibility = if (state.yesterdayNotifications.isNotEmpty()) View.VISIBLE else View.GONE
                        
                        binding.notifLast7Header.visibility = if (state.last7DaysNotifications.isNotEmpty()) View.VISIBLE else View.GONE
                        binding.notifLast7Rv.visibility = if (state.last7DaysNotifications.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                    is NotificationsViewModel.UiState.Error -> {
                        android.util.Log.e("NotificationsFragment", "Error: ${state.message}")
                    }
                }
            }
        }
    }

    private fun setupGroup(rv: androidx.recyclerview.widget.RecyclerView, data: List<Notification>) {
        val adapter = NotificationAdapter { notification ->
            handleNotificationClick(notification)
        }
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        rv.isNestedScrollingEnabled = false
        adapter.submitList(data)
    }
    
    private fun handleNotificationClick(notification: Notification) {
        // Mark as read
        viewModel.markAsRead(notification.id)
        
        when (notification.type) {
            NotificationType.LIKE -> {
                // Navigate to post
                notification.postId?.let { postId ->
                    // Navigate to profile and scroll to post
                    // TODO: Implement navigation to specific post
                }
            }
            NotificationType.COMMENT -> {
                // Open comments modal with highlight
                notification.postId?.let { postId ->
                    val bottomSheet = CommentsBottomSheet.newInstance(
                        postId = postId,
                        likeCount = 0,
                        isLiked = false
                    )
                    // Pass the commentId to highlight
                    notification.commentId?.let { commentId ->
                        bottomSheet.arguments?.putString("highlightCommentId", commentId)
                    }
                    bottomSheet.show(childFragmentManager, "comments")
                }
            }
            NotificationType.FOLLOW -> {
                // Navigate to user profile
                // TODO: Add navigation action to other profile
                // For now, just mark as read
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
