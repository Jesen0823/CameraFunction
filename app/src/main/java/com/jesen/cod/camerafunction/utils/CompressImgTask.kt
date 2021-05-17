package com.jesen.cod.camerafunction.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import android.widget.ImageView
import com.jesen.cod.camerafunction.utils.BitmapUtil.decodeBitmap
import java.lang.ref.WeakReference

class CompressImgTask(imageView: ImageView, private val s: Int) : AsyncTask<String, Int, Bitmap>() {
    private val ivReference: WeakReference<ImageView> = WeakReference(imageView)
    private var temp = 0L

    override fun doInBackground(vararg strings: String?): Bitmap? {
        temp = System.currentTimeMillis()
        return BitmapFactory.decodeFile(strings[0])
    }

    override fun onPostExecute(bitmap: Bitmap) {
        if (bitmap != null) {
            ivReference.get()?.let {
                val compressBmp = decodeBitmap(bitmap,
                        (bitmap.width) / s,
                        (bitmap.height) / s)
                it.setImageBitmap(compressBmp)
                Outil.log("CompressImgTask, onPostExecute ,origin size: ${bitmap.width} x ${bitmap.height}")
                Outil.log("CompressImgTask, onPostExecute ,after compress size:" +
                        " ${compressBmp.width} x ${compressBmp.height}")
                Outil.log("CompressImgTask, onPostExecute ,used time: ${System.currentTimeMillis() - temp}")
            }
        }
        super.onPostExecute(bitmap)
    }
}