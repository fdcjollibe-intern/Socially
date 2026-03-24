package com.apollo.socially.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.apollo.socially.R
import com.apollo.socially.data.cache.ProfileCache
import com.apollo.socially.data.local.database.AppDatabase
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private lateinit var database: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getInstance(requireContext())

        view.findViewById<View>(R.id.settings_btn_back).setOnClickListener {
            findNavController().popBackStack()
        }

        // Logout button
        view.findViewById<View>(R.id.settings_btn_logout).setOnClickListener {
            showLogoutConfirmation()
        }

        // TODO: wire up each row's click listeners as screens are built
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log out") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uid = auth.currentUser?.uid

                // 1. Clear Firebase Auth session
                auth.signOut()

                // 2. Clear Room Database
                uid?.let { database.userSessionDao().logout(it) }
                database.userSessionDao().clearAllSessions()

                // 3. Clear all in-memory caches
                ProfileCache.invalidateCache()

                // 4. Clear Glide cache
                Glide.get(requireContext()).clearMemory()
                launch {
                    Glide.get(requireContext()).clearDiskCache()
                }

                // 5. Clear app preferences if any
                requireContext().getSharedPreferences("app_prefs", 0)
                    .edit()
                    .clear()
                    .apply()

                // 6. Navigate to login and clear back stack
                val intent = Intent(requireContext(), com.apollo.socially.MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()

            } catch (e: Exception) {
                // Show error
                AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage("Failed to log out: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
}
