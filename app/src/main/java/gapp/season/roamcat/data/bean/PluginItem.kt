package gapp.season.roamcat.data.bean

import android.view.View

class PluginItem {
    companion object {
        const val TYPE_BASE = 0
        const val TYPE_EXTEND = 1
        const val TYPE_DEV = 2
    }

    var name: String? = null //名称（作为键值，需确保唯一性）
    var type: Int = TYPE_BASE //Item类型：基础功能、扩展功能、开发版功能
    var iconId: Int = 0 //图标
    var title: String? = null //标题
    var message: String? = null //说明信息
    var listener: View.OnClickListener? = null //点击操作
    var minSysVersion: Int = 0 //最低支持系统版本
    var maxSysVersion: Int = Int.MAX_VALUE //最高支持系统版本
    var defaultOpen = false //默认开启状态
    var needPermissions = mutableListOf<String>()

    var realItem: Boolean = false //标记是真实的Item还是补位Item
    var isOpen: Boolean = false //是否开启(基础功能默认为开启，不受此字段影响)

    constructor()

    constructor(name: String, type: Int, iconId: Int, title: String?, message: String?, listener: View.OnClickListener?)
            : this(name, type, iconId, title, message, false, listener)

    constructor(name: String, type: Int, iconId: Int, title: String?, message: String?, defaultOpen: Boolean, listener: View.OnClickListener?) {
        realItem = true
        this.name = name
        this.type = type
        this.iconId = iconId
        this.title = title
        this.message = message
        this.defaultOpen = defaultOpen
        this.listener = listener
        if (type == TYPE_BASE) {
            this.defaultOpen = true
            isOpen = true
        }
    }
}
