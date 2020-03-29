# Calender组件混淆规则(内部通过反射映射日历的组件)
-keep class com.haibin.calendarview.** {*;}
-dontwarn com.haibin.calendarview.**
-keep class gapp.season.calender.CalenderMonthView {
    public <init>(android.content.Context);
}
-keep class gapp.season.calender.CalenderWeekBar {
    public <init>(android.content.Context);
}
-keep class gapp.season.calender.CalenderWeekView {
    public <init>(android.content.Context);
}
-keep class gapp.season.calender.CalenderYearView {
    public <init>(android.content.Context);
}
