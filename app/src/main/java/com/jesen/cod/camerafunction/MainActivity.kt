package com.jesen.cod.camerafunction

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.jesen.cod.camerafunction.activity.SystemCameraActivity
import com.jesen.cod.camerafunction.audio.AudioRecordActivity
import com.jesen.cod.camerafunction.camera.CameraActivity
import com.jesen.cod.camerafunction.camera2.Camera2Activity
import com.jesen.cod.camerafunction.camera2.Camera2SimpleActivity
import com.jesen.cod.camerafunction.databinding.ActivityMainBinding
import com.jesen.cod.camerafunction.utils.Outil
import com.jesen.cod.camerafunction.utils.PermissionUtil
import com.jesen.cod.camerafunction.utils.PermissionUtil.PERMISSION_REQUEST_CODE
import com.jesen.cod.camerafunction.utils.PermissionUtil.PERMISSION_SETTING_CODE

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val permissionList = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        //setContentView(R.layout.activity_main)
        setContentView(binding.root)

        setClick()

        PermissionUtil.checkPermissions(this,permissionList, Runnable {  })
    }

    private fun setClick(){
        binding.btCapture.setOnClickListener {
            PermissionUtil.checkPermissions(this, permissionList, Runnable {
                startActivity(Intent(this, SystemCameraActivity::class.java))
            })
        }

        binding.cameraBtn.setOnClickListener {
            PermissionUtil.checkPermissions(this, permissionList, Runnable {
                val intent = Intent(this, CameraActivity::class.java)
                intent.putExtra("type", 0)
                startActivity(intent)
            })
        }

        binding.cameraVideoBtn.setOnClickListener {
            PermissionUtil.checkPermissions(this, permissionList, Runnable {
                val intent = Intent(this, CameraActivity::class.java)
                intent.putExtra("type", 1)
                startActivity(intent)
            })
        }

        binding.camera2Btn.setOnClickListener {
            PermissionUtil.checkPermissions(this, permissionList, Runnable {
                val intent = Intent(this, Camera2Activity::class.java)
                startActivity(intent)
            })
        }

        binding.camera2SimpleBtn.setOnClickListener {
            PermissionUtil.checkPermissions(this, permissionList, Runnable {
                val intent = Intent(this, Camera2SimpleActivity::class.java)
                startActivity(intent)
            })
        }

        binding.audioRecordingBtn.setOnClickListener {
            val intent = Intent(this, AudioRecordActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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