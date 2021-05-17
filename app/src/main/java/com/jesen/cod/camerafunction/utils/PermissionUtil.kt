package com.jesen.cod.camerafunction.utils

import android.app.AlertDialog
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtil {
    const val TAG = "PermissionUtils"
    const val PERMISSION_REQUEST_CODE = 100
    const val PERMISSION_SETTING_CODE = 101

    lateinit var permissionListDialog: AlertDialog
    lateinit var permissionSettingDialog: AlertDialog

    fun checkPermissions(activity: AppCompatActivity, permissionList: Array<String>, callback: Runnable) {
        var allGranted = true
        permissionList.forEach {
            val result = ContextCompat.checkSelfPermission(activity, it)
            Log.d(TAG, "checkPermissions, result: $result")

            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false
            }
        }
        if (allGranted) {
            callback.run()
        } else {
            startRequestPermission(activity, permissionList)
        }
    }

    /**
     * 如果拒绝过某权限，展示提示框，否则直接请求权限
     * */
    private fun startRequestPermission(activity: AppCompatActivity, permissionList: Array<String>) {
        permissionList.forEach {

            /*
            拒绝过该权限             {return true}
            拒绝过且“不再提示”       {return false}
            设备策略禁止应用获取权限  {return false}
            */
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, it)) {
                // 异步非阻塞展示dialog
                showPermissionListDialog(activity, permissionList)
            } else {
                Log.d(TAG, "requestPermission.")
                requestPermission(activity, permissionList)
            }
        }
    }

    private fun requestPermission(activity: AppCompatActivity, permissionList: Array<String>) {
        ActivityCompat.requestPermissions(activity, permissionList, PERMISSION_REQUEST_CODE)
    }

    private fun showPermissionListDialog(activity: AppCompatActivity, permissionList: Array<String>) {
        permissionListDialog = AlertDialog.Builder(activity)
                .setTitle("权限申请")
                .setMessage("您拒绝了权限将无法继续使用该功能，是否继续？")
                .setPositiveButton("确定") { dialog, _ ->
                    requestPermission(activity, permissionList)
                    dialog.cancel()
                }
                .setNegativeButton("取消") { dialog, _ ->
                    dialog.cancel()
                }
                .create()

        permissionListDialog.let {
            if (!it.isShowing) it.show()
        }
    }

    fun showPermissionSettingDialog(activity: AppCompatActivity) {
        permissionSettingDialog = AlertDialog.Builder(activity)
                .setTitle("权限设置")
                .setMessage("您刚才拒绝了相关的权限，请到应用设置页面更改应用的权限")
                .setPositiveButton("确定") { dialog, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    val uri = Uri.fromParts("package", activity.packageName, null)
                    intent.data = uri
                    activity.startActivityForResult(intent, PERMISSION_SETTING_CODE)
                    dialog.cancel()
                }
                .setNegativeButton("取消") { dialog, _ ->
                    dialog.cancel()
                }
                .create()

        permissionSettingDialog?.let {
            if (!it.isShowing) {
                it.show()
            }
        }
    }
}








