package gapp.season.calender

import android.app.Activity
import android.content.Context
import android.content.Intent

/**
 * 日历页，MonthView/WeekView等是通过反射配置到CalendarView的，所以混淆时应绕过
 */
object CalenderHelper {
    fun openCalender(context: Context) {
        val intent = Intent(context, CalenderActivity::class.java)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
