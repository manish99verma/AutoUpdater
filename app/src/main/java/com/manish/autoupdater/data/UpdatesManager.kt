package com.manish.autoupdater.data

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.manish.autoupdater.BuildConfig
import com.manish.autoupdater.utils.Event
import com.manish.autoupdater.utils.Resource
import java.io.File
import java.util.UUID

class UpdatesManager {
    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var firebaseStorage: FirebaseStorage = FirebaseStorage.getInstance()
    private var downloadedFile: File? = null

    private var _updateResultMutableLiveData = MutableLiveData<Event<Resource<Action>>>()
    var updateResultLiveData = _updateResultMutableLiveData as LiveData<Event<Resource<Action>>>

    private var _downloadingProgress = MutableLiveData<Event<Resource<DownloadProgress>>>()
    var downloadingProgressLiveData =
        _downloadingProgress as LiveData<Event<Resource<DownloadProgress>>>

    private var updateInfo: UpdateInfo? = null

    fun checkForUpdates() {
        _updateResultMutableLiveData.postValue(Event(Resource.loading(Action.ACTION_CHECKING_FOR_UPDATES)))

        firestore.collection("updates").document("latest_version").get(Source.SERVER)
            .addOnSuccessListener {
                updateInfo = it.toObject(UpdateInfo::class.java)

                // Notify UI about updates
                updateInfo?.let { info ->
                    val newVersionCode = info.versionCode ?: BuildConfig.VERSION_CODE

                    val action =
                        if (newVersionCode > BuildConfig.VERSION_CODE)
                            Action.ACTION_UPDATE_AVAILABLE
                        else
                            Action.ACTION_NO_UPDATES_NEEDED

                    _updateResultMutableLiveData.postValue(Event(Resource.success(action)))
                }
            }.addOnFailureListener {
                _updateResultMutableLiveData.postValue(
                    Event(
                        Resource.error(
                            it.localizedMessage,
                            Action.ACTION_DISMISS
                        )
                    )
                )

                it.printStackTrace()
            }.addOnCanceledListener {
                _updateResultMutableLiveData.postValue(
                    Event(
                        Resource.error(
                            null,
                            Action.ACTION_DISMISS
                        )
                    )
                )

            }
    }

    fun downloadApk() {
        val url = updateInfo?.apkUrl ?: return

        _updateResultMutableLiveData.postValue(Event(Resource.loading(Action.ACTION_SHOW_DOWNLOADING_DIALOG)))

        val storageRef = firebaseStorage.getReferenceFromUrl(url)
        val fileName = "${UUID.randomUUID()}.apk"
        downloadedFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )

        storageRef.getFile(downloadedFile!!).addOnSuccessListener {
            _updateResultMutableLiveData.postValue(Event(Resource.success(Action.ACTION_INSTALL_APK)))
        }.addOnCanceledListener {
            _updateResultMutableLiveData.postValue(
                Event(
                    Resource.error(
                        null,
                        Action.ACTION_DISMISS
                    )
                )
            )

        }.addOnFailureListener {
            _updateResultMutableLiveData.postValue(
                Event(
                    Resource.error(
                        it.localizedMessage,
                        Action.ACTION_DISMISS
                    )
                )
            )

            it.printStackTrace()
        }.addOnProgressListener {
            _downloadingProgress.postValue(
                Event(
                    Resource.loading(
                        DownloadProgress(
                            it.bytesTransferred,
                            it.totalByteCount
                        )
                    )
                )
            )
        }
    }

    fun installApp(context: Context) {
        downloadedFile?.let {
            val contentUri = FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + PROVIDER_PATH,
                it
            )

            val install = Intent(Intent.ACTION_VIEW)
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            install.data = contentUri
            context.startActivity(install)
        }
    }

    companion object {
        private val PROVIDER_PATH = ".provider"
    }

    enum class Action {
        ACTION_CHECKING_FOR_UPDATES,
        ACTION_UPDATE_AVAILABLE,
        ACTION_NO_UPDATES_NEEDED,
        ACTION_DISMISS,
        ACTION_INSTALL_APK,
        ACTION_SHOW_DOWNLOADING_DIALOG
    }

    data class DownloadProgress(val bytesTransferred: Long, val totalByteCount: Long)
}