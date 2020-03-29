package gapp.season.drawboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import java.io.*

object DrawBoardHelper {
    fun openDrawBoard(context: Context) {
        val intent = Intent(context, DrawBoardActivity::class.java)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun compressBitmap(bm: Bitmap, f: File): String {
        if (f.exists()) {
            f.delete()
        }
        if (null != f.parentFile && !f.parentFile!!.exists()) {
            f.mkdirs()
        }
        try {
            val out = FileOutputStream(f)
            bm.compress(Bitmap.CompressFormat.PNG, 80, out)
            val baos = ByteArrayOutputStream()

            while (baos.toByteArray().size > 1048576) {
                baos.reset()
                bm.compress(Bitmap.CompressFormat.PNG, 50, out)
            }
            out.flush()
            out.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return f.absolutePath
    }
}
