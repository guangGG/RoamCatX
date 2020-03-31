package gapp.season.filemanager

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import gapp.season.util.tips.ToastUtil
import java.io.File
import java.util.*

class FileManagerStack {
    private val diskStack = SafeStack<FileManagerFragment>()
    private var activity: FragmentActivity? = null
    private var containerId: Int = 0
    var listener: OnFmStackListener? = null

    constructor(activity: FragmentActivity?, containerId: Int) {
        this.activity = activity
        this.containerId = containerId
    }

    /**
     * 获取当前顶层fragment
     */
    fun getTopFragment(): FileManagerFragment? {
        return diskStack.peek()
    }

    /**
     * 打开指定目录，并定位到指定文件
     */
    fun push(dirPath: String?, fileName: String?) {
        if (dirPath.isNullOrBlank()) {
            ToastUtil.showShort("打开目录失败，目录不存在")
            return
        }
        if (FileManager.DISKS_DIR_TAG != dirPath) {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) {
                ToastUtil.showShort("打开目录失败，目录不存在")
                return
            }
        }
        val basementFragment = getTopFragment() //当前顶层fragment
        val fragment = FileManagerFragment()
        val args = Bundle()
        args.putString("dirPath", dirPath)
        args.putString("fileName", fileName)
        fragment.arguments = args
        diskStack.push(fragment) //推送新的顶层fragment
        val ft = activity?.supportFragmentManager?.beginTransaction()
        // 设置进入动画
        ft?.setCustomAnimations(R.anim.fm_anim_right_in, R.anim.fm_anim_left_out,
                R.anim.fm_anim_left_in, R.anim.fm_anim_right_out)
        ft?.add(containerId, fragment, "tag://" + System.currentTimeMillis())
        ft?.show(fragment)
        if (basementFragment != null) {
            ft?.hide(basementFragment)
            basementFragment.stackPause()
        }
        //commit可能报错:IllegalStateException: Can not perform this action after onSaveInstanceState
        ft?.commitAllowingStateLoss()
        listener?.onStackChange(diskStack, true)
    }

    /**
     * 关闭顶层fragment，返回Stack是否为Empty
     */
    fun pop(): Boolean {
        if (diskStack.size > 1) {
            val fragment = diskStack.pop() //移除顶层fragment
            val basementFragment = diskStack.peek() //获取到次顶层fragment
            if (fragment != null) {
                val ft = activity?.supportFragmentManager?.beginTransaction()
                // 设置退出动画
                ft?.setCustomAnimations(R.anim.fm_anim_left_in, R.anim.fm_anim_right_out,
                        R.anim.fm_anim_right_in, R.anim.fm_anim_left_out)
                ft?.remove(fragment)
                return if (basementFragment != null) {
                    ft?.show(basementFragment)
                    basementFragment.stackResume()
                    ft?.commitAllowingStateLoss()
                    listener?.onStackChange(diskStack, false)
                    false
                } else {
                    true //正常不会出现到这里
                }
            } else {
                return true //正常不会出现到这里
            }
        } else {
            return true
        }
    }


    interface OnFmStackListener {
        fun onStackChange(diskStack: SafeStack<FileManagerFragment>, push: Boolean)
    }

    /**
     * 把Stack抛异常的情况返回为null
     */
    class SafeStack<E> : Stack<E>() {
        @Synchronized
        override fun pop(): E? {
            return try {
                super.pop()
            } catch (e: Exception) {
                null
            }
        }

        @Synchronized
        override fun peek(): E? {
            return try {
                super.peek()
            } catch (e: Exception) {
                null
            }
        }
    }
}
