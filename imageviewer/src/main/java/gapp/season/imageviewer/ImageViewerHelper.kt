package gapp.season.imageviewer

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import androidx.exifinterface.media.ExifInterface
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import gapp.season.util.file.ImgUtil
import gapp.season.util.sys.ClipboardUtil
import gapp.season.util.sys.MemoryUtil
import gapp.season.util.task.OnTaskDone
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*


object ImageViewerHelper {
    private const val IMAGE_SUFFIX = ".jpg,.jpeg,.png,.bmp,.gif,.ico,.raw,.pcx,.heif,.heic" //.hei*文件为ios系统图片文件
    const val INTENT_TYPE = "intent_type"
    const val INTENT_TYPE_SINGLE = 1
    const val INTENT_TYPE_MULTIPLE = 2

    const val TASK_CODE_IMG_POSITION = 11 //data返回图片文件绝对路径的String值

    var isDev = false
    var callBack: OnTaskDone<Any>? = null
    //暂时记录传来的IntentExtra，到Activity中再从这里取(防止列表数据大小超过限制时传递失败)
    var imgPath: String? = null
    var imgPaths: List<String>? = null
    var index: Int = 0

    fun init(application: Application, dev: Boolean, callBack: OnTaskDone<Any>) {
        this.isDev = dev
        this.callBack = callBack
        BigImageViewer.initialize(GlideImageLoader.with(application))
    }

    /**
     * 浏览上次看过的图片
     */
    fun show(context: Context) {
        val intent = Intent(context, ImageViewerActivity::class.java)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 浏览单张图片(会同时浏览同目录下的其它图片)
     */
    fun showImage(context: Context, imgPath: String?) {
        this.imgPath = imgPath
        val intent = Intent(context, ImageViewerActivity::class.java)
        intent.putExtra(INTENT_TYPE, INTENT_TYPE_SINGLE)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 浏览多张图片
     */
    fun showImages(context: Context, imgPaths: List<String>?, index: Int) {
        this.imgPaths = imgPaths
        this.index = index
        val intent = Intent(context, ImageViewerActivity::class.java)
        intent.putExtra(INTENT_TYPE, INTENT_TYPE_MULTIPLE)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun savePath(context: Context?, path: String?) {
        context?.getSharedPreferences("ImageViewer", Context.MODE_PRIVATE)?.edit()?.putString("last_show_img", path)?.apply()
    }

    fun getSavedPath(context: Context?): String? {
        return context?.getSharedPreferences("ImageViewer", Context.MODE_PRIVATE)?.getString("last_show_img", null)
    }

    fun resourceIdToUri(context: Context, resourceId: Int): Uri {
        return Uri.parse("android.resource://" + context.packageName + "/" + resourceId)
    }

    fun isImage(file: File?): Boolean {
        if (file?.exists() == true) {
            val extension = "." + file.extension
            return extension.length > 1 && IMAGE_SUFFIX.contains(extension, true)
        }
        return false
    }

    fun showImageInfo(context: Context, image: File?) {
        showImageInfo(context, image, isDev, callBack)
    }

    fun showImageInfo(context: Context, image: File?, isDev: Boolean, onTaskDone: OnTaskDone<Any>?) {
        AlertDialog.Builder(context)
                .setMessage(getImageInfo(image, isDev))
                .setNegativeButton("取消", null)
                .setPositiveButton("复制全路径") { _, _ ->
                    ClipboardUtil.putText(context, image?.absolutePath)
                }.setNeutralButton("查看文件位置") { _, _ ->
                    onTaskDone?.onTaskDone(TASK_CODE_IMG_POSITION, image?.name, image?.absolutePath)
                }.show()
    }

    private fun getImageInfo(image: File?, isDev: Boolean): String {
        if (image?.exists() != true) {
            return "图片不存在"
        }
        val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.CHINA)
        val dfGps = DecimalFormat("#.######")
        val df2 = DecimalFormat("#.##")
        val sb = StringBuilder()
        sb.append("图片路径：" + image.absolutePath)
        sb.append("\n\n图片大小：" + MemoryUtil.formatMemorySize(image.length(), 2) + "(" + image.length() + "B)")
        sb.append("\n修改时间：" + sdf.format(image.lastModified()))
        try {
            val ei = ExifInterface(image.absolutePath)
            val tagDatetime = ei.getAttribute(ExifInterface.TAG_DATETIME)
            //String TAG_FLASH = ei.getAttribute(ExifInterface.TAG_FLASH);//闪光灯
            val tagFocalLength = ei.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
            val focalLength35mm = ei.getAttribute("FocalLengthIn35mmFilm")
            val tagImageWidth = ei.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
            val tagImageLength = ei.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
            val tagMake = ei.getAttribute(ExifInterface.TAG_MAKE)
            val tagModel = ei.getAttribute(ExifInterface.TAG_MODEL)
            val tagOrientation = ei.getAttribute(ExifInterface.TAG_ORIENTATION)
            //String TAG_WHITE_BALANCE = ei.getAttribute(ExifInterface.TAG_WHITE_BALANCE);//白平衡
            addParamStr(sb, "拍摄时间", tagDatetime)
            addParamStr(sb, "图片宽度", tagImageWidth)
            addParamStr(sb, "图片高度", tagImageLength)
            addParamStr(sb, "图片旋转", ImgUtil.getExifOrientationInfo(tagOrientation))
            addParamStr(sb, "设备厂商", tagMake)
            addParamStr(sb, "设备型号", tagModel)
            addParamStr(sb, "相机画幅", ImgUtil.getExifSensingSizeInfo(tagFocalLength, focalLength35mm))
            addParamStr(sb, "等效焦距", focalLength35mm)
            addParamStr(sb, "焦距", tagFocalLength)
            val tagAperture = ei.getAttribute(ExifInterface.TAG_APERTURE_VALUE)
            val tagExposureTime = ei.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
            val tagIso = ei.getAttribute(ExifInterface.TAG_ISO_SPEED)
            addParamStr(sb, "光圈", ImgUtil.getExifApertureInfo(tagAperture))
            addParamStr(sb, "快门", ImgUtil.getExifExposureTimeInfo(tagExposureTime))
            addParamStr(sb, "ISO", tagIso)
            val gpsLon = ImgUtil.getExifGPSLongitude(ei)
            val gpsLat = ImgUtil.getExifGPSLatitude(ei)
            val gpsAlt = ImgUtil.getExifGPSAltitude(ei)
            if (gpsLon != 0f || gpsLat != 0f || gpsAlt != 0f) {
                addParamStr(sb, "经度", dfGps.format(gpsLon.toDouble()) + "°")
                addParamStr(sb, "纬度", dfGps.format(gpsLat.toDouble()) + "°")
                addParamStr(sb, "海拔", df2.format(gpsAlt.toDouble()) + "m")
            }
            //额外全部信息显示
            if (isDev) {
                sb.append("\n")
                //图片所有的Exif属性值
                val field = ExifInterface::class.java.getDeclaredField("mAttributes")
                field.isAccessible = true
                val attributes = field.get(ei)
                val list = ArrayList<Map<*, *>>()
                if (attributes is Map<*, *>) {
                    list.add(attributes)
                } else if (attributes is Array<*>) {
                    for (map in attributes) {
                        if (map is Map<*, *>)
                            list.add(map)
                    }
                }
                for (map in list) {
                    if (map.isNotEmpty()) {
                        sb.append("\n--------")
                        for (key in map.keys) {
                            if (key != null)
                                sb.append("\n").append(key).append("：").append(ei.getAttribute(key.toString()))//map.get(key)获取的非String类型
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }
        return sb.toString()
    }


    private fun addParamStr(sb: StringBuilder, keyStr: String, valueStr: String?) {
        if (!TextUtils.isEmpty(valueStr))
            sb.append("\n").append(keyStr).append("：").append(valueStr)
    }
}
