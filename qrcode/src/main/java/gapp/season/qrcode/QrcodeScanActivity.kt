@file:Suppress("DEPRECATION")

package gapp.season.qrcode

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.text.ClipboardManager
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import cn.bingoogolapple.qrcode.core.QRCodeView
import gapp.season.util.view.ThemeUtil
import kotlinx.android.synthetic.main.qrcode_activity_scan.*

class QrcodeScanActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_CHOOSE_IMAGE = 1001
    }

    private var isShowResult = false
    private var openFlashligh = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this, 0)
        setContentView(R.layout.qrcode_activity_scan)

        qrcodeBack.setOnClickListener { finish() }
        qrcodeFromImg.setOnClickListener {
            val innerIntent = Intent(Intent.ACTION_GET_CONTENT)
            innerIntent.type = "image/*"
            val intent = Intent.createChooser(innerIntent, "选择图片")
            startActivityForResult(intent, REQUEST_CODE_CHOOSE_IMAGE)
        }
        qrcodeFlashlight.setColorFilter(Color.WHITE)
        qrcodeFlashlight.setOnClickListener {
            openFlashligh = !openFlashligh
            if (openFlashligh) {
                qrcodeView.openFlashlight()
                qrcodeFlashlight.setImageResource(R.drawable.qrcode_flashlight_on)
            } else {
                qrcodeView.closeFlashlight()
                qrcodeFlashlight.setImageResource(R.drawable.qrcode_flashlight_off)
            }
        }

        qrcodeView.setDelegate(object : QRCodeView.Delegate {
            override fun onScanQRCodeSuccess(result: String?) {
                if (QrcodeHelper.listener?.onScanQRCodeSuccess(this@QrcodeScanActivity, result) != true) {
                    //扫码结果(扫描图片的结果可能为空)
                    vibrate()
                    AlertDialog.Builder(this@QrcodeScanActivity)
                            .setTitle("扫描结果")
                            .setMessage(result)
                            .setPositiveButton("复制") { _, _ ->
                                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.text = result
                                Toast.makeText(this@QrcodeScanActivity, "复制成功", Toast.LENGTH_SHORT).show()
                            }.setNegativeButton("取消", null)
                            .setNeutralButton("浏览扫描结果") { _, _ ->
                                if (QrcodeHelper.listener?.onBrowseResult(this@QrcodeScanActivity, result) != true) {
                                    try {
                                        if (!TextUtils.isEmpty(result)) {
                                            val uri = Uri.parse(result)
                                            val intent = Intent("android.intent.action.VIEW", uri)
                                            if (intent.resolveActivity(packageManager) != null) {
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                startActivity(intent)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }.setOnDismissListener {
                                isShowResult = false
                                qrcodeView.startSpotAndShowRect()
                            }.show()
                    isShowResult = true
                    qrcodeView.stopSpotAndHiddenRect()
                }
            }

            override fun onCameraAmbientBrightnessChanged(isDark: Boolean) {}

            override fun onScanQRCodeOpenCameraError() {
                finish() //出现异常时关闭页面
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CHOOSE_IMAGE && resultCode == Activity.RESULT_OK) {
            qrcodeView.decodeQRCode(QrcodeHelper.listener?.getFilePath(this, data?.data)
                    ?: (data?.data?.path))
        }
    }

    override fun onStart() {
        super.onStart()
        qrcodeView.startCamera() //打开后置摄像头开始预览，但是并未开始识别
        if (!isShowResult) {
            qrcodeView.startSpotAndShowRect() // 显示扫描框，并开始识别
        }
    }

    override fun onStop() {
        qrcodeView.stopCamera() //关闭摄像头预览
        qrcodeView.stopSpotAndHiddenRect() //停止识别并且隐藏扫描框
        super.onStop()
    }

    override fun onDestroy() {
        if (openFlashligh) {
            openFlashligh = false
            qrcodeView.closeFlashlight()
            qrcodeFlashlight.setImageResource(R.drawable.qrcode_flashlight_off)
        }
        qrcodeView.onDestroy()
        super.onDestroy()
    }

    private fun vibrate() { //扫码成功时振动一下
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(200)
    }
}
