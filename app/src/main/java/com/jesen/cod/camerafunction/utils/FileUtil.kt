package com.jesen.cod.camerafunction.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileUtil {

    @JvmStatic
    fun getStoragePath(context: Context, type: String?): String {
        var baseDir: String
        val state = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED == state) {
            var baseDirFile = context.getExternalFilesDir(type)
            baseDir = if (baseDirFile != null) {
                baseDirFile.absolutePath
            } else {
                context.filesDir.absolutePath
            }
        } else {
            baseDir = context.filesDir.absolutePath;
        }
        return baseDir
    }

    @JvmStatic
    fun createImageFile(isCut: Boolean, context: Context): File? {
        return try {
            var rootFile = File(getStoragePath(context, Environment.DIRECTORY_PICTURES)
                    + File.separator + "capture")
            if (!rootFile.exists())
                rootFile.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val fileName = if (isCut) "IMG_CUT_$timeStamp.jpg" else "IMG_$timeStamp.jpg"
            File(rootFile.absolutePath + File.separator + fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createCameraFile(folderName: String = "camera", context: Context): File? {
        return try {
            val rootFile = File(getStoragePath(context, Environment.DIRECTORY_DCIM)
                    + File.separator + folderName)
            if (!rootFile.exists())
                rootFile.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val fileName = "IMG_$timeStamp.jpg"
            File(rootFile.absolutePath + File.separator + fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createVideoFile(context: Context): File? {
        return try {
            var rootFile = File(getStoragePath(context, Environment.DIRECTORY_MOVIES)
                    + File.separator + "video")
            if (!rootFile.exists())
                rootFile.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val fileName = "VIDEO_$timeStamp.mp4"
            File(rootFile.absolutePath + File.separator + fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}