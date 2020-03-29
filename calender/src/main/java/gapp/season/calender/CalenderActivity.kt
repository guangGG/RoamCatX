package gapp.season.calender

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.haibin.calendarview.Calendar
import com.haibin.calendarview.CalendarView
import gapp.season.util.sys.ScreenUtil
import gapp.season.util.text.StringUtil
import gapp.season.util.view.ThemeUtil
import kotlinx.android.synthetic.main.calender_activity.*
import java.util.*

class CalenderActivity : AppCompatActivity(), CalendarView.OnYearChangeListener, CalendarView.OnCalendarSelectListener {
    private var showYear: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this, 0)
        ScreenUtil.setSysBarColor(this, Color.WHITE, true)
        setContentView(R.layout.calender_activity)

        calendarBarMonthDay.setOnClickListener(View.OnClickListener {
            if (!calendarLayout.isExpand) { //显示周视图时展开为月视图
                calendarLayout.expand()
                return@OnClickListener
            }
            //展示为全屏年视图
            calendarView.showYearSelectLayout(showYear)
            calendarBarLunar.visibility = View.GONE
            calendarBarYear.visibility = View.GONE
            calendarBarMonthDay.text = showYear.toString()
        })
        //点击“今日”的按钮切换日期到今日
        calendarBarTodayLayout.setOnClickListener { calendarView.scrollToCurrent() }
        //设置监听器
        calendarView.setOnYearChangeListener(this) //设置视图年份改变回调
        calendarView.setOnCalendarSelectListener(this) //设置查看日期改变回调

        addCalenderTags() //增加一些记事、假日等日期标签

        //启动后切换到今日
        calendarBarToday.text = calendarView.curDay.toString()
        calendarView.scrollToCurrent()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        //启动后切换到今日
        calendarBarToday.text = calendarView.curDay.toString()
        calendarView.scrollToCurrent()
    }

    private fun addCalenderTags() {
        val map = HashMap<String, Calendar>()
        /*val year = calendarView.curYear
        val month = calendarView.curMonth
        val day = calendarView.curDay
        addCalenderTag(map, year, month, day, Color.BLACK, "今")*/
        addCalenderTag(map, 2019, 9, 29, 0xffff0000.toInt(), "班")
        addCalenderTag(map, 2019, 10, 1, 0xff008800.toInt(), "假")
        addCalenderTag(map, 2019, 10, 2, 0xff008800.toInt(), "假")
        addCalenderTag(map, 2019, 10, 3, 0xff008800.toInt(), "假")
        addCalenderTag(map, 2019, 10, 4, 0xff008800.toInt(), "假")
        addCalenderTag(map, 2019, 10, 5, 0xff008800.toInt(), "假")
        addCalenderTag(map, 2019, 10, 6, 0xff008800.toInt(), "假")
        addCalenderTag(map, 2019, 10, 7, 0xff008800.toInt(), "假")
        addCalenderTag(map, 2019, 10, 12, 0xffff0000.toInt(), "班")
        addCalenderTag(map, 2020, 1, 1, 0xff008800.toInt(), "假")
        //此方法在巨大的数据量上不影响遍历性能，推荐使用
        calendarView.setSchemeDate(map)
    }

    private fun addCalenderTag(map: HashMap<String, Calendar>, year: Int, month: Int, day: Int, color: Int, text: String) {
        val calendar = Calendar()
        calendar.year = year
        calendar.month = month
        calendar.day = day
        calendar.schemeColor = color//如果单独设置标记颜色则会使用这个颜色，否则用xml中配置的默认颜色
        calendar.scheme = text
        map[calendar.toString()] = calendar
    }

    override fun onCalendarOutOfRange(calendar: Calendar) {
    }

    override fun onCalendarSelect(calendar: Calendar, isClick: Boolean) {
        calendarBarLunar.visibility = View.VISIBLE
        calendarBarYear.visibility = View.VISIBLE
        calendarBarMonthDay.text = String.format("%d月%d日", calendar.month, calendar.day)
        calendarBarYear.text = calendar.year.toString()
        calendarBarLunar.text = calendar.lunar
        showYear = calendar.year

        calendarMessage.text = String.format(" 公历：%s \n 农历：%s \n 节气：%s \n 公历节日：%s \n 农历节日：%s \n 是否闰月：%s",
                calendar.year.toString() + "年" + calendar.month.toString() + "月" + calendar.day + "日",
                StringUtil.toCNBigWrite(calendar.lunarCalendar.month.toLong(), 1) + "月"
                        + StringUtil.toCNBigWrite(calendar.lunarCalendar.day.toLong(), 1) + "号",
                if (TextUtils.isEmpty(calendar.solarTerm)) "无" else calendar.solarTerm,
                if (TextUtils.isEmpty(calendar.gregorianFestival)) "无" else calendar.gregorianFestival,
                if (TextUtils.isEmpty(calendar.traditionFestival)) "无" else calendar.traditionFestival,
                if (calendar.leapMonth == 0) "否" else String.format("闰%s月", calendar.leapMonth))
    }

    override fun onYearChange(year: Int) { //全屏年视图时年份切换时更新展示
        calendarBarMonthDay.text = year.toString()
        calendarBarLunar.visibility = View.GONE
        calendarBarYear.visibility = View.GONE
    }
}
