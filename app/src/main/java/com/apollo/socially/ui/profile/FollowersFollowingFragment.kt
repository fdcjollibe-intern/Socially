package com.apollo.socially.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.apollo.socially.R
import com.apollo.socially.databinding.FragmentFollowersFollowingBinding
import com.apollo.socially.databinding.FragmentFollowTabBinding
import com.apollo.socially.domain.model.User
import com.apollo.socially.model.UiUserModel
import com.apollo.socially.ui.search.SearchUserAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class FollowersFollowingFragment : Fragment() {

    private var _binding: FragmentFollowersFollowingBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: FollowersFollowingViewModel by viewModels()
    private var startTab: Int = 0
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startTab = arguments?.getInt("startTab", 0) ?: 0
        userId = arguments?.getString("userId") ?: FirebaseAuth.getInstance().currentUser?.uid
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFollowersFollowingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.followBtnBack.setOnClickListener { findNavController().popBackStack() }
        binding.followToolbarUsername.text = arguments?.getString("username") ?: "Loading..."

        setupViewPager()
        observeViewModel()
        
        userId?.let { viewModel.loadFollowData(it) }
    }

    private fun setupViewPager() {
        binding.followViewPager.adapter = FollowPagerAdapter(this)
        
        TabLayoutMediator(binding.followTabLayout, binding.followViewPager) { tab, position ->
            tab.text = if (position == 0) "Followers" else "Following"
        }.attach()

        binding.followViewPager.setCurrentItem(startTab, false)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is FollowersFollowingViewModel.UiState.Loading -> {
                        // Show loading state if needed
                    }
                    is FollowersFollowingViewModel.UiState.Success -> {
                        // Data updates are handled by the fragments observing the ViewModel
                    }
                    is FollowersFollowingViewModel.UiState.Error -> {
                        // Show error if needed
                    }
                }
            }
        }
    }

    inner class FollowPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount() = 2
        override fun createFragment(position: Int): Fragment =
            FollowTabFragment.newInstance(position, viewModel)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class FollowTabFragment : Fragment() {

    private var _binding: FragmentFollowTabBinding? = null
    private val binding get() = _binding!!
    
    private var tabType: Int = 0 // 0 for followers, 1 for following
    private var viewModel: FollowersFollowingViewModel? = null
    private lateinit var adapter: SearchUserAdapter

    companion object {
        fun newInstance(
            tabType: Int,
            viewModel: FollowersFollowingViewModel
        ) = FollowTabFragment().apply {
            this.tabType = tabType
            this.viewModel = viewModel
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFollowTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SearchUserAdapter(
            onUserClick = { user ->
                // Navigate to profile
            },
            onFollowClick = { user, isFollowing ->
                viewModel?.toggleFollow(user.id, isFollowing) { newState ->
                    // State update is handled by ViewModel
                }
            }
        )
        
        binding.followTabRv.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@FollowTabFragment.adapter
        }
        
        observeViewModel()
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel?.uiState?.collect { state ->
                when (state) {
                    is FollowersFollowingViewModel.UiState.Success -> {
                        val users = if (tabType == 0) state.followers else state.following
                        
                        if (users.isEmpty()) {
                            binding.followTabRv.visibility = View.GONE
                            binding.followTabEmptyState.visibility = View.VISIBLE
                        } else {
                            binding.followTabRv.visibility = View.VISIBLE
                            binding.followTabEmptyState.visibility = View.GONE
                            
                            // Convert UserWithFollowState to UiUserModel
                            val uiUsers = users.map { userWithState ->
                                UiUserModel(
                                    id = userWithState.user.uid,
                                    fullName = userWithState.user.displayName,
                                    username = userWithState.user.username,
                                    avatarUrl = userWithState.user.profileImageUrl,
                                    isVerified = userWithState.user.isVerified,
                                    isFollowing = userWithState.isFollowing
                                )
                            }
                            adapter.submitList(uiUsers)
                        }
                    }
                    else -> {
                        // Handle other states if needed
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

