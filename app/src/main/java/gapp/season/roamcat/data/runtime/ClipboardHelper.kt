package gapp.season.roamcat.data.runtime

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import gapp.season.roamcat.data.file.MmkvUtil
import gapp.season.roamcat.page.setting.ClipboardActivity

object ClipboardHelper {
    private var clipChangedListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    var autoJump = false
    private const val MIN_JUMP_INTERVAL = 200 //设置最小跳转间隔，防止恶意更新剪贴板的情况
    private var lastJumpTime = 0L

    fun init(context: Context) {
        updateAutoJump()
        clipChangedListener = ClipboardManager.OnPrimaryClipChangedListener {
            if (autoJump && (System.currentTimeMillis() - lastJumpTime) > MIN_JUMP_INTERVAL) {
                val intent = Intent(context, ClipboardActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                lastJumpTime = System.currentTimeMillis()
            }
        }
        (context.getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager).addPrimaryClipChangedListener(clipChangedListener)
    }

    fun updateAutoJump() {
        autoJump = MmkvUtil.map()?.decodeBool(MmkvUtil.CLIPBOARD_AUTO_JUMP) ?: false
    }
}
