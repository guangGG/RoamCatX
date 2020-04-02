package gapp.season.roamcat.page.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.View
import android.webkit.URLUtil
import com.github.promeg.pinyinhelper.Pinyin
import com.github.promeg.tinypinyin.lexicons.android.cncity.CnCityDict
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.interfaces.OnSelectListener
import gapp.season.encryptlib.hash.HashExtUtil
import gapp.season.encryptlib.hash.HashUtil
import gapp.season.filemanager.FileManager
import gapp.season.roamcat.App
import gapp.season.roamcat.BuildConfig
import gapp.season.roamcat.R
import gapp.season.roamcat.data.net.AppNetwork
import gapp.season.roamcat.page.BaseFragment
import gapp.season.roamcat.page.setting.ClipboardActivity
import gapp.season.roamcat.util.PermissionsChecker
import gapp.season.util.file.FileUtil
import gapp.season.util.sys.ClipboardUtil
import gapp.season.util.sys.DeviceUtil
import gapp.season.util.sys.MemoryUtil
import gapp.season.util.sys.ScreenUtil
import gapp.season.util.text.StringUtil
import gapp.season.webbrowser.WebViewHelper
import kotlinx.android.synthetic.main.fragment_hot_action.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@SuppressLint("SetTextI18n", "DefaultLocale")
class HotActionFragment : BaseFragment() {
    companion object {
        const val REQ_CODE_SYS_INFO = 1
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_hot_action
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!baseActivity!!.supportImmersionBarVersion()) {
            titleLayout.setPadding(0, 0, 0, 0)
        }
        systemInfoBtn.setOnClickListener {
            if (!PermissionsChecker.checkSelfPermission(baseActivity!!, Manifest.permission.READ_PHONE_STATE)) {
                PermissionsChecker.requestPermissions(baseActivity!!, arrayOf(Manifest.permission.READ_PHONE_STATE), REQ_CODE_SYS_INFO)
                return@setOnClickListener
            }
            if (!PermissionsChecker.checkSelfPermission(baseActivity!!, Manifest.permission.ACCESS_FINE_LOCATION)) {
                PermissionsChecker.requestPermissions(baseActivity!!, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQ_CODE_SYS_INFO)
                return@setOnClickListener
            }
            showSysInfo()
        }
        systemSettingBtn.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val ssb = SpannableStringBuilder("系统设置主要页面快捷方式(点击跳转到相应页面)：\n")
                appendSetting(ssb, "系统设置主页", Settings.ACTION_SETTINGS)
                appendSetting(ssb, "显示设置", Settings.ACTION_DISPLAY_SETTINGS)
                appendSetting(ssb, "声音设置", Settings.ACTION_SOUND_SETTINGS)
                appendSetting(ssb, "日期/时间设置", Settings.ACTION_DATE_SETTINGS)
                appendSetting(ssb, "WiFi设置", Settings.ACTION_WIFI_SETTINGS)
                appendSetting(ssb, "内部存储设置", Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                appendSetting(ssb, "SD卡存储设置", Settings.ACTION_MEMORY_CARD_SETTINGS)
                appendSetting(ssb, "应用列表", Settings.ACTION_APPLICATION_SETTINGS)
                appendSetting(ssb, "应用管理", Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                appendSetting(ssb, "关于手机", Settings.ACTION_DEVICE_INFO_SETTINGS)
                ssb.append("\n系统设置-全部页面：\n")
                appendSetting(ssb, "无障碍", Settings.ACTION_ACCESSIBILITY_SETTINGS)
                appendSetting(ssb, "添加账号", Settings.ACTION_ADD_ACCOUNT)
                appendSetting(ssb, "更多连接方式", Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                appendSetting(ssb, "APN", Settings.ACTION_APN_SETTINGS)
                appendSetting(ssb, "*应用详情", Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                appendSetting(ssb, "开发者选项", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                appendSetting(ssb, "应用列表", Settings.ACTION_APPLICATION_SETTINGS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appendSetting(ssb, "*应用通知", Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    appendSetting(ssb, "*省电设置", Settings.ACTION_BATTERY_SAVER_SETTINGS)
                }
                appendSetting(ssb, "蓝牙", Settings.ACTION_BLUETOOTH_SETTINGS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    appendSetting(ssb, "字幕", Settings.ACTION_CAPTIONING_SETTINGS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    appendSetting(ssb, "投射", Settings.ACTION_CAST_SETTINGS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appendSetting(ssb, "*渠道通知", Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                }
                appendSetting(ssb, "漫游设置", Settings.ACTION_DATA_ROAMING_SETTINGS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    appendSetting(ssb, "**", Settings.ACTION_DATA_USAGE_SETTINGS)
                }
                appendSetting(ssb, "日期和时间", Settings.ACTION_DATE_SETTINGS)
                appendSetting(ssb, "状态信息", Settings.ACTION_DEVICE_INFO_SETTINGS)
                appendSetting(ssb, "显示", Settings.ACTION_DISPLAY_SETTINGS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    appendSetting(ssb, "屏保", Settings.ACTION_DREAM_SETTINGS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    appendSetting(ssb, "指纹登记", Settings.ACTION_FINGERPRINT_ENROLL)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appendSetting(ssb, "实体键盘", Settings.ACTION_HARD_KEYBOARD_SETTINGS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    appendSetting(ssb, "默认应用", Settings.ACTION_HOME_SETTINGS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appendSetting(ssb, "**", Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    appendSetting(ssb, "电池优化", Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                }
                appendSetting(ssb, "管理键盘", Settings.ACTION_INPUT_METHOD_SETTINGS)
                appendSetting(ssb, "输入法键盘设置", Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS)
                appendSetting(ssb, "存储空间", Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                appendSetting(ssb, "语言", Settings.ACTION_LOCALE_SETTINGS)
                appendSetting(ssb, "位置信息", Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                appendSetting(ssb, "应用管理", Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)
                appendSetting(ssb, "应用管理", Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appendSetting(ssb, "默认应用", Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    appendSetting(ssb, "显示在其它应用的上层", Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appendSetting(ssb, "安装未知应用", Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    appendSetting(ssb, "修改系统设置", Settings.ACTION_MANAGE_WRITE_SETTINGS)
                }
                appendSetting(ssb, "存储空间", Settings.ACTION_MEMORY_CARD_SETTINGS)
                appendSetting(ssb, "网络设置", Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
                appendSetting(ssb, "*NFC共享设置", Settings.ACTION_NFCSHARING_SETTINGS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    appendSetting(ssb, "*NFC支付设置", Settings.ACTION_NFC_PAYMENT_SETTINGS)
                }
                appendSetting(ssb, "*NFC设置", Settings.ACTION_NFC_SETTINGS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appendSetting(ssb, "护眼模式", Settings.ACTION_NIGHT_DISPLAY_SETTINGS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    appendSetting(ssb, "通知使用权", Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    appendSetting(ssb, "勿扰权限", Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    appendSetting(ssb, "打印", Settings.ACTION_PRINT_SETTINGS)
                }
                appendSetting(ssb, "备份和重置", Settings.ACTION_PRIVACY_SETTINGS)
                appendSetting(ssb, "**", Settings.ACTION_QUICK_LAUNCH_SETTINGS)
                @SuppressLint("BatteryLife")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    appendSetting(ssb, "**", Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appendSetting(ssb, "**", Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
                }
                appendSetting(ssb, "搜索设置", Settings.ACTION_SEARCH_SETTINGS)
                appendSetting(ssb, "安全性和位置信息", Settings.ACTION_SECURITY_SETTINGS)
                appendSetting(ssb, "设置主页", Settings.ACTION_SETTINGS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    appendSetting(ssb, "**", Settings.ACTION_SHOW_REGULATORY_INFO)
                }
                appendSetting(ssb, "声音和振动", Settings.ACTION_SOUND_SETTINGS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    appendSetting(ssb, "目录访问权限", Settings.ACTION_STORAGE_VOLUME_ACCESS_SETTINGS)
                }
                appendSetting(ssb, "同步", Settings.ACTION_SYNC_SETTINGS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    appendSetting(ssb, "使用情况访问权限", Settings.ACTION_USAGE_ACCESS_SETTINGS)
                }
                appendSetting(ssb, "个人字典", Settings.ACTION_USER_DICTIONARY_SETTINGS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    appendSetting(ssb, "**", Settings.ACTION_VOICE_CONTROL_AIRPLANE_MODE)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    appendSetting(ssb, "**", Settings.ACTION_VOICE_CONTROL_BATTERY_SAVER_MODE)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    appendSetting(ssb, "**", Settings.ACTION_VOICE_CONTROL_DO_NOT_DISTURB_MODE)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    appendSetting(ssb, "助手和语音输入", Settings.ACTION_VOICE_INPUT_SETTINGS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appendSetting(ssb, "VPN设置", Settings.ACTION_VPN_SETTINGS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appendSetting(ssb, "VR助手服务", Settings.ACTION_VR_LISTENER_SETTINGS)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appendSetting(ssb, "WebView实现", Settings.ACTION_WEBVIEW_SETTINGS)
                }
                appendSetting(ssb, "WLAN偏好设置", Settings.ACTION_WIFI_IP_SETTINGS)
                appendSetting(ssb, "WLAN(Wifi)", Settings.ACTION_WIFI_SETTINGS)
                appendSetting(ssb, "更多连接方式", Settings.ACTION_WIRELESS_SETTINGS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appendSetting(ssb, "防打扰", Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS)
                }
                infoView.text = ssb
            }

            private fun appendSetting(ssb: SpannableStringBuilder, name: String?, action: String) {
                val ss = SpannableString(action.replace("android.settings.", "").toLowerCase())
                ss.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        try {
                            val intent = Intent(action)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }, 0, ss.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                ssb.append(name ?: "").append(if (name == null) "" else "：").append(ss).append("\n")
            }
        })
        matchUrlBtn.setOnClickListener { matchUrl() }
        sandboxBtn.setOnClickListener {
            //沙盒浏览
            val ssb = SpannableStringBuilder()
            val baseDir = SpannableString(App.baseDir)
            baseDir.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    FileManager.enter(activity!!, baseDir.toString())
                }
            }, 0, baseDir.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            val innerDir = SpannableString(context!!.filesDir.parent)
            innerDir.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    FileManager.enter(activity!!, innerDir.toString())
                }
            }, 0, innerDir.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            val outerDir = SpannableString(context!!.getExternalFilesDir(null)!!.parent)
            outerDir.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    FileManager.enter(activity!!, outerDir.toString())
                }
            }, 0, outerDir.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            ssb.append("沙盒浏览(点击进入目录)：")
                    .append("\n应用SD卡存储目录：").append(baseDir)
                    .append("\n应用公开存储目录：").append(outerDir)
                    .append("\n应用内部存储目录：").append(innerDir)
                    .append("\n\n文件收藏夹：")
            FileManager.getFavorList().forEach {
                val filePath = SpannableString(it)
                filePath.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        FileManager.enter(activity!!, filePath.toString())
                    }
                }, 0, filePath.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                ssb.append("\n").append(filePath)
            }
            infoView.text = ssb
        }
        moreBtn.setOnClickListener {
            val hash = getString(R.string.ha_to_hash)
            val pinyin = getString(R.string.ha_to_pinyin)
            val links = "常用链接"
            val sysFile = "系统文件"
            val actions = if (BuildConfig.DEV) arrayOf(hash, pinyin, links, sysFile) else arrayOf(hash, pinyin)
            XPopup.Builder(baseActivity!!).atView(moreBtn).hasShadowBg(false).offsetY(ScreenUtil.dpToPx(3f))
                    .asAttachList(actions, null, object : OnSelectListener {
                        override fun onSelect(position: Int, text: String?) {
                            when (text) {
                                pinyin -> infoView.text = infoView.text.toString() + "\n--------\n" +
                                        Pinyin.toPinyin(infoView.text.toString(), " ")?.toLowerCase()
                                hash -> {
                                    val md5 = HashUtil.md5(infoView.text.toString())
                                    val sha1 = HashUtil.sha1(infoView.text.toString())
                                    val sha256 = HashUtil.sha256(infoView.text.toString())
                                    val sha512 = HashUtil.sha512(infoView.text.toString())
                                    val md5sha512 = HashUtil.md5sha512(infoView.text.toString())
                                    val hashCode = HashExtUtil.hashCode(infoView.text.toString())
                                    val modHash = HashExtUtil.modHash(infoView.text.toString(), 36)
                                    val modCheckCode = HashExtUtil.modCheckCode(infoView.text.toString())
                                    val xorHash = HashExtUtil.xorHash(infoView.text.toString().toByteArray())
                                    infoView.text = infoView.text.toString() + "\n--------\n" +
                                            "md5=$md5\nsha1=$sha1\nsha256=$sha256\nsha512=$sha512\nmd5sha512=$md5sha512\n" +
                                            "hashCode=$hashCode\nmodHash=$modHash\nmodCheckCode=$modCheckCode\nxorHash=$xorHash"
                                }
                                links -> {
                                    var marks = if (WebViewHelper.marks.isNullOrEmpty()) "(无)\n" else ""
                                    WebViewHelper.marks.forEach {
                                        marks = marks + it.title + "：" + it.url + "\n"
                                    }
                                    infoView.text = "浏览器书签：\n" + marks + "\n应用推荐：\n" +
                                            "RoamCatX(正式版): " + AppNetwork.URL_DOWNLOAD_APP + "\n" +
                                            "RoamCatX(开发版): " + AppNetwork.URL_DOWNLOAD_APP_DEV + "\n" +
                                            "诗文阅读(正式版): " + AppNetwork.URL_DOWNLOAD_POEM
                                    matchUrl()
                                }
                                sysFile -> {
                                    val ssb = SpannableStringBuilder()
                                    val mem = SpannableString("/proc/meminfo")
                                    mem.setSpan(object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            showFileInfo(mem.toString())
                                        }
                                    }, 0, mem.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    val host = SpannableString("/system/etc/hosts")
                                    host.setSpan(object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            showFileInfo(host.toString())
                                        }
                                    }, 0, host.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    val wifi = SpannableString("/data/misc/wifi/wpa_supplicant.conf")
                                    wifi.setSpan(object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            showFileInfo(wifi.toString()) //需要root
                                        }
                                    }, 0, wifi.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    val permission = SpannableString("/system/etc/permissions/platform.xml")
                                    permission.setSpan(object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            showFileInfo(permission.toString())
                                        }
                                    }, 0, permission.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    val font = SpannableString("/system/etc/fonts.xml")
                                    font.setSpan(object : ClickableSpan() {
                                        override fun onClick(widget: View) {
                                            showFileInfo(font.toString())
                                        }
                                    }, 0, font.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    ssb.append("常用系统文件(点击浏览文件内容，部分文件需要root权限)：")
                                            .append("\n内存信息文件：").append(mem)
                                            .append("\nhosts信息文件：").append(host)
                                            .append("\n权限信息文件：").append(permission)
                                            .append("\nwifi信息文件：").append(wifi)
                                            .append("\n字体信息文件：").append(font)
                                    infoView.text = ssb
                                }
                            }
                        }

                        private fun showFileInfo(path: String) {
                            val f = File(path)
                            infoView.text = "“$path”文件内容:\n" + FileUtil.getFileContent(f, null)
                        }
                    }).show()
        }
        copyBtn.setOnClickListener { ClipboardUtil.putText(baseActivity, infoView.text) }
        pasteBtn.setOnClickListener { infoView.text = ClipboardUtil.getText(baseActivity) }
        clipboardBtn.setOnClickListener { startActivity(Intent(baseActivity, ClipboardActivity::class.java)) }
        infoView.movementMethod = LinkMovementMethod.getInstance() //设置这个属性后，TextView中的ClickableSpan才可点击

        if (!BuildConfig.DEV) {
            //开发版本允许使用的功能
            sandboxBtn.visibility = View.GONE
        }

        // init utils
        Pinyin.init(Pinyin.newConfig().with(CnCityDict.getInstance(baseActivity!!)))

        showSysInfo()
    }

    @Suppress("DEPRECATION")
    private fun showSysInfo() {
        val sb = StringBuffer()
        try {
            // 显示日期(格式：12小时制hh:mm:ss;24小时制HH:mm:ss;星期几E;)
            val dt = Date()
            val sdf = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss ", Locale.CHINA)
            val timeNow = sdf.format(dt)
            val weekDays = arrayOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
            val cal = Calendar.getInstance()
            cal.time = dt
            val w = cal.get(Calendar.DAY_OF_WEEK) - 1
            sb.append("系统时间：" + timeNow + weekDays[w] + "\n")

            // 显示当前内存使用情况
            val totalMem = MemoryUtil.getTotalMem(baseActivity!!)
            val availMem = MemoryUtil.getAvailMem(baseActivity!!)
            if (totalMem > availMem) {
                val useMem = totalMem - availMem
                val tMem = MemoryUtil.formatMemorySize(totalMem, 2)
                val uMem = MemoryUtil.formatMemorySize(useMem, 2)
                sb.append("系统内存使用：" + uMem + "/" + tMem + " [" + getPercent(useMem, totalMem) + "]\n")
            } else {
                sb.append("系统可用内存大小：" + MemoryUtil.formatMemorySize(availMem, 2) + "\n")
            }
            // 每个应用进程都开一个虚拟机，下面得到的是主进程虚拟机的内存情况
            val runtime = Runtime.getRuntime()
            val maxR = runtime.maxMemory()
            val totalR = runtime.totalMemory()
            val freeR = runtime.freeMemory()
            val maxRS = MemoryUtil.formatMemorySize(maxR, 1)
            val totalRS = MemoryUtil.formatMemorySize(totalR, 1)
            val allocatedRS = MemoryUtil.formatMemorySize(totalR - freeR, 1)
            sb.append("应用内存使用：" + allocatedRS + "/" + totalRS + "/" + maxRS + " [" + getPercent(totalR, maxR) + "]\n")
            //sb.append("单个应用最大可申请内存：" + MemoryUtil.formatMemorySize(runtime.maxMemory(), 2) + "\n");
            //sb.append("当前应用申请内存：" + MemoryUtil.formatMemorySize(runtime.totalMemory(), 2) + "\n");
            //sb.append("当前应用空闲内存：" + MemoryUtil.formatMemorySize(runtime.freeMemory(), 2) + "\n");

            // 显示所有存储卡容量信息
            val storageList = FileUtil.getSdCards(baseActivity!!)
            for (sPath in storageList) {
                try {
                    val s = StatFs(sPath)
                    val tSize = s.blockCount.toLong() * s.blockSize
                    val uSize = s.blockCount.toLong() * s.blockSize - s.availableBlocks.toLong() * s.blockSize
                    val totalSize = MemoryUtil.formatMemorySize(tSize, 2)
                    val usedSize = MemoryUtil.formatMemorySize(uSize, 2)
                    if (Environment.getExternalStorageDirectory().absolutePath == sPath) {
                        // SD卡
                        sb.append("默认存储卡使用：")
                    } else {
                        // 其他存储卡(如：外置SD卡、USB设备等)
                        val cardName = File(sPath).name
                        sb.append("存储卡“$cardName”使用：")
                    }
                    sb.append(usedSize + "/" + totalSize + " [" + getPercent(uSize, tSize) + "]\n")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            //sb.append("SD卡总容量：" + MemoryUtil.formatMemorySize(MemoryUtil.getSDTotalSize(), 2) + "\n");
            //sb.append("SD卡剩余容量：" + MemoryUtil.formatMemorySize(MemoryUtil.getSDFreeSize(), 2) + "\n");

            // 显示屏幕分辨率(不包括底部虚拟按键栏)
            val heightPx = ScreenUtil.getScreenHeight(baseActivity!!)
            val widthPx = ScreenUtil.getScreenWidth(baseActivity!!)
            val heightDp = ScreenUtil.pxToDp(heightPx.toFloat()).roundToInt()
            val widthDp = ScreenUtil.pxToDp(widthPx.toFloat()).roundToInt()
            sb.append("屏幕分辨率：" + widthPx + "*" + heightPx + "像素 [" + widthDp + "*" + heightDp + "dp]\n")

            // 显示设备信息
            sb.append("设备类型：").append(if (DeviceUtil.isTablet(baseActivity!!)) "平板手机" else "手机").append("\n")
            sb.append("设备型号：").append(DeviceUtil.getOsModel()).append("\n")
            sb.append("设备系统版本：").append(DeviceUtil.getOsVersion()).append("\n")
            sb.append("设备CPU架构：").append(DeviceUtil.getCpuAbi()).append("\n")
            sb.append("设备是否root：").append(DeviceUtil.isRoot()).append("\n")
            sb.append("设备AndroidId：").append(DeviceUtil.getAndroidId(baseActivity!!)).append("\n")
            sb.append("设备IMEI：").append(DeviceUtil.getIMEI(baseActivity!!)).append("\n")
            sb.append("设备IMSI：").append(DeviceUtil.getIMSI(baseActivity!!)).append("\n")
            if (DeviceUtil.isGpsEnabled(baseActivity!!)) {
                sb.append("GPS状态：打开\n")
                val location = requestLocation()
                if (location.isNotEmpty()) sb.append("经纬度：").append(location).append("\n")
            }
            if (DeviceUtil.isBluetoothEnabled())
                sb.append("蓝牙状态：打开\n")
            val netName = (baseActivity?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo?.typeName
            if (!TextUtils.isEmpty(netName)) {
                sb.append("网络状态：").append(netName + "\n")
            }
            sb.append("Wifi-IP地址：").append(DeviceUtil.getWifiIp(baseActivity!!)).append("\n")
            sb.append("Wifi-Mac地址：").append(DeviceUtil.getWifiMac(baseActivity!!)).append("\n")
            sb.append("IP地址：").append(DeviceUtil.getLocalIps(true)).append("\n")
            sb.append("Mac地址：").append(DeviceUtil.getLocalMacs(true)).append("\n")
        } catch (e: Exception) {
            sb.insert(0, "获取系统信息失败：" + e.message + "\n\n")
            e.printStackTrace()
        }
        infoView.text = sb
    }

    private fun getPercent(num: Long, total: Long): String {
        if (num < 0 || total < num) {
            return "-"
        }
        val p = (num * 100 / total).toInt()
        return "$p%"
    }

    //从系统读取并更新一次当前地理定位
    private fun requestLocation(): String {
        try {
            val locationManager = (context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
            val providers = locationManager.getProviders(true)
            if (providers != null) {
                for (provider in providers) {
                    @SuppressLint("MissingPermission")
                    val location = locationManager.getLastKnownLocation(provider)
                    if (location != null) {
                        val localLongitude = location.longitude
                        val localLatitude = location.latitude
                        return "[$localLongitude,$localLatitude]"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    /*private val PATTERN_WEB_URL = "((?:(http|https|Http|Https|rtsp|Rtsp):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]" +
            "|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]" +
            "|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?((?:(?:[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}\\.)+(?:(?:aero|arpa|asia|a[cdefgilmnoqrstuwxz])" +
            "|(?:biz|b[abdefghijmnorstvwyz])|(?:cat|com|coop|c[acdfghiklmnoruvxyz])|d[ejkmoz]|(?:edu|e[cegrstu])|f[ijkmor]" +
            "|(?:gov|g[abdefghilmnpqrstuwy])|h[kmnrtu]|(?:info|int|i[delmnoqrst])|(?:jobs|j[emop])|k[eghimnprwyz]|l[abcikrstuvy]" +
            "|(?:mil|mobi|museum|m[acdeghklmnopqrstuvwxyz])|(?:name|net|n[acefgilopruz])|(?:org|om)|(?:pro|p[aefghklmnrstwy])|qa|r[eosuw]" +
            "|s[abcdeghijklmnortuvyz]|(?:tel|travel|t[cdfghjklmnoprtvwz])|u[agksyz]|v[aceginu]|w[fs]" +
            "|(?:δοκιμή|испытание|рф|срб|טעסט|آزمایشی|إختبار|الاردن|الجزائر|السعودية|المغرب|امارات|بھارت|تونس|سورية|فلسطين|قطر|مصر" +
            "|परीक्षा|भारत|ভারত|ਭਾਰਤ|ભારત|இந்தியா|இலங்கை|சிங்கப்பூர்|பரிட்சை|భారత్|ලංකා|ไทย|テスト" +
            "|中国|中國|台湾|台灣|新加坡|测试|測試|香港|테스트|한국|xn\\-\\-0zwm56d|xn\\-\\-11b5bs3a9aj6g|xn\\-\\-3e0b707e" +
            "|xn\\-\\-45brj9c|xn\\-\\-80akhbyknj4f|xn\\-\\-90a3ac|xn\\-\\-9t4b11yi5a|xn\\-\\-clchc0ea0b2g2a9gcd|xn\\-\\-deba0ad" +
            "|xn\\-\\-fiqs8s|xn\\-\\-fiqz9s|xn\\-\\-fpcrj9c3d|xn\\-\\-fzc2c9e2c|xn\\-\\-g6w251d|xn\\-\\-gecrj9c|xn\\-\\-h2brj9c" +
            "|xn\\-\\-hgbk6aj7f53bba|xn\\-\\-hlcj6aya9esc7a|xn\\-\\-j6w193g|xn\\-\\-jxalpdlp|xn\\-\\-kgbechtv|xn\\-\\-kprw13d" +
            "|xn\\-\\-kpry57d|xn\\-\\-lgbbat1ad8j|xn\\-\\-mgbaam7a8h|xn\\-\\-mgbayh7gpa|xn\\-\\-mgbbh1a71e|xn\\-\\-mgbc0a9azcg" +
            "|xn\\-\\-mgberp4a5d4ar|xn\\-\\-o3cw4h|xn\\-\\-ogbpf8fl|xn\\-\\-p1ai|xn\\-\\-pgbs0dh|xn\\-\\-s9brj9c|xn\\-\\-wgbh1c" +
            "|xn\\-\\-wgbl6a|xn\\-\\-xkc2al3hye2a|xn\\-\\-xkc2dl3a5ee0h|xn\\-\\-yfro4i67o|xn\\-\\-ygbi2ammx|xn\\-\\-zckzah|xxx)" +
            "|y[et]|z[amw]))|(?:(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}" +
            "|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(?:25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}" +
            "|[1-9][0-9]|[0-9])))(?:\\:\\d{1,5})?)((?:\\/(?:(?:[a-zA-Z0-9 -\uD7FF豈-\uFDCFﷰ-\uFFEF\\;\\/\\?\\:\\@\\&\\=\\#\\~\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])" +
            "|(?:\\%[a-fA-F0-9]{2}))+[\\.\\=\\?][a-zA-Z0-9\\%\\#\\&\\-\\_\\.\\~]*)|(?:\\/(?:(?:[a-zA-Z0-9\\;\\/\\?\\:\\@\\&\\=\\#\\~\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])" +
            "|(?:\\%[a-fA-F0-9]{2}))*))?(?:\\b|$|(?=[ -\uD7FF豈-\uFDCFﷰ-\uFFEF]))"*/
    private fun matchUrl() {
        infoView.text = StringUtil.toClickableSpannableString(infoView.text.toString(), Patterns.WEB_URL) {
            if (URLUtil.isNetworkUrl(it)) {
                WebViewHelper.showWebPage(baseActivity!!, it)
            }
        }
    }
}
