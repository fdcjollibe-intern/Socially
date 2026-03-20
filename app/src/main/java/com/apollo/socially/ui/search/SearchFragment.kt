package com.apollo.socially.ui.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollo.socially.R
import com.apollo.socially.databinding.FragmentSearchBinding
import com.apollo.socially.model.UiUserModel
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var searchAdapter: SearchUserAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        setupSearch()
        observeViewModel()
    }

    private fun setupList() {
        searchAdapter = SearchUserAdapter(
            onUserClick = { user ->
                val bundle = Bundle().apply {
                    putString("userId", user.id)
                }
                findNavController().navigate(R.id.action_search_to_otherProfile, bundle)
            },
            onFollowClick = { user, isFollowing ->
                viewModel.toggleFollow(user.id, isFollowing) { }
            },
            showFollowButton = false
        )
        
        val layoutManager = LinearLayoutManager(requireContext())
        
        binding.searchResultsRv.apply {
            this.layoutManager = layoutManager
            adapter = searchAdapter
            
            // Add scroll listener for pagination
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    val currentState = viewModel.uiState.value
                    
                    // Load more when scrolled to near bottom
                    if (currentState is SearchViewModel.UiState.Success 
                        && !currentState.isLoadingMore 
                        && currentState.hasMore
                        && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 2
                        && firstVisibleItemPosition >= 0) {
                        
                        viewModel.loadMore()
                    }
                }
            })
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                // Trigger search with 800ms debouncing
                viewModel.search(query)
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is SearchViewModel.UiState.Idle -> {
                        // Show empty state or suggestions
                        binding.searchResultsRv.visibility = View.GONE
                        binding.searchEmptyState.visibility = View.VISIBLE
                        binding.searchLoadingIndicator.visibility = View.GONE
                    }
                    is SearchViewModel.UiState.Searching -> {
                        // Show loading indicator
                        binding.searchLoadingIndicator.visibility = View.VISIBLE
                        binding.searchEmptyState.visibility = View.GONE
                    }
                    is SearchViewModel.UiState.Success -> {
                        binding.searchLoadingIndicator.visibility = View.GONE
                        
                        if (state.users.isEmpty()) {
                            binding.searchResultsRv.visibility = View.GONE
                            binding.searchEmptyState.visibility = View.VISIBLE
                        } else {
                            binding.searchResultsRv.visibility = View.VISIBLE
                            binding.searchEmptyState.visibility = View.GONE
                            
                            // Convert UserWithFollowState to UiUserModel
                            val uiUsers = state.users.map { userWithState ->
                                UiUserModel(
                                    id = userWithState.user.uid,
                                    fullName = userWithState.user.displayName,
                                    username = userWithState.user.username,
                                    avatarUrl = userWithState.user.profileImageUrl,
                                    isVerified = userWithState.user.isVerified,
                                    isFollowing = userWithState.isFollowing
                                )
                            }
                            searchAdapter.submitList(uiUsers)
                        }
                    }
                    is SearchViewModel.UiState.Error -> {
                        binding.searchLoadingIndicator.visibility = View.GONE
                        binding.searchResultsRv.visibility = View.GONE
                        binding.searchEmptyState.visibility = View.VISIBLE
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
