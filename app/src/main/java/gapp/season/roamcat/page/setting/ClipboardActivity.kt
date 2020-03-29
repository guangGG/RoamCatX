package gapp.season.roamcat.page.setting

import android.content.ClipboardManager
import android.os.Bundle
import gapp.season.roamcat.R
import gapp.season.roamcat.page.BaseActivity
import gapp.season.util.sys.ClipboardUtil
import kotlinx.android.synthetic.main.activity_clipboard.*

class ClipboardActivity : BaseActivity() {
    private var clipChangedListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_clipboard)
        setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.item_clipboard)
        updateUI()

        clipChangedListener = ClipboardManager.OnPrimaryClipChangedListener {
            updateUI()
        }
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).addPrimaryClipChangedListener(clipChangedListener)
    }

    override fun onDestroy() {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).removePrimaryClipChangedListener(clipChangedListener)
        super.onDestroy()
    }

    private fun updateUI() {
        infoView.text = ClipboardUtil.getText(this)
    }
}
