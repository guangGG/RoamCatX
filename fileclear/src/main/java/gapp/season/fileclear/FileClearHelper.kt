package gapp.season.fileclear

import android.content.Context
import gapp.season.util.tips.ToastUtil

object FileClearHelper {
    fun openPage(context: Context) {
        //todo
        ToastUtil.showShort("敬请期待")
        /*val intent = Intent(context, Activity::class.java)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)*/
    }
}
