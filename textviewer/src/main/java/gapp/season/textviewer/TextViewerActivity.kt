package gapp.season.textviewer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import gapp.season.util.sys.MemoryUtil
import gapp.season.util.tips.ToastUtil
import gapp.season.util.view.ThemeUtil
import kotlinx.android.synthetic.main.textv_activity.*
import java.io.File
import java.text.DecimalFormat

class TextViewerActivity : AppCompatActivity() {
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        getIntentData(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this, 0)
        setContentView(R.layout.textv_activity)
        textvBack.setOnClickListener { finish() }
        textvTitle.setOnClickListener {
            //显示文件属性信息
            AlertDialog.Builder(this)
                    .setTitle("文件信息")
                    .setMessage("文件路径：" + (textvView.fileData?.absolutePath
                            ?: "") + "\n文件大小：" + MemoryUtil.formatMemorySize((textvView.fileData?.length()
                            ?: 0), 2))
                    .setPositiveButton("确定", null)
                    .show()
        }
        textvCharset.setOnClickListener {
            var checkedItem = 0
            VastTextView.CHARSETS.forEachIndexed { index, s ->
                if (s == textvView.charset) {
                    checkedItem = index
                    return@forEachIndexed
                }
            }
            AlertDialog.Builder(this)
                    .setSingleChoiceItems(VastTextView.CHARSETS, checkedItem) { dialog, i ->
                        val charset = VastTextView.CHARSETS[i]
                        textvView.charset = charset
                        dialog.dismiss()
                    }
                    .show()
        }
        textvProgress.setOnClickListener {
            if (textvProgressLayout.visibility == View.VISIBLE) {
                textvProgressLayout.visibility = View.GONE
            } else {
                textvProgressLayout.visibility = View.VISIBLE
            }
        }
        textvProgressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    textvView.toProgress(1f * progress / 10000)
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {
            }

            override fun onStopTrackingTouch(sb: SeekBar?) {
            }
        })
        textvView.setProgressListener { progress, _, _, _ ->
            val df = DecimalFormat("0.0%")
            textvProgress.text = df.format(progress)
            textvProgressBar.progress = (progress * 10000).toInt()
        }

        getIntentData(intent)
    }

    private fun getIntentData(intent: Intent?) {
        val filePath =
                if (intent?.action == Intent.ACTION_VIEW) {
                    intent.data?.path ?: ""
                } else {
                    intent?.extras?.getString(TextViewerHelper.INTENT_FILE_PATH)
                            ?: TextViewerHelper.getSavedPath(applicationContext) ?: ""
                }
        if (filePath.isNotEmpty()) TextViewerHelper.savePath(this, filePath)
        val file = File(filePath)
        if (file.exists()) {
            textvTitle.text = file.name
            textvView.setFileData(file, null)
        } else {
            ToastUtil.showLong("文件不存在")
        }
    }
}
