package com.jesen.cod.camerafunction.camera2

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Build.VERSION_CODES.P
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.jesen.cod.camerafunction.utils.BitmapUtil
import com.jesen.cod.camerafunction.utils.Outil
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

const val PREVIEW_WIDTH = 720
const val PREVIEW_HEIGHT = 1280
const val SAVE_WIDTH = 720
const val SAVE_HEIGHT = 1280

class Camera2Helper(val activity: Activity, private val textureView: TextureView) {

    private lateinit var mCameraManager: CameraManager
    private var mImgReader: ImageReader? = null
    private var mCameraDevice: CameraDevice? = null
    private var mCameraCaptureSession: CameraCaptureSession? = null
    private lateinit var mCameraCharacteristics: CameraCharacteristics
    private lateinit var mSurface:Surface

    private var mCameraId = "0"
    private var mCameraSensorOrientation = 0 // 摄像头方向
    private var mCameraFacing = CameraCharacteristics.LENS_FACING_BACK  // 后置摄像头
    private val mDeviceOrientation = activity.windowManager.defaultDisplay.rotation

    private var canTakePic = true
    private var canChangeCamera = false

    private var mCameraHandler: Handler
    private val mHandlerThread = HandlerThread("CameraThread")

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private var mPreviewSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT)
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private var mSaveSize = Size(SAVE_WIDTH, SAVE_HEIGHT)

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {

        val image = it.acquireNextImage()
        val byteBuffer = image.planes[0].buffer
        val byteArray = ByteArray(byteBuffer.remaining())
        byteBuffer.get(byteArray)
        it.close()
        BitmapUtil.savePic(activity, byteArray, mCameraSensorOrientation == 270,
                { savedPath, _ ->
                    activity.runOnUiThread {
                        Toast.makeText(activity, "Image saved success", Toast.LENGTH_SHORT).show()
                        Outil.log("saved image path: $savedPath")
                    }
                }, { msg ->
            activity.runOnUiThread {
                Toast.makeText(activity, "Image saved failed!", Toast.LENGTH_SHORT).show()
                Outil.log("saved image path error: $msg")
            }
        })
    }

    private val mCaptureCallBack = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession,
                                        request: CaptureRequest, result: TotalCaptureResult) {
            super.onCaptureCompleted(session, request, result)
            canChangeCamera = true
            canTakePic = true
            Outil.log("mCaptureCallBack, onCaptureCompleted")
        }

        override fun onCaptureFailed(session: CameraCaptureSession,
                                     request: CaptureRequest, failure: CaptureFailure) {
            super.onCaptureFailed(session, request, failure)
            Outil.log("mCaptureCallBack, onCaptureFailed")
            Toast.makeText(activity, "start preview failed.", Toast.LENGTH_SHORT).show()
        }
    }

    init {
        mHandlerThread.start()
        mCameraHandler = Handler(mHandlerThread.looper)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                releaseCamera()
                return true
            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                initCameraInfo()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun initCameraInfo() {
        mCameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIdList = mCameraManager.cameraIdList
        if (cameraIdList.isEmpty()) {
            Toast.makeText(activity, "not find valid Camera", Toast.LENGTH_SHORT).show()
            return
        }

        cameraIdList.forEach {
            val cameraCharacteristics = mCameraManager.getCameraCharacteristics(it)
            val facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == mCameraFacing) {
                mCameraId = it
                mCameraCharacteristics = cameraCharacteristics
            }
            Outil.log("in this device has camera， id : $it")

            val supportLevel = mCameraCharacteristics
                    .get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            Outil.log("supportLevel : $supportLevel")

            // 摄像头方向
            mCameraSensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

            val configurationMap = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val saveImgSize = configurationMap?.getOutputSizes(ImageFormat.JPEG)
            val previewImgSize = configurationMap?.getOutputSizes(SurfaceTexture::class.java)

            val isNeedChange = isChangeWidthAndHeight(mDeviceOrientation, mCameraSensorOrientation)

            if (saveImgSize != null) {
                mSaveSize = getBestSize(
                        if (isNeedChange) mSaveSize.height else mSaveSize.width,
                        if (isNeedChange) mSaveSize.width else mSaveSize.height,
                        if (isNeedChange) mSaveSize.height else mSaveSize.width,
                        if (isNeedChange) mSaveSize.width else mSaveSize.height,
                        saveImgSize.toList())
            }

            if (previewImgSize != null) {
                mPreviewSize = getBestSize(
                        if (isNeedChange) mPreviewSize.height else mPreviewSize.width,
                        if (isNeedChange) mPreviewSize.width else mPreviewSize.height,
                        if (isNeedChange) textureView.height else textureView.width,
                        if (isNeedChange) textureView.width else textureView.height,
                        previewImgSize.toList())
            }

            textureView.surfaceTexture?.setDefaultBufferSize(mPreviewSize.width, mPreviewSize.height)
            Outil.log("best preview size ：${mPreviewSize.width} * ${mPreviewSize.height}, " +
                    "scale:  ${mPreviewSize.width.toFloat() / mPreviewSize.height}")
            Outil.log("best size to save image ：${mSaveSize.width} * ${mSaveSize.height}, " +
                    "scale:  ${mSaveSize.width.toFloat() / mSaveSize.height}")

            mImgReader = ImageReader.newInstance(
                    mSaveSize.width,
                    mSaveSize.height,
                    ImageFormat.JPEG,
                    1)
            mImgReader?.setOnImageAvailableListener(onImageAvailableListener, mCameraHandler)
            mCameraId = cameraIdList[0]  //testtest
            mSurface = Surface(textureView.surfaceTexture)


            openCamera()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(activity,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Outil.log("denied permission of camera.")
            return
        }

        mCameraManager.openCamera(mCameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(cameraDevice: CameraDevice) {
                Outil.log("openCamera success.")
                mCameraDevice = cameraDevice
                createCaptureSession(cameraDevice)
            }

            override fun onDisconnected(cameraDevice: CameraDevice) {
                Outil.log("openCamera onDisconnected.")
                cameraDevice.close()
                mCameraDevice = null
            }

            override fun onError(cameraDevice: CameraDevice, error: Int) {
                Outil.log("openCamera error, errorCode: $error.")
                cameraDevice.close()
                mCameraDevice = null
            }
        }, mCameraHandler)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createCaptureSession(cameraDevice: CameraDevice) {
        val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(mSurface)
        // 闪光灯
        /*captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)*/
        // 自动对焦
        /*captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)*/

        if (Build.VERSION.SDK_INT >= P) {
            cameraDevice.createCaptureSession(
                    SessionConfiguration(
                            SessionConfiguration.SESSION_REGULAR,
                            Collections.singletonList(OutputConfiguration(mSurface)),
                            activity.mainExecutor,
                            object : CameraCaptureSession.StateCallback() {

                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    Outil.log("cameraDevice, open preview session failed.")
                                }

                                override fun onConfigured(session: CameraCaptureSession) {
                                    mCameraCaptureSession = session
                                    session.setRepeatingRequest(
                                            captureRequestBuilder.build(),
                                            mCaptureCallBack,
                                            mCameraHandler
                                    )
                                }
                            }
                    )
            )
        }

        /*
        cameraDevice.createCaptureSession(arrayListOf(surface, mImgReader?.surface),
                     object : CameraCaptureSession.StateCallback() {
                         override fun onConfigureFailed(session: CameraCaptureSession) {
                             Outil.log("cameraDevice, open preview session failed.")
                         }

                         override fun onConfigured(session: CameraCaptureSession) {
                             mCameraCaptureSession = session
                             session.setRepeatingRequest(captureRequestBuilder.build(), mCaptureCallBack, mCameraHandler)
                         }

                     }, mCameraHandler)
         */
    }

    /**
     * 拍照
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun takePic() {
        if (mCameraDevice == null || !textureView.isAvailable || !canTakePic) return

        mCameraDevice?.apply {

            val captureRequestBuilder
                    = createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            mImgReader?.surface?.let { captureRequestBuilder.addTarget(it) }

            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) // 自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)     // 闪光灯
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mCameraSensorOrientation)
            //根据摄像头方向对保存的照片进行旋转，使其为"自然方向"
            mCameraCaptureSession?.capture(captureRequestBuilder.build(), null, mCameraHandler)
                    ?: Toast.makeText(activity, "takePic error", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 切换摄像头
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun changeCamera() {
        if (mCameraDevice == null || !canChangeCamera || !textureView.isAvailable) return

        mCameraFacing = if (mCameraFacing == CameraCharacteristics.LENS_FACING_FRONT)
            CameraCharacteristics.LENS_FACING_BACK
        else
            CameraCharacteristics.LENS_FACING_FRONT

        mPreviewSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT) //重置预览大小
        releaseCamera()
        initCameraInfo()
    }

    fun releaseCamera() {
        mCameraCaptureSession?.close()
        mCameraCaptureSession = null

        mCameraDevice?.close()
        mCameraDevice = null

        mImgReader?.close()
        mImgReader = null

        canChangeCamera = false
    }


    fun releaseThread() {
        mHandlerThread.quitSafely()
    }

    /**
     * 根据提供的屏幕方向 [displayOrientation]
     * 和相机方向 [sensorOrientation] 返回是否需要交换宽高
     */
    private fun isChangeWidthAndHeight(displayOrientation: Int, sensorOrientation: Int): Boolean {
        var isChange = false
        when (displayOrientation) {
            Surface.ROTATION_0, Surface.ROTATION_180 ->
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    isChange = true
                }
            Surface.ROTATION_90, Surface.ROTATION_270 ->
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    isChange = true
                }
            else -> Outil.log("Screen rotation is invalid: $displayOrientation")
        }

        Outil.log("screen orientation:  $displayOrientation")
        Outil.log("camera orientation:  $sensorOrientation")
        return isChange
    }

    /**
     *
     * 根据提供的参数值返回与指定宽高相等或最接近的尺寸
     *
     * @param targetWidth   目标宽度
     * @param targetHeight  目标高度
     * @param maxWidth      最大宽度(即TextureView的宽度)
     * @param maxHeight     最大高度(即TextureView的高度)
     * @param sizeList      支持的Size列表
     *
     * @return  返回与指定宽高相等或最接近的尺寸
     *
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getBestSize(targetWidth: Int, targetHeight: Int, maxWidth: Int, maxHeight: Int,
                            sizeList: List<Size>): Size {
        val bigEnough = ArrayList<Size>()     //比指定宽高大的Size列表
        val notBigEnough = ArrayList<Size>()  //比指定宽高小的Size列表

        for (size in sizeList) {

            //宽<=最大宽度  &&  高<=最大高度  &&  宽高比 == 目标值宽高比
            if (size.width <= maxWidth && size.height <= maxHeight
                    && size.width == size.height * targetWidth / targetHeight) {

                if (size.width >= targetWidth && size.height >= targetHeight)
                    bigEnough.add(size)
                else
                    notBigEnough.add(size)
            }
            Outil.log("size system supported: ${size.width} * ${size.height} ," +
                    "  scale ：${size.width.toFloat() / size.height}")
        }

        Outil.log("max size is ：$maxWidth * $maxHeight, " +
                "scale ：${targetWidth.toFloat() / targetHeight}")
        Outil.log("dst size is ：$targetWidth * $targetHeight, " +
                "scale ：${targetWidth.toFloat() / targetHeight}")

        //选择bigEnough中最小的值  或 notBigEnough中最大的值
        return when {
            bigEnough.size > 0 -> Collections.min(bigEnough, CompareSizesByArea())
            notBigEnough.size > 0 -> Collections.max(notBigEnough, CompareSizesByArea())
            else -> sizeList[0]
        }
    }

    private class CompareSizesByArea : Comparator<Size> {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun compare(size1: Size, size2: Size): Int {
            return java.lang.Long.signum(size1.width.toLong() * size1.height
                    - size2.width.toLong() * size2.height)
        }
    }
}