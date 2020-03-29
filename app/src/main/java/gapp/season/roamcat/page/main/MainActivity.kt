package gapp.season.roamcat.page.main

import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager.widget.ViewPager
import com.didichuxing.doraemonkit.DoraemonKit
import com.gyf.immersionbar.ktx.immersionBar
import gapp.season.roamcat.R
import gapp.season.roamcat.page.BaseActivity
import gapp.season.roamcat.page.find.FindActivity
import gapp.season.roamcat.page.setting.AboutActivity
import gapp.season.roamcat.page.setting.SettingActivity
import gapp.season.util.app.AppUtil
import gapp.season.util.tips.ToastUtil
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity() {
    private var drawerSubTitle: TextView? = null
    private var drawerToggle: ActionBarDrawerToggle? = null
    private var pagerAdapter: MainPagerAdapter? = null
    private var backTime = 0L
    var currentTab = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setDisplayHomeAsUpEnabled(false)
        setTitle(R.string.app_name)
        if (!supportImmersionBarVersion()) {
            appBarLayout.setPadding(0, 0, 0, 0)
        }

        //抽屉菜单设置
        drawerToggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name)
        drawerToggle!!.isDrawerIndicatorEnabled = false
        //densityDpi为实际屏幕密度，density = densityDpi * 图片需显示大小与图片实际大小的比值
        val densityDpi = Resources.getSystem().displayMetrics.densityDpi
        val menuDrawable = resources.getDrawableForDensity(R.drawable.icon_instruction, (densityDpi * 0.5).toInt())
        val homeAsUpIndicator = DrawableCompat.wrap(menuDrawable!!)
        DrawableCompat.setTint(homeAsUpIndicator, Color.BLACK) //给图标着色(同：ImageView.setColorFilter(int))
        drawerToggle!!.setHomeAsUpIndicator(homeAsUpIndicator)
        drawerToggle!!.setToolbarNavigationClickListener {
            //设置点击toolbar左侧按钮的事件
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        drawerToggle!!.syncState()
        drawerLayout.addDrawerListener(drawerToggle!!)
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                //打开菜单时，更新菜单的UI
                drawerSubTitle?.setText(R.string.drawer_subtitle)
                DoraemonKit.show()
            }

            override fun onDrawerClosed(drawerView: View) {
                DoraemonKit.hide()
            }
        })
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED) //抽屉默认允许侧拉滑动
        val menuHeader = startDrawer.getHeaderView(0)
        drawerSubTitle = menuHeader.findViewById<TextView>(R.id.drawerSubTitle)
        drawerSubTitle?.setText(R.string.drawer_subtitle)
        startDrawer.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_setting -> {
                    startActivity(Intent(this, SettingActivity::class.java))
                }
                R.id.navigation_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                }
            }
            return@setNavigationItemSelectedListener true
        }

        //ViewPager设置
        viewPager.noScroll = false //设置为false表示允许滑动切换
        viewPager.offscreenPageLimit = MainPagerAdapter.TAB_COUNT
        pagerAdapter = MainPagerAdapter(supportFragmentManager)
        viewPager.adapter = pagerAdapter
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                currentTab = position
                when (position) {
                    MainPagerAdapter.TAB_HOME -> {
                        appBarLayout.visibility = View.VISIBLE
                        if (supportImmersionBarVersion()) {
                            immersionBar {
                                statusBarDarkFont(true)
                            }
                        }
                        //抽屉允许侧拉滑动
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    }
                    MainPagerAdapter.TAB_HOT_ACTION -> {
                        appBarLayout.visibility = View.GONE
                        if (supportImmersionBarVersion()) {
                            immersionBar {
                                statusBarDarkFont(false)
                            }
                        }
                        //抽屉保持关闭状态并禁用手势滑出
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    }
                }
            }
        })
        navigation.enableAnimation(false)
        navigation.enableShiftingMode(false)
        navigation.enableItemShiftingMode(false)
        navigation.setupWithViewPager(viewPager)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.navigation_main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigation_find -> {
                startActivity(Intent(this, FindActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return
        }
        if (System.currentTimeMillis() - backTime > 2000) {
            backTime = System.currentTimeMillis()
            ToastUtil.showLong(R.string.click_again_exit)
            return
        }
        AppUtil.closeApp(300)
    }

    override fun customImmersionBar(): Boolean {
        //在部分低版本手机上显示会有问题
        if (supportImmersionBarVersion()) {
            immersionBar {
                fitsSystemWindows(false)
                statusBarColor(R.color.transparent)
                statusBarDarkFont(true)
                navigationBarColor(R.color.white)
                navigationBarDarkIcon(true)
            }
        }
        return true
    }
}
