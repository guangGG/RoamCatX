package gapp.season.roamcat.page.main

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import java.util.*

@Suppress("DEPRECATION")
@SuppressLint("UseSparseArrays")
class MainPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
    companion object {
        const val TAB_HOME = 0
        const val TAB_HOT_ACTION = 1

        const val TAB_COUNT = 2
    }

    private val fragmentMap = HashMap<Int, Fragment>()

    override fun getItem(position: Int): Fragment {
        val fragment: Fragment?
        when (position) {
            TAB_HOME -> fragment = MainFragment()
            TAB_HOT_ACTION -> fragment = HotActionFragment()
            else -> fragment = Fragment()
        }
        fragmentMap[position] = fragment
        return fragment

    }

    override fun getCount(): Int {
        return TAB_COUNT
    }

    fun getFragment(tabId: Int): Fragment? {
        return fragmentMap.get(tabId)
    }
}
