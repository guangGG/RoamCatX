package gapp.season.roamcat.page.find

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import gapp.season.roamcat.BuildConfig
import gapp.season.roamcat.R
import gapp.season.roamcat.data.bean.PluginItem
import gapp.season.roamcat.data.event.PluginsUpdateEvent
import gapp.season.roamcat.data.file.MmkvUtil
import gapp.season.roamcat.data.runtime.PluginHelper
import gapp.season.roamcat.page.BaseFragment
import gapp.season.util.app.AppUtil
import gapp.season.util.tips.AlertUtil
import kotlinx.android.synthetic.main.fragment_find.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class FindFragment : BaseFragment() {
    var adapter: FindAdapter? = null

    override fun getLayoutId(): Int {
        return R.layout.fragment_find
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!baseActivity!!.supportImmersionBarVersion()) {
            titleLayout.setPadding(0, 0, 0, 0)
        }

        closeFind.setOnClickListener {
            baseActivity!!.finish()
        }
        resetFind.setOnClickListener {
            AlertUtil.list(baseActivity, null, arrayOf(getString(R.string.enable_all_feature),
                    getString(R.string.disable_all_feature), getString(R.string.reset_default)), { _, opt ->
                when (opt) {
                    0 -> {
                        adapter?.data?.forEach {
                            if (it.type != PluginItem.TYPE_BASE) {
                                MmkvUtil.map(MmkvUtil.PLUGIN_OPEN_TAG)?.encode((MmkvUtil.PLUGIN_OPEN_TAG + it.name), true)
                            }
                        }
                        PluginHelper.updateOpenPlugins()
                    }
                    1 -> {
                        adapter?.data?.forEach {
                            if (it.type != PluginItem.TYPE_BASE) {
                                MmkvUtil.map(MmkvUtil.PLUGIN_OPEN_TAG)?.encode((MmkvUtil.PLUGIN_OPEN_TAG + it.name), false)
                            }
                        }
                        PluginHelper.updateOpenPlugins()
                    }
                    2 -> {
                        MmkvUtil.map(MmkvUtil.PLUGIN_OPEN_TAG)?.clearAll()
                        PluginHelper.updateOpenPlugins()
                    }
                }
            }, true)
        }
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = RecyclerView.VERTICAL
        findRecyclerView.layoutManager = layoutManager
        adapter = FindAdapter()
        findRecyclerView.adapter = adapter

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
        val list: MutableList<PluginItem> = mutableListOf()
        PluginHelper.allPlugins.forEach {
            if (it.type == PluginItem.TYPE_BASE || ((it.minSysVersion < Build.VERSION.SDK_INT) &&
                            (it.maxSysVersion > Build.VERSION.SDK_INT) && (BuildConfig.DEV || it.type != PluginItem.TYPE_DEV))) {
                list.add(it)
            }
        }
        adapter?.setNewData(list)
    }

    class FindAdapter : BaseQuickAdapter<PluginItem, BaseViewHolder>(R.layout.item_find_entry) {
        @Suppress("DEPRECATION")
        override fun convert(helper: BaseViewHolder, item: PluginItem?) {
            if (item != null) {
                helper.setImageResource(R.id.itemIcon, item.iconId)
                helper.setText(R.id.itemTitle, item.title)
                helper.setText(R.id.itemStatus, (if (item.isOpen)
                    AppUtil.getString(R.string.is_enabled) else AppUtil.getString(R.string.is_not_enabled)))
                helper.setText(R.id.itemSubTitle, item.message)
                helper.setTextColor(R.id.itemOpenBtn, (if (item.isOpen)
                    AppUtil.getColor(R.color.color_f66) else AppUtil.getColor(R.color.colorAccent)))
                helper.setText(R.id.itemOpenBtn, (if (item.isOpen)
                    AppUtil.getString(R.string.disable_feature) else AppUtil.getString(R.string.enable_feature)))
                helper.setOnClickListener(R.id.itemOpenBtn) {
                    if (item.isOpen) {
                        MmkvUtil.map(MmkvUtil.PLUGIN_OPEN_TAG)?.encode((MmkvUtil.PLUGIN_OPEN_TAG + item.name), false)
                    } else {
                        MmkvUtil.map(MmkvUtil.PLUGIN_OPEN_TAG)?.encode((MmkvUtil.PLUGIN_OPEN_TAG + item.name), true)
                    }
                    PluginHelper.updateOpenPlugins()
                }
                helper.setGone(R.id.itemStatus, item.type != PluginItem.TYPE_BASE)
                helper.setGone(R.id.itemOpenBtn, item.type != PluginItem.TYPE_BASE)
                helper.setGone(R.id.itemDevTag, item.type == PluginItem.TYPE_DEV)
            }
        }
    }
}
