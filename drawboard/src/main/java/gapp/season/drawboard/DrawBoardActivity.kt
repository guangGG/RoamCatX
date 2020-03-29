package gapp.season.drawboard

import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import gapp.season.util.file.MediaScanUtil
import gapp.season.util.tips.ToastUtil
import gapp.season.util.view.ThemeUtil
import kotlinx.android.synthetic.main.drawb_activity.*
import me.panavtec.drawableview.DrawableViewConfig
import java.io.File

class DrawBoardActivity : AppCompatActivity() {
    private var hasInit = false
    private var strokeColor = Color.BLACK
    private var strokeWidth = 9f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this, 0)
        setContentView(R.layout.drawb_activity)
        drawbBack.setOnClickListener { finish() }
        drawbMenu.setOnClickListener {
            AlertDialog.Builder(this)
                    .setItems(arrayOf("还原配置", "保存到手机")) { _, which ->
                        when (which) {
                            0 -> {
                                strokeColor = Color.BLACK
                                strokeWidth = 9f
                                //无法还原缩放比例
                                updateBoard()
                            }
                            1 -> {
                                val bm = drawbPaintView.obtainBitmap()
                                @Suppress("DEPRECATION")
                                val file = File(Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_PICTURES), "drawboard_" + System.currentTimeMillis() + ".png")
                                DrawBoardHelper.compressBitmap(bm, file)
                                MediaScanUtil.scanFile(file.absolutePath) //更新系统图库
                                ToastUtil.showLong("保存图片成功")
                            }
                        }
                    }
                    .show()
        }
        drawbPaintView.viewTreeObserver.addOnGlobalLayoutListener {
            if (!hasInit) updateBoard()
            hasInit = true
        }
    }

    private fun updateBoard() {
        val config = DrawableViewConfig()
        config.isShowCanvasBounds = true // If the view is bigger than canvas, with this the user will see the bounds (Recommended)
        config.strokeColor = strokeColor
        config.strokeWidth = strokeWidth
        config.minZoom = 1f
        config.maxZoom = 5f
        config.canvasHeight = drawbPaintView.height
        config.canvasWidth = drawbPaintView.width
        drawbPaintView.setConfig(config)

        drawbColorView.setBackgroundColor(strokeColor)
        val lp = drawbWidthView.layoutParams
        lp.height = strokeWidth.toInt()
        drawbWidthView.layoutParams = lp
    }

    fun drawbClickBLACK(view: View) {
        strokeColor = Color.BLACK
        updateBoard()
    }

    fun drawbClickDKGRAY(view: View) {
        strokeColor = Color.DKGRAY
        updateBoard()
    }

    fun drawbClickGRAY(view: View) {
        strokeColor = Color.GRAY
        updateBoard()
    }

    fun drawbClickLTGRAY(view: View) {
        strokeColor = Color.LTGRAY
        updateBoard()
    }

    fun drawbClickWHITE(view: View) {
        strokeColor = Color.WHITE
        updateBoard()
    }

    fun drawbClickRED(view: View) {
        strokeColor = Color.RED
        updateBoard()
    }

    fun drawbClickGREEN(view: View) {
        strokeColor = Color.GREEN
        updateBoard()
    }

    fun drawbClickBLUE(view: View) {
        strokeColor = Color.BLUE
        updateBoard()
    }

    fun drawbClickYELLOW(view: View) {
        strokeColor = Color.YELLOW
        updateBoard()
    }

    fun drawbClickCYAN(view: View) {
        strokeColor = Color.CYAN
        updateBoard()
    }

    fun drawbClickMAGENTA(view: View) {
        strokeColor = Color.MAGENTA
        updateBoard()
    }

    fun drawbClickW1(view: View) {
        strokeWidth = 1f
        updateBoard()
    }

    fun drawbClickW3(view: View) {
        strokeWidth = 3f
        updateBoard()
    }

    fun drawbClickW5(view: View) {
        strokeWidth = 5f
        updateBoard()
    }

    fun drawbClickW7(view: View) {
        strokeWidth = 7f
        updateBoard()
    }

    fun drawbClickW9(view: View) {
        strokeWidth = 9f
        updateBoard()
    }

    fun drawbClickW12(view: View) {
        strokeWidth = 12f
        updateBoard()
    }

    fun drawbClickW15(view: View) {
        strokeWidth = 15f
        updateBoard()
    }

    fun drawbClickW18(view: View) {
        strokeWidth = 18f
        updateBoard()
    }

    fun drawbClickW21(view: View) {
        strokeWidth = 21f
        updateBoard()
    }

    fun drawbClickW25(view: View) {
        strokeWidth = 25f
        updateBoard()
    }

    fun drawbClickW30(view: View) {
        strokeWidth = 30f
        updateBoard()
    }

    fun drawbClickUndo(view: View) {
        drawbPaintView.undo()
    }

    fun drawbClickClear(view: View) {
        drawbPaintView.clear()
    }
}
