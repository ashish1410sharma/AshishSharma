package com.ashishsharma.appupdate

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream

class AutoUpdate {
    companion object {
        val STARTING_DOWNLOAD = 111
        private var progress: Int = 0

        fun updateApp(
            activity: Activity,
            apkDownloadURL: String,
            installRequestLauncher: ActivityResultLauncher<Intent>,
            requestPermissionLauncher: ActivityResultLauncher<String>,
            directoryName:String,
            updateProgressListener: ResponseListener
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!activity.packageManager.canRequestPackageInstalls()) {
                    installRequestLauncher.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        setData(Uri.parse(String.format("package:%s", activity.packageName)))
                    })
                    return
                }
            }
            if (Build.VERSION.SDK_INT >= 33) {
                if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    return
                }
            } else if (Build.VERSION.SDK_INT >= 23) {
                if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    return
                } else if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    return
                }
            }
            downloadFile(activity, apkDownloadURL, directoryName, updateProgressListener)
        }

        private fun downloadFile(activity: Activity, apkLink: String, directoryName:String, listener: ResponseListener) {
            listener.onResponse(STARTING_DOWNLOAD)
            GlobalScope.launch(Dispatchers.IO) {
                var retrofit = Retrofit.Builder()
                    .baseUrl(apkLink.substring(0, apkLink.lastIndexOf("/") + 1))
                    .build()
                var retrofitInterface = retrofit.create(MyApi::class.java)
                var fileName = "ApkFile"
                var urlConnection = retrofitInterface
                    .downloadFile(apkLink
                        .substring(apkLink.lastIndexOf("/") + 1).also {
                            fileName = it
                        }).execute().body()
                var fileDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    directoryName
                )
                if (!fileDir.exists())
                    fileDir.mkdir()
                else {
                    fileDir.delete()
                    fileDir.mkdir()
                }
                var apkfile = File(fileDir, fileName)
                if (!apkfile.createNewFile())
                    apkfile.delete()
                var inputStream = BufferedInputStream(urlConnection?.byteStream(), 1024 * 8)
                var size = urlConnection?.contentLength()
                var outputStream = FileOutputStream(apkfile)
                var downloadSize = 0
                var buffer = ByteArray(2024)
                var bufferLength = inputStream.read(buffer)
                while (bufferLength > 0) {
                    outputStream.write(buffer, 0, bufferLength)
                    downloadSize += bufferLength
                    bufferLength = inputStream.read(buffer)
                    progress = ((downloadSize.toFloat() / size?.toFloat()!!) * 100f).toInt()
                    listener.onResponse(progress)
                }
                outputStream.close()
                installFile(activity, apkfile.absolutePath)
            }
        }

        private fun installFile(activity: Activity, path: String) {
            val file = File(path)
            if (file.exists()) {
                try {
                    activity.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(
                            uriFromFile(activity, file),
                            "application/vnd.android.package-archive"
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })
                } catch (e: Exception) {
                    print("${e.message}")
                }
            }
        }

        private fun uriFromFile(activity: Activity, file: File): Uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                FileProvider.getUriForFile(
                    activity.applicationContext,
                    getAppId(activity)
                    /*BuildConfig.APPLICATION_ID*/ + ".provider", file
                )
            else Uri.fromFile(file)

        fun getAppId(activity:Activity):String =
            if (Build.VERSION.SDK_INT >= 33)
                activity.packageManager.getApplicationInfo(activity.packageName,
                (  PackageManager.ApplicationInfoFlags.of(0)))
                .metaData.getString("app.update", "No Application Id Found")
            else
                activity.packageManager.getApplicationInfo(activity.packageName,
                (  PackageManager.GET_META_DATA))
                .metaData.getString("app.update", "No Application Id Found")
    }
}