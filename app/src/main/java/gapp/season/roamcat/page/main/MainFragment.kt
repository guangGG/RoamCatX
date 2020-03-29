package gapp.season.roamcat.page.main

import android.os.Build
import android.os.Bundle
import android.view.View
import gapp.season.roamcat.BuildConfig
import gapp.season.roamcat.R
import gapp.season.roamcat.data.bean.PluginItem
import gapp.season.roamcat.data.event.PluginsUpdateEvent
import gapp.season.roamcat.data.runtime.PluginHelper
import gapp.season.roamcat.page.BaseFragment
import gapp.season.roamcat.page.widget.MainItemView
import gapp.season.roamcat.util.PermissionsChecker
import gapp.season.util.log.LogUtil
import gapp.season.util.sys.ScreenUtil
import kotlinx.android.synthetic.main.fragment_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainFragment : BaseFragment() {
    private var itemCount = 0 //计数器

    override fun getLayoutId(): Int {
        return R.layout.fragment_main
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initItems()
        EventBus.getDefault().register(this)
    }

    override fun onDestroyView() {
        EventBus.getDefault().unregister(this)
        super.onDestroyView()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPluginsUpdateEvent(event: PluginsUpdateEvent) {
        initItems()
    }

    private fun initItems() {
        itemCount = 0
        flexItems.removeAllViews()
        PluginHelper.openPlugins.forEach {
            addItem(it)
        }
        //用占位Item补齐最后一行
        val lineNum = ScreenUtil.getScreenWidth(baseActivity) / ScreenUtil.dpToPx(MainItemView.widthOfDp.toFloat())
        LogUtil.d("MainFragmentTag", "initItems itemCount=$itemCount lineNum=$lineNum")
        if (itemCount % lineNum != 0) {
            val num = lineNum - (itemCount % lineNum)
            for (i in 1..num) {
                addItem(PluginItem()) //占位Item
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun addItem(pluginItem: PluginItem) {
        if (pluginItem.realItem) {
            if (pluginItem.type == PluginItem.TYPE_BASE || (pluginItem.isOpen && (pluginItem.minSysVersion < Build.VERSION.SDK_INT) &&
                            (pluginItem.maxSysVersion > Build.VERSION.SDK_INT) && (BuildConfig.DEV || pluginItem.type != PluginItem.TYPE_DEV))) {
                val itemView = MainItemView(baseActivity!!)
                if (pluginItem.iconId != 0) itemView.updateIcon(resources.getDrawable(pluginItem.iconId))
                if (pluginItem.title != null) itemView.updateTitle(pluginItem.title)
                if (pluginItem.listener != null) {
                    itemView.setOnClickListener { it ->
                        pluginItem.needPermissions.forEach {
                            if (!PermissionsChecker.checkSelfPermission(baseActivity!!, it)) {
                                PermissionsChecker.requestPermissions(baseActivity!!, arrayOf(it), 0)
                                return@setOnClickListener
                            }
                        }
                        pluginItem.listener?.onClick(it)
                    }
                }
                flexItems.addView(itemView)
                itemCount++
            }
        } else {
            flexItems.addView(MainItemView(baseActivity!!))
        }
    }
}
