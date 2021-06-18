package com.jesen.cod.camerafunction.camera

import android.graphics.BitmapFactory
import android.graphics.RectF
import android.hardware.Camera
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.jesen.cod.camerafunction.R
import com.jesen.cod.camerafunction.databinding.ActivityCameraBinding
import com.jesen.cod.camerafunction.utils.BitmapUtil
import com.jesen.cod.camerafunction.utils.FileUtil
import com.jesen.cod.camerafunction.utils.Outil
import okio.buffer
import okio.sink
import java.lang.Exception
import kotlin.concurrent.thread

const val TYPE_RECORD = 1
const val TYPE_CAPTURE = 0

class CameraActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityCameraBinding
    private lateinit var mCameraHelper: CameraHelper
    private var mVideoRecorderHelper: VideoRecorderHelper? = null
    var lock = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_camera)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        mCameraHelper = CameraHelper(this, mBinding.surfaceView)
        mCameraHelper.addCallBack(object : CameraHelper.CameraCallBack {
            override fun onPreviewFrame(data: ByteArray) {
                if (!lock) {
                    mCameraHelper.getCamera()?.let {
                        mVideoRecorderHelper = VideoRecorderHelper(this@CameraActivity,
                                mCameraHelper.getCamera()!!,
                                mCameraHelper.mDisplayOrientation,
                                mCameraHelper.mSurfaceHolder.surface)
                    }
                    lock = true
                }
            }

            override fun onTakePic(data: ByteArray) {
                savePic(data)
                mBinding.captureBtn.isClickable = true
            }

            override fun onFaceDetect(faces: ArrayList<RectF>) {
                mBinding.faceView.setFaces(faces)
            }
        })

        if (intent.getIntExtra("type", 0) == TYPE_RECORD) { //录视频
            mBinding.captureBtn.visibility = View.GONE
            mBinding.startBtn.visibility = View.VISIBLE
        }

        mBinding.captureBtn.setOnClickListener { mCameraHelper.takePic() }
        mBinding.changeCamera.setOnClickListener { mCameraHelper.exchangeCamera() };
        mBinding.startBtn.setOnClickListener {
            mBinding.changeCamera.isClickable = false
            mBinding.startBtn.visibility = View.GONE
            mBinding.stopBtn.visibility = View.VISIBLE
            mVideoRecorderHelper?.startRecord()
        }
        mBinding.stopBtn.setOnClickListener {
            mBinding.startBtn.visibility = View.VISIBLE
            mBinding.stopBtn.visibility = View.GONE
            mBinding.changeCamera.isClickable = true
            mVideoRecorderHelper?.stopRecord()
        }
    }

    private fun savePic(data: ByteArray) {
        thread {
            try {
                val time = System.currentTimeMillis()
                val picFile = FileUtil.createCameraFile("camera", this)
                if (picFile != null && data != null) {
                    val rawBitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                    val dstBitmap = if (mCameraHelper.mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT)
                        BitmapUtil.mirror(BitmapUtil.rotate(rawBitmap, 270f))
                    else
                        BitmapUtil.rotate(rawBitmap, 90f)
                    picFile.sink().buffer().write(BitmapUtil.toByteArray(dstBitmap)).close()
                    runOnUiThread {
                        Toast.makeText(this, "图片已保存", Toast.LENGTH_SHORT).show()
                        Outil.log("Picture saved! use time：${System.currentTimeMillis() - time} \n" +
                                "Path ：  ${picFile.absolutePath}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "图片保存失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        mCameraHelper.releaseCamera()
        mVideoRecorderHelper?.let {
            if (it.isRunning) {
                it.stopRecord()
            }
            it.release()
        }
        super.onDestroy()
    }
}

