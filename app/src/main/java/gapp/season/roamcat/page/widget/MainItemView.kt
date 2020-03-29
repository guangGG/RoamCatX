package gapp.season.roamcat.page.widget

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import gapp.season.roamcat.R
import kotlinx.android.synthetic.main.item_main_entry.view.*

class MainItemView : FrameLayout {
    companion object {
        const val widthOfDp = 80
    }

    private var mItemView: View? = null
    private var mItemIcon: ImageView? = null
    private var mItemTitle: TextView? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView()

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MainItemView, defStyleAttr, 0)
        val icon = typedArray.getDrawable(R.styleable.MainItemView_itemIcon) as BitmapDrawable?
        val title = typedArray.getString(R.styleable.MainItemView_itemTitle)
        typedArray.recycle()

        updateIcon(icon)
        updateTitle(title)
    }

    @Suppress("DEPRECATION")
    private fun initView() {
        val view = View.inflate(context, R.layout.item_main_entry, null)
        mItemView = view
        mItemIcon = view.itemIcon
        mItemTitle = view.itemTitle
        addView(view)
        setBackgroundColor(resources.getColor(R.color.transparent))
    }

    override fun setOnClickListener(clickListener: OnClickListener?) {
        mItemView?.setOnClickListener(clickListener)
    }

    fun updateIcon(icon: Drawable?) {
        if (icon != null) {
            mItemIcon?.visibility = View.VISIBLE
            mItemIcon?.setImageDrawable(icon)
        } else {
            mItemIcon?.visibility = View.GONE
        }
    }

    fun updateTitle(title: String?) {
        if (!TextUtils.isEmpty(title)) {
            mItemTitle?.visibility = View.VISIBLE
            mItemTitle?.text = title
        } else {
            mItemTitle?.visibility = View.GONE
        }
    }
}