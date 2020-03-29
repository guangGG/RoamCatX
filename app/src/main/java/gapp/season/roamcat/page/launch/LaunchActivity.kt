package gapp.season.roamcat.page.launch

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import gapp.season.roamcat.R
import gapp.season.roamcat.page.BaseActivity
import gapp.season.roamcat.page.main.MainActivity
import gapp.season.util.tips.AlertUtil
import permissions.dispatcher.*

@RuntimePermissions
class LaunchActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
        setUp()
    }

    @Suppress("DEPRECATION")
    override fun customImmersionBar(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.navigationBarColor = resources.getColor(R.color.launch_gradient_end)
        }
        return true
    }

    private fun setUp() {
        //申请权限
        requestExternalStoragePermissionWithPermissionCheck()
    }

    private fun initAppData() {
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 200)
    }

    @NeedsPermission(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun requestExternalStoragePermission() {
        initAppData()
    }

    @OnPermissionDenied(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun onRequestExternalStoragePermissionDenied() {
        alertPermissionDenied()
    }

    @OnNeverAskAgain(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun onRequestExternalStoragePermissionNeverAskAgain() {
        alertPermissionDenied()
    }

    @OnShowRationale(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun showPermissionRationaleForRequestExternalStorage(request: PermissionRequest) {
        // NOTE: Show a rationale to explain why the permission is needed, e.g. with a dialog.
        // Call proceed() or cancel() on the provided PermissionRequest to continue or abort
        request.proceed()
    }

    @SuppressLint("NeedOnRequestPermissionsResult")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun alertPermissionDenied() {
        AlertUtil.confirm(this, null, getString(R.string.grant_storage_permission),
                getString(R.string.btn_ok), getString(R.string.btn_cancel),
                false) { code, _, _ ->
            if (code == AlertUtil.POSITIVE_BUTTON) {
                requestExternalStoragePermissionWithPermissionCheck()
            } else {
                finish()
            }
        }
    }
}
