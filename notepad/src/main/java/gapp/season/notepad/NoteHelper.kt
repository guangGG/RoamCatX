package gapp.season.notepad

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Base64
import gapp.season.encryptlib.code.HexUtil
import gapp.season.encryptlib.symmetric.AESUtil
import gapp.season.notepad.db.NoteDbHelper
import gapp.season.util.log.LogUtil
import gapp.season.util.task.OnTaskCode
import gapp.season.util.task.OnTaskDone
import gapp.season.util.text.DateUtil
import gapp.season.util.tips.ToastUtil
import java.io.File

object NoteHelper {
    private var privacyKey: String? = null
    private var noteDir: String? = null

    fun init(context: Context, noteDir: String) {
        NoteDbHelper.init(context)
        this.noteDir = noteDir
    }

    fun openNote(context: Context, privacy: Boolean = false) {
        if (privacy) {
            val authenticate = BiometricPromptHelper.authenticate(context, OnTaskDone { code, msg, data ->
                if (code == OnTaskCode.CODE_SUCCESS && !data.isNullOrBlank()) {
                    privacyKey = data
                    toNotePage(context, privacy)
                } else if (code == OnTaskCode.CODE_FAIL) {
                    ToastUtil.showShort("身份验证失败：$msg")
                }
            })
            if (!authenticate) throw RuntimeException("BiometricPromptHelper must come from FragmentActivity")
        } else {
            toNotePage(context, privacy)
        }
    }

    private fun toNotePage(context: Context, privacy: Boolean) {
        val intent = Intent(context, NotePadActivity::class.java)
        intent.putExtra("privacy", privacy)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    internal fun encode(data: String?): String? {
        try {
            if (!data.isNullOrEmpty() && privacyKey != null) {
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Base64.encodeToString(AESUtil.encryptGCM(data.toByteArray(), HexUtil.decodeHexStr(privacyKey), HexUtil.decodeHexStr(privacyKey)), Base64.DEFAULT)
                } else {
                    Base64.encodeToString(AESUtil.encrypt(data.toByteArray(), HexUtil.decodeHexStr(privacyKey), null, AESUtil.AES_ALGORITHM_ECB), Base64.DEFAULT)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    internal fun decode(data: String?, privacy: Boolean): String {
        try {
            return if (privacy && !data.isNullOrEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    String(AESUtil.decryptGCM(Base64.decode(data, Base64.DEFAULT), HexUtil.decodeHexStr(privacyKey), HexUtil.decodeHexStr(privacyKey)))
                } else {
                    String(AESUtil.decrypt(Base64.decode(data, Base64.DEFAULT), HexUtil.decodeHexStr(privacyKey), null, AESUtil.AES_ALGORITHM_ECB))
                }
            } else {
                data ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    internal fun formatTime(time: Long): String = DateUtil.getDateStr(time, "yyyy年M月d日 H:mm")
    internal fun backUpFile(): File {
        return File(noteDir ?: "/", "note-back-up.json")
    }

    internal fun saveFontSize(context: Context, size: Float) {
        LogUtil.d("NoteHelper", "saveFontSize: $size")
        context.getSharedPreferences("NotePad", Context.MODE_PRIVATE)?.edit()?.putFloat("note_edit_font_size", size)?.apply()
    }

    internal fun getFontSize(context: Context): Float {
        return context.getSharedPreferences("NotePad", Context.MODE_PRIVATE)?.getFloat("note_edit_font_size", 20f)
                ?: 20f
    }
}
