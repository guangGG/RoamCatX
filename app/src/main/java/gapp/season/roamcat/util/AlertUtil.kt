package gapp.season.roamcat.util

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import gapp.season.roamcat.R
import gapp.season.util.app.AppUtil
import gapp.season.util.task.OnTaskDone

@Deprecated("use gapp.season.util.tips.AlertUtil instead")
object AlertUtil {
    const val POSITIVE_BUTTON = 1
    const val NEUTRAL_BUTTON = 2
    const val NEGATIVE_BUTTON = 3

    fun alert(context: Context, title: String?, msg: String?, cancelable: Boolean) {
        show(context, title, msg, AppUtil.getString(R.string.btn_ok),
                null, null, cancelable, null)
    }

    fun confirm(context: Context, title: String?, msg: String?, cancelable: Boolean, listener: OnTaskDone<DialogInterface>?) {
        show(context, title, msg, AppUtil.getString(R.string.btn_ok), null,
                AppUtil.getString(R.string.btn_cancel), cancelable, listener)
    }

    fun show(context: Context, title: String?, msg: String?, positiveButton: String?, neutralButton: String?,
             negativeButton: String?, cancelable: Boolean, listener: OnTaskDone<DialogInterface>?) {
        AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(positiveButton) { dialog, which ->
                    listener?.onTaskDone(POSITIVE_BUTTON, msg, dialog)
                }.setNeutralButton(neutralButton) { dialog, which ->
                    listener?.onTaskDone(NEUTRAL_BUTTON, msg, dialog)
                }.setNegativeButton(negativeButton) { dialog, which ->
                    listener?.onTaskDone(NEGATIVE_BUTTON, msg, dialog)
                }.setCancelable(cancelable)
                .show()
    }

    fun list(context: Context, title: String?, items: Array<CharSequence>,
             listener: DialogInterface.OnClickListener?, cancelable: Boolean) {
        AlertDialog.Builder(context)
                .setTitle(title)
                .setItems(items, listener)
                .setCancelable(cancelable)
                .show()
    }
}
