#引入的BeanShell中有不少用不到的不兼容问题(如:AWTConsole类继承自TextArea,TextArea在安卓中是没有的),这里设置屏蔽掉打包时的警告
-dontwarn bsh.**
-keep class bsh.** {*;}
