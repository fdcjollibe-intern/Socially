package com.apollo.socially.utils

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

object MediaPermissionHelper {

    fun requiredPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun checkAndRequest(
        fragment: Fragment,
        launcher: ActivityResultLauncher<Array<String>>,
        onGranted: () -> Unit
    ) {
        val permissions = requiredPermissions()
        val activity = fragment.requireActivity()
        val allGranted = permissions.all {
            ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            onGranted()
            return
        }
        val shouldShowRationale = permissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
        if (shouldShowRationale) {
            AlertDialog.Builder(activity)
                .setTitle("Media Access Needed")
                .setMessage("Socially needs access to your photos and videos to create posts.")
                .setPositiveButton("Allow") { _, _ -> launcher.launch(permissions) }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            val neverAsked = permissions.all {
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, it) &&
                ActivityCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
            }
            // Check if previously denied permanently (asked before but no rationale shown)
            val prefs = activity.getSharedPreferences("socially_prefs", 0)
            val askedBefore = prefs.getBoolean("media_permission_asked", false)
            if (askedBefore && neverAsked) {
                // Permanently denied — send to Settings
                AlertDialog.Builder(activity)
                    .setTitle("Permission Denied")
                    .setMessage("You've permanently denied media access. Please enable it in Settings to create posts.")
                    .setPositiveButton("Go to Settings") { _, _ ->
                        activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", activity.packageName, null)
                        })
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                prefs.edit().putBoolean("media_permission_asked", true).apply()
                launcher.launch(permissions)
            }
        }
    }

    fun handleResult(
        fragment: Fragment,
        results: Map<String, Boolean>,
        onGranted: () -> Unit
    ) {
        val activity = fragment.requireActivity()
        if (results.values.all { it }) {
            onGranted()
        } else {
            val permanentlyDenied = results.keys.none {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
            if (permanentlyDenied) {
                AlertDialog.Builder(activity)
                    .setTitle("Permission Denied")
                    .setMessage("Media access was denied. Go to Settings to enable it and create posts.")
                    .setPositiveButton("Go to Settings") { _, _ ->
                        activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", activity.packageName, null)
                        })
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
}
