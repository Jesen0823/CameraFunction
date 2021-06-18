package com.jesen.cod.camerafunction.camera2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.jesen.cod.camerafunction.R
import com.jesen.cod.camerafunction.databinding.ActivityCamera2SimpleBinding
import com.jesen.cod.camerafunction.utils.Outil

class Camera2SimpleActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityCamera2SimpleBinding

    private lateinit var mCameraDevice: CameraDevice
    private lateinit var textureViewSurface: Surface
    lateinit var imageReaderSurface: Surface
    lateinit var captureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityCamera2SimpleBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_camera2_simple)

        mBinding.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                textureViewSurface = Surface(mBinding.textureView.surfaceTexture)
                openCamera()
            }

        }

        mBinding.button.setOnClickListener {
            takePic()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun takePic() {
        val requestBuilder = mCameraDevice.createCaptureRequest(
                CameraDevice.TEMPLATE_STILL_CAPTURE
        )
        requestBuilder.addTarget(imageReaderSurface)
        requestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) // 自动对焦
        requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)     // 闪光灯
        requestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90)
        captureSession.capture(requestBuilder.build(), null, null)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun openCamera() {
        imageReader = ImageReader.newInstance(
                200,
                200,
                ImageFormat.JPEG,
                2
        )
        imageReader.setOnImageAvailableListener(ImageReader.OnImageAvailableListener {
            Outil.log("get a photo.")
            val image = it.acquireLatestImage()
            val buffer = image.planes[0].buffer
            val bytesLen = buffer.remaining()
            var bytes = ByteArray(bytesLen)
            buffer.get(bytes)

            image.close()

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytesLen)
            mBinding.imageView.setImageBitmap(bitmap)

        }, null)

        imageReaderSurface = imageReader.surface


        val sessionStateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigureFailed(session: CameraCaptureSession) {
            }

            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                val requestBuilder = mCameraDevice.createCaptureRequest(
                        CameraDevice.TEMPLATE_PREVIEW)
                requestBuilder.addTarget(textureViewSurface)
                // 闪光灯
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                // 自动对焦
                requestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                captureSession.setRepeatingRequest(requestBuilder.build(), null, null)
            }

        }

        val cameraStateCallback = object : CameraDevice.StateCallback() {
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onOpened(cameraDevice: CameraDevice) {
                mCameraDevice = cameraDevice
                mCameraDevice.createCaptureSession(
                        listOf(imageReaderSurface, textureViewSurface),
                        sessionStateCallback,
                        null
                )

                // new Api:
                /*val sessionConfiguration = SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
                        Collections.singletonList( OutputConfiguration(textureViewSurface)),
                        mainExecutor,
                        sessionStateCallback)
                mCameraDevice.createCaptureSession(sessionConfiguration)*/
            }

            override fun onDisconnected(cameraDevice: CameraDevice) {

            }

            override fun onError(cameraDevice: CameraDevice, error: Int) {

            }
        }


        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
            }
            return
        }
        cameraManager.openCamera(cameraManager.cameraIdList[0], cameraStateCallback, null)
    }
}
