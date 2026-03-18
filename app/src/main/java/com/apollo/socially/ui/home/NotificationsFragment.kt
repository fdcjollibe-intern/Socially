package com.apollo.socially.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollo.socially.R
import com.apollo.socially.databinding.FragmentNotificationsBinding
import com.apollo.socially.model.NotificationModel
import com.apollo.socially.model.NotifType

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

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

        setupGroup(binding.notifTodayRv, todayNotifs())
        setupGroup(binding.notifYesterdayRv, yesterdayNotifs())
        setupGroup(binding.notifLast7Rv, last7Notifs())
    }

    private fun setupGroup(rv: androidx.recyclerview.widget.RecyclerView, data: List<NotificationModel>) {
        val adapter = NotificationAdapter()
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter
        rv.isNestedScrollingEnabled = false
        adapter.submitList(data)
    }

    private fun todayNotifs() = listOf(
        NotificationModel("t1", R.drawable.user_profile_placeholder_avatar,
            "You have 10+ channel invites from people you follow. 14h",
            NotifType.GENERAL)
    )

    private fun yesterdayNotifs() = listOf(
        NotificationModel("y1", R.drawable.user_profile_placeholder_avatar,
            "vibeteller just shared a new post. 1d",
            NotifType.POST, R.drawable.sample_photo)
    )

    private fun last7Notifs() = listOf(
        NotificationModel("l1", R.drawable.user_profile_placeholder_avatar,
            "mooddreamlms started following you. 1d",
            NotifType.FOLLOW),
        NotificationModel("l2", R.drawable.user_profile_placeholder_avatar,
            "sunsetvibes just shared a new post. 2d",
            NotifType.POST, R.drawable.sample_photo),
        NotificationModel("l3", R.drawable.user_profile_placeholder_avatar,
            "calmwaves and vibeteller liked your post. 3d",
            NotifType.LIKE, R.drawable.sample_photo),
        NotificationModel("l4", R.drawable.user_profile_placeholder_avatar,
            "goldenhour_ just shared a new post. 4d",
            NotifType.POST, R.drawable.sample_photo),
        NotificationModel("l5", R.drawable.user_profile_placeholder_avatar,
            "dreamy.lens started following you. 5d",
            NotifType.FOLLOW),
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
