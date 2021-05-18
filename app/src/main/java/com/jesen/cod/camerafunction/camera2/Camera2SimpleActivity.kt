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
import android.media.ImageReader
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.jesen.cod.camerafunction.R
import com.jesen.cod.camerafunction.utils.Outil
import kotlinx.android.synthetic.main.activity_camera2_simple.*

class Camera2SimpleActivity : AppCompatActivity() {

    private lateinit var mCameraDevice: CameraDevice
    private lateinit var textureViewSurface: Surface
    lateinit var imageReaderSurface: Surface
    lateinit var captureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera2_simple)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                textureViewSurface = Surface(textureView.surfaceTexture)
                openCamera()
            }

        }

        button.setOnClickListener {
            takePic()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun takePic() {
        val requestBuilder = mCameraDevice.createCaptureRequest(
                CameraDevice.TEMPLATE_STILL_CAPTURE
        )
        requestBuilder.addTarget(imageReaderSurface)
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
            imageView.setImageBitmap(bitmap)

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

                captureSession.setRepeatingRequest(requestBuilder.build(), null, null)
            }

        }

        val cameraStateCallback = object : CameraDevice.StateCallback() {
            override fun onOpened(cameraDevice: CameraDevice) {
                mCameraDevice = cameraDevice
                mCameraDevice.createCaptureSession(
                        listOf(imageReaderSurface, textureViewSurface),
                        sessionStateCallback,
                        null
                )
            }

            override fun onDisconnected(cameraDevice: CameraDevice) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onError(cameraDevice: CameraDevice, error: Int) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
