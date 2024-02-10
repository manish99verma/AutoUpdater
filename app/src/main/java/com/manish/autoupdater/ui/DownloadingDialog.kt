package com.manish.autoupdater.ui

import android.app.Activity
import android.app.Dialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.manish.autoupdater.databinding.DownloadingDialogBinding
import kotlin.math.roundToLong


class DownloadingDialog(activity: Activity) {
    private var binding: DownloadingDialogBinding
    private var dialog: Dialog

    init {
        binding = DownloadingDialogBinding.inflate(activity.layoutInflater)
        val builder = MaterialAlertDialogBuilder(activity)
        builder.setView(binding.root)
        dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
    }

    fun show() {
        dialog.show()
    }

    fun dismiss(){
        dialog.dismiss()
    }

    fun setProgress(curr: Long, total: Long) {
        val percentage = (curr * 100) / total
        binding.txtPercentage.text = buildString {
            append(percentage)
            append("%")
        }

        binding.progressBar.progress = percentage.toInt()

        binding.txtFileSize.text = buildString {
            append(convertToMB(curr))
            append(" / ")
            append(convertToMB(total))
            append(" MB")
        }
    }

    private fun convertToMB(input: Long): Double {
        val res = input / (1024 * 1024L)
        return (res * 10.0).roundToLong() / 10.0
    }
}