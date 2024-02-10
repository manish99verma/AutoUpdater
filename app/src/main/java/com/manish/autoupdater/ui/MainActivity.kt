package com.manish.autoupdater.ui

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.manish.autoupdater.R
import com.manish.autoupdater.data.UpdatesManager
import com.manish.autoupdater.databinding.ActivityMainBinding
import com.manish.autoupdater.utils.Resource

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var storagePermissions: List<String>
    private lateinit var updatesManager: UpdatesManager

    private var downloadingDialog: DownloadingDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Permissions
        val currVersion = Build.VERSION.SDK_INT
        storagePermissions =
            if (currVersion <= 32) {
                listOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            } else {
                listOf()
            }

        updatesManager = UpdatesManager()

        // Check permissions
        if (permissionsAllowed(storagePermissions))
            updatesManager.checkForUpdates()
        else
            permissionsLauncher.launch(storagePermissions.toTypedArray())

        // Update Manager responses
        updatesManager.updateResultLiveData.observe(this) {
            val res = it.getContentIfNotHandled() ?: return@observe

            if (res.status == Resource.Status.ERROR) {
                val msg = res.message ?: resources.getString(R.string.unknown_error_occurred)

                downloadingDialog?.dismiss()
                // Todo -> show specific error
                binding.textView.text = resources.getString(R.string.internet_error_occurred)

                binding.retryButton.visibility = View.VISIBLE
            } else if (res.status == Resource.Status.LOADING) {
                val action = res.data
                if (action == UpdatesManager.Action.ACTION_CHECKING_FOR_UPDATES) {
                    binding.textView.text = getString(R.string.checking_for_updates)
                    binding.retryButton.visibility = View.GONE
                } else if (action == UpdatesManager.Action.ACTION_SHOW_DOWNLOADING_DIALOG) {
                    binding.textView.text = getString(R.string.downloading)
                    binding.retryButton.visibility = View.GONE

                    downloadingDialog = DownloadingDialog(this)
                    downloadingDialog?.show()
                }
            } else {
                when (res.data) {
                    UpdatesManager.Action.ACTION_UPDATE_AVAILABLE -> {
                        MaterialAlertDialogBuilder(this)
                            .setTitle(getString(R.string.update_title))
                            .setMessage(getString(R.string.update_msg))
                            .setPositiveButton(getString(R.string.update_title)) { dialog, which ->
                                updatesManager.downloadApk()
                                dialog.dismiss()
                            }
                            .setNegativeButton(getString(R.string.update_negative_btn)) { dialog, which ->
                                dialog.dismiss()

                                binding.textView.text = getString(R.string.update_cancelled)
                            }.setCancelable(false)
                            .show()
                    }

                    UpdatesManager.Action.ACTION_NO_UPDATES_NEEDED -> {
                        binding.textView.text = getString(R.string.already_on_latest_version)
                    }

                    UpdatesManager.Action.ACTION_INSTALL_APK -> {
                        updatesManager.installApp(this)
                    }

                    else -> {}
                }
            }
        }

        // Downloading Progress
        updatesManager.downloadingProgressLiveData.observe(this) {
            val res = it.getContentIfNotHandled()
            if (res == null) {
                downloadingDialog?.dismiss()
            } else {
                val data = res.data ?: return@observe
                downloadingDialog?.setProgress(data.bytesTransferred, data.totalByteCount)
            }
        }

        // Retry
        binding.retryButton.setOnClickListener {
            binding.retryButton.visibility = View.GONE

            if (permissionsAllowed(storagePermissions))
                updatesManager.checkForUpdates()
            else
                permissionsLauncher.launch(storagePermissions.toTypedArray())
        }
    }

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { map ->
            if (permissionsAllowed(storagePermissions)) {
                updatesManager.checkForUpdates()
            } else {
                binding.textView.text = getString(R.string.permissions_not_allowed)
                binding.retryButton.visibility = View.VISIBLE
            }
        }

    private fun permissionsAllowed(permissions: List<String>): Boolean {
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            ) return false
        }

        return true
    }

}