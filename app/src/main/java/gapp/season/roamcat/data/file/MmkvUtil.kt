package gapp.season.roamcat.data.file

import android.content.Context
import com.tencent.mmkv.MMKV

/**
 * 使用MMKV存储和读取数据(同步读写，代替SharedPreferences)
 * 存储数据： MmkvUtil.map().encode(key, value) (或put)
 * 读取数据： MmkvUtil.map().decodeType(key, default) (或get)
 */
object MmkvUtil {
    private const val CRYPT_KEY = "mmkv_key"
    private var mmkv: MMKV? = null
    private var hasInit = false
    private var customMmkvs: MutableMap<String, MMKV> = mutableMapOf()

    fun init(context: Context) {
        if (!hasInit || mmkv == null) {
            MMKV.initialize(context)
            mmkv = MMKV.defaultMMKV(MMKV.MULTI_PROCESS_MODE, CRYPT_KEY)
            hasInit = true
        }
    }

    fun map(): MMKV? {
        return mmkv
    }

    fun map(mmapID: String): MMKV? {
        if (customMmkvs.containsKey(mmapID)) {
            return customMmkvs[mmapID]
        }
        val kv = MMKV.mmkvWithID(mmapID)
        customMmkvs.put(mmapID, kv)
        return kv
    }

    //自定义mmapID
    const val PLUGIN_OPEN_TAG = "plugin_open_" //标记插件开启状态
    //Key-Value列表
    const val LANGUAGE_TAG = "language_tag" //记录应用当前设置的语言
    const val CLIPBOARD_AUTO_JUMP = "clipboard_auto_jump" //标记剪贴板更新后是否立即查看
}
