package com.jesen.cod.camerafunction.camera2

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.jesen.cod.camerafunction.R
import kotlinx.android.synthetic.main.activity_camera2.*

class Camera2Activity : AppCompatActivity() {

    private lateinit var mCamera2Helper: Camera2Helper

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera2)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        mCamera2Helper = Camera2Helper(this, textureView)

        captureBtn.setOnClickListener { mCamera2Helper.takePic() }
        changeCamera.setOnClickListener { mCamera2Helper.changeCamera() }

    }


    override fun onDestroy() {
        super.onDestroy()
        mCamera2Helper.releaseCamera()
        mCamera2Helper.releaseThread()
    }
}
