package gapp.season.qrcode

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri

object QrcodeHelper {
    var listener: QrcodeListener? = null

    fun init(listener: QrcodeListener) {
        this.listener = listener
    }

    fun scanQrcode(context: Context) {
        val intent = Intent(context, QrcodeScanActivity::class.java)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    interface QrcodeListener {
        fun onScanQRCodeSuccess(context: Context, result: String?): Boolean
        fun onBrowseResult(context: Context, result: String?): Boolean
        fun getFilePath(context: Context, uri: Uri?): String?
    }
}
