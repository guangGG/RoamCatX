package gapp.season.roamcat.page.find

import android.os.Bundle
import com.gyf.immersionbar.ktx.immersionBar
import gapp.season.roamcat.R
import gapp.season.roamcat.page.BaseActivity

class FindActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_frame_container)
        supportFragmentManager.beginTransaction().replace(R.id.frameContainer, FindFragment()).commit()
    }

    override fun customImmersionBar(): Boolean {
        //在部分低版本手机上显示会有问题
        if (supportImmersionBarVersion()) {
            immersionBar {
                fitsSystemWindows(false)
                statusBarColor(R.color.transparent)
                statusBarDarkFont(false)
                navigationBarColor(R.color.white)
                navigationBarDarkIcon(true)
            }
        }
        return true
    }
}
