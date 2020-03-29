package gapp.season.calender;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.widget.TextView;

import com.haibin.calendarview.Calendar;
import com.haibin.calendarview.WeekBar;

import gapp.season.util.sys.ScreenUtil;

/**
 * 自定义星期栏(可选)，XML要使用merge布局
 */
public class CalenderWeekBar extends WeekBar {
    private int mPreSelectedIndex;

    public CalenderWeekBar(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.cv_week_bar, this, true);
        setBackgroundColor(Color.WHITE);
        int padding = ScreenUtil.dpToPx(10); //需和布局文件calendar_padding配置一样
        setPadding(padding, getPaddingTop(), padding, getPaddingBottom());
    }

    @Override
    protected void onDateSelected(Calendar calendar, int weekStart, boolean isClick) {
        getChildAt(mPreSelectedIndex).setSelected(false);
        int viewIndex = getViewIndexByCalendar(calendar, weekStart);
        getChildAt(viewIndex).setSelected(true);
        mPreSelectedIndex = viewIndex;
    }

    /**
     * 当周起始发生变化，使用自定义布局需要重写这个方法
     *
     * @param weekStart 周起始
     */
    @Override
    protected void onWeekStartChange(int weekStart) {
        for (int i = 0; i < getChildCount(); i++) {
            ((TextView) getChildAt(i)).setText(getWeekString(i, weekStart));
        }
    }

    /**
     * 获取周文本
     */
    private String getWeekString(int index, int weekStart) {
        String[] weeks = new String[]{"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        if (weekStart == 1) { //周日
            return weeks[index];
        }
        if (weekStart == 2) { //周一
            return weeks[index == 6 ? 0 : index + 1];
        }
        return weeks[index == 0 ? 6 : index - 1]; //周六
    }
}
