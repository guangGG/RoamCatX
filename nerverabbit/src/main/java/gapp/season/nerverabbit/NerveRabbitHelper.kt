package gapp.season.nerverabbit

import android.app.Activity
import android.content.Context
import android.content.Intent

object NerveRabbitHelper {
    fun startPlay(context: Context) {
        val intent = Intent(context, NerveRabbitActivity::class.java)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
