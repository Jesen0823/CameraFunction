package com.jesen.cod.camerafunction.camera

import android.app.Activity
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.hardware.Camera
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.jesen.cod.camerafunction.utils.Outil

class CameraHelper(activity: Activity, surfaceView: SurfaceView) : Camera.PreviewCallback {

    private var mCamera: Camera? = null
    private lateinit var mParameters: Camera.Parameters
    private var mSurfaceView: SurfaceView = surfaceView
    var mSurfaceHolder: SurfaceHolder
    private var mActivity: Activity = activity
    private lateinit var mCallback: CameraCallBack

    var mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK  //摄像头方向
    var mDisplayOrientation: Int = 0    //预览旋转的角度

    private var picWidth = 2160
    private var picHeight = 3840

    init {
        mSurfaceHolder = mSurfaceView.holder
        init()
    }

    override fun onPreviewFrame(p0: ByteArray?, p1: Camera?) {
        if (p0 != null) {
            mCallback.onPreviewFrame(p0)
        }
    }



    private fun init(){
        mSurfaceHolder.addCallback(object :SurfaceHolder.Callback{
            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                releaseCamera()
            }

            override fun surfaceCreated(p0: SurfaceHolder) {
                if (mCamera == null){
                    openCamera(mCameraFacing)
                }
                startPreview()
            }
        })
    }

    // 打开相机
    private fun openCamera(cameraFacing: Int = Camera.CameraInfo.CAMERA_FACING_BACK): Boolean {
        val supportCameraFacing = isSupportCameraFacing(cameraFacing)
        if (supportCameraFacing) {
            try {
                mCamera = Camera.open(cameraFacing)
                mCamera?.let {
                    initParameters(it)
                    it.setPreviewCallback(this)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Outil.log("Open Camera failed.")
                return false
            }
        }
        return supportCameraFacing
    }

    //配置相机参数
    private fun initParameters(camera: Camera) {
        try {
            mParameters = camera.parameters
            mParameters.previewFormat = ImageFormat.NV21

            //获取与指定宽高相等或最接近的尺寸
            //设置预览尺寸
            val bestPreviewSize = getBestSize(mSurfaceView.width, mSurfaceView.height, mParameters.supportedPreviewSizes)
            bestPreviewSize?.let {
                mParameters.setPreviewSize(it.width, it.height)
            }
            //设置保存图片尺寸
            val bestPicSize = getBestSize(picWidth, picHeight, mParameters.supportedPictureSizes)
            bestPicSize?.let {
                mParameters.setPictureSize(it.width, it.height)
            }
            //对焦模式
            if (isSupportFocus(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
                mParameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE

            camera.parameters = mParameters
        } catch (e: Exception) {
            e.printStackTrace()
            Outil.log("init Camera failed.")
        }
    }

    // 开启预览
    private fun startPreview(){
        mCamera?.let {
            it.setPreviewDisplay(mSurfaceHolder)
            setCameraDisplayOrientation(mActivity)
            it.startPreview()
            startFaceDetect()
        }
    }

    private fun startFaceDetect(){
        mCamera?.let {
            it.startFaceDetection()
            it.setFaceDetectionListener { faces, _ ->
                mCallback?.onFaceDetect(transForm(faces))
                Outil.log("Detect there are  ${faces.size} people faces.")
            }
        }
    }

    //释放相机
    fun releaseCamera() {
        /*if (mCamera != null) {
            // mCamera?.stopFaceDetection()
            mCamera?.stopPreview()
            mCamera?.setPreviewCallback(null)
            mCamera?.release()
        }*/
    }

    //切换摄像头
    fun exchangeCamera() {
        releaseCamera()
        mCameraFacing = if (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_BACK)
            Camera.CameraInfo.CAMERA_FACING_FRONT
        else
            Camera.CameraInfo.CAMERA_FACING_BACK

        openCamera(mCameraFacing)
        startPreview()
    }




    // 判断是否支持某相机
    private fun isSupportCameraFacing(facing: Int): Boolean {
        val info = Camera.CameraInfo()
        for (i in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(i, info)
            if (info.facing == facing) return true
        }
        return false
    }

    //判断是否支持某一对焦模式
    private fun isSupportFocus(focusMode: String): Boolean {
        var autoFocus = false
        val listFocusMode = mParameters.supportedFocusModes
        for (mode in listFocusMode) {
            if (mode == focusMode)
                autoFocus = true
            Outil.log("The focusMode that camera supported is ： $mode")
        }
        return autoFocus
    }

    //获取与指定宽高相等或最接近的尺寸
    private fun getBestSize(targetWidth: Int, targetHeight: Int, sizeList: List<Camera.Size>): Camera.Size? {
        var bestSize: Camera.Size? = null
        val targetRatio = (targetHeight.toDouble() / targetWidth)  //目标大小的宽高比
        var minDiff = targetRatio

        for (size in sizeList) {
            val supportedRatio = (size.width.toDouble() / size.height)
            Outil.log("Size of system supported : ${size.width} x ${size.height} ,    比例$supportedRatio")
        }

        for (size in sizeList) {
            if (size.width == targetHeight && size.height == targetWidth) {
                bestSize = size
                break
            }

            val supportedRatio = (size.width.toDouble() / size.height)
            if (Math.abs(supportedRatio - targetRatio) < minDiff) {
                minDiff = Math.abs(supportedRatio - targetRatio)
                bestSize = size
            }
        }
        Outil.log("dst size ：$targetWidth x $targetHeight ，ratio:  $targetRatio")
        Outil.log("best size ：${bestSize?.height} x ${bestSize?.width}")
        return bestSize
    }

    //将相机中用于表示人脸矩形的坐标转换成UI页面的坐标
    private fun transForm(faces: Array<Camera.Face>): ArrayList<RectF> {
        val matrix = Matrix()
        // Need mirror for front camera.
        val mirror = (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        matrix.setScale(if (mirror) -1f else 1f, 1f)
        // This is the value for android.hardware.Camera.setDisplayOrientation.
        matrix.postRotate(mDisplayOrientation.toFloat())
        // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
        // UI coordinates range from (0, 0) to (width, height).
        matrix.postScale(mSurfaceView.width / 2000f, mSurfaceView.height / 2000f)
        matrix.postTranslate(mSurfaceView.width / 2f, mSurfaceView.height / 2f)

        val rectList = ArrayList<RectF>()
        for (face in faces) {
            val srcRect = RectF(face.rect)
            val dstRect = RectF(0f, 0f, 0f, 0f)
            matrix.mapRect(dstRect, srcRect)
            rectList.add(dstRect)
        }
        return rectList
    }

    //设置预览旋转的角度
    private fun setCameraDisplayOrientation(activity: Activity) {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(mCameraFacing, info)
        val rotation = activity.windowManager.defaultDisplay.rotation

        var screenDegree = 0
        when (rotation) {
            Surface.ROTATION_0 -> screenDegree = 0
            Surface.ROTATION_90 -> screenDegree = 90
            Surface.ROTATION_180 -> screenDegree = 180
            Surface.ROTATION_270 -> screenDegree = 270
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mDisplayOrientation = (info.orientation + screenDegree) % 360
            mDisplayOrientation = (360 - mDisplayOrientation) % 360          // compensate the mirror
        } else {
            mDisplayOrientation = (info.orientation - screenDegree + 360) % 360
        }
        mCamera?.setDisplayOrientation(mDisplayOrientation)

        Outil.log("orientation of screen : $rotation")
        Outil.log("setDisplayOrientation(result) : $mDisplayOrientation")
    }

    fun getCamera():Camera? = mCamera

    fun addCallBack(callBack: CameraCallBack){
        this.mCallback = callBack
    }

    fun takePic() {
        mCamera?.let {
            it.takePicture({}, null, { data, _ ->
                it.startPreview()
                mCallback?.onTakePic(data)
            })
        }
    }

    interface CameraCallBack {
        fun onPreviewFrame(data: ByteArray)
        fun onTakePic(data: ByteArray)
        fun onFaceDetect(faces: ArrayList<RectF>)
    }
}
