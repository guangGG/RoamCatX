package gapp.season.sudoku

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.Gravity
import gapp.season.util.view.ThemeUtil
import kotlinx.android.synthetic.main.sudoku_input_dialog.*

class SudokuInputDialog(context: Context) : Dialog(context) {
    private var listener: CallBack? = null
    private var text: String = ""

    init {
        ThemeUtil.setTheme(context as Activity, 0)
        setContentView(R.layout.sudoku_input_dialog)
        initView()
        refreshUI()
        setCanceledOnTouchOutside(false)
        if (window != null) {
            window!!.setGravity(Gravity.BOTTOM)
        }
    }

    private fun initView() {
        sudokuInputNum0.setOnClickListener { addNum(0) }
        sudokuInputNum1.setOnClickListener { addNum(1) }
        sudokuInputNum2.setOnClickListener { addNum(2) }
        sudokuInputNum3.setOnClickListener { addNum(3) }
        sudokuInputNum4.setOnClickListener { addNum(4) }
        sudokuInputNum5.setOnClickListener { addNum(5) }
        sudokuInputNum6.setOnClickListener { addNum(6) }
        sudokuInputNum7.setOnClickListener { addNum(7) }
        sudokuInputNum8.setOnClickListener { addNum(8) }
        sudokuInputNum9.setOnClickListener { addNum(9) }

        sudokuInputClose.setOnClickListener { dismiss() }
        sudokuInputClear.setOnClickListener {
            text = ""
            refreshUI()
        }
        sudokuInputBackspace.setOnClickListener {
            if (text.isNotEmpty()) {
                text = text.substring(0, text.length - 1)
            }
            refreshUI()
        }
        sudokuInputOk.setOnClickListener {
            refreshUI()
            dismiss()
        }
    }

    private fun addNum(num: Int) {
        if (text.length < 81) {
            text += num.toString()
        }
        refreshUI()
    }

    private fun refreshUI() {
        sudokuInputText!!.text = text
        if (listener != null) {
            var str = StringBuilder(text)
            if (str.length > 81) {
                str = StringBuilder(str.substring(0, 81))
            }
            while (str.length < 81) {
                str.append("0")
            }
            listener!!.onInputChange(str.toString())
        }
    }

    override fun show() {
        super.show()
        refreshUI()
    }

    fun setCallBack(callBack: CallBack): SudokuInputDialog {
        listener = callBack
        return this
    }

    interface CallBack {
        fun onInputChange(str: String)
    }
}
