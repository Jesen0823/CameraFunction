package com.jesen.cod.camerafunction

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.jesen.cod.camerafunction.activity.SystemCameraActivity
import com.jesen.cod.camerafunction.camera.CameraActivity
import com.jesen.cod.camerafunction.camera2.Camera2Activity
import com.jesen.cod.camerafunction.camera2.Camera2SimpleActivity
import com.jesen.cod.camerafunction.utils.Outil
import com.jesen.cod.camerafunction.utils.PermissionUtil
import com.jesen.cod.camerafunction.utils.PermissionUtil.PERMISSION_REQUEST_CODE
import com.jesen.cod.camerafunction.utils.PermissionUtil.PERMISSION_SETTING_CODE
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val permissionList = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setClick()

        PermissionUtil.checkPermissions(this,permissionList, Runnable {  })
    }

    private fun setClick(){
        btCapture.setOnClickListener {
            PermissionUtil.checkPermissions(this, permissionList, Runnable {
                startActivity(Intent(this, SystemCameraActivity::class.java))
            })
        }

        cameraBtn.setOnClickListener {
            PermissionUtil.checkPermissions(this, permissionList, Runnable {
                val intent = Intent(this, CameraActivity::class.java)
                intent.putExtra("type", 0)
                startActivity(intent)
            })
        }

        cameraVideoBtn.setOnClickListener {
            PermissionUtil.checkPermissions(this, permissionList, Runnable {
                val intent = Intent(this, CameraActivity::class.java)
                intent.putExtra("type", 1)
                startActivity(intent)
            })
        }

        camera2Btn.setOnClickListener {
            PermissionUtil.checkPermissions(this, permissionList, Runnable {
                val intent = Intent(this, Camera2Activity::class.java)
                startActivity(intent)
            })
        }

        camera2SimpleBtn.setOnClickListener {
            PermissionUtil.checkPermissions(this, permissionList, Runnable {
                val intent = Intent(this, Camera2SimpleActivity::class.java)
                startActivity(intent)
            })
        }



    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Outil.log("MainActivity, onRequestPermissionsResult")
        when(requestCode){
            PERMISSION_REQUEST_CODE ->{
                var allGranted = true

                grantResults.forEach {
                    if (it != PackageManager.PERMISSION_GRANTED){
                        allGranted = false
                    }
                }

                if (allGranted){
                    Outil.log("MainActivity, onRequestPermissionsResult, allGranted Permissions.")
                }else{
                    Outil.log("MainActivity, onRequestPermissionsResult, Permissions is denied.")
                    PermissionUtil.showPermissionSettingDialog(this)
                }
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            PERMISSION_SETTING_CODE ->{
                Outil.log("MainActivity, onActivityResult, return from setting page, request again.")
                PermissionUtil.checkPermissions(this, permissionList, Runnable {
                    startActivity(Intent(this, SystemCameraActivity::class.java))
                })
            }
        }
    }
}













