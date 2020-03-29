package gapp.season.calculator

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import gapp.season.util.log.LogUtil
import gapp.season.util.simple.SimpleTextWatcher
import gapp.season.util.sys.ClipboardUtil
import gapp.season.util.tips.ToastUtil
import gapp.season.util.view.ThemeUtil
import kotlinx.android.synthetic.main.calc_activity.*

@SuppressLint("SetTextI18n")
class CalculatorActivity : AppCompatActivity() {
    companion object {
        private const val LIMIT_SERIAL_NUM = 7
    }

    private var calcBufferValue: Double = 0.0
    private var showDebugInfo = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this, 0)
        setContentView(R.layout.calc_activity)
        initView()
        initData()
    }

    private fun initView() {
        calcBack.setOnClickListener { onBackPressed() }
        calc_btn_function_copy.setOnClickListener { calcBtn(it) }
        calc_btn_function_paste.setOnClickListener { calcBtn(it) }
        calc_btn_function_backspace.setOnClickListener { calcBtn(it) }
        calc_btn_function_clear.setOnClickListener { calcBtn(it) }
        calc_btn_function_mc.setOnClickListener { calcBtn(it) }
        calc_btn_function_madd.setOnClickListener { calcBtn(it) }
        calc_btn_function_mminus.setOnClickListener { calcBtn(it) }
        calc_btn_function_mr.setOnClickListener { calcBtn(it) }
        calc_btn_sign_pai.setOnClickListener { calcBtn(it) }
        calc_btn_sign_bracket_left.setOnClickListener { calcBtn(it) }
        calc_btn_sign_bracket_right.setOnClickListener { calcBtn(it) }
        calc_btn_function_switch.setOnClickListener { calcBtn(it) }
        calc_btn_num_7.setOnClickListener { calcBtn(it) }
        calc_btn_num_8.setOnClickListener { calcBtn(it) }
        calc_btn_num_9.setOnClickListener { calcBtn(it) }
        calc_btn_sign_division.setOnClickListener { calcBtn(it) }
        calc_btn_num_4.setOnClickListener { calcBtn(it) }
        calc_btn_num_5.setOnClickListener { calcBtn(it) }
        calc_btn_num_6.setOnClickListener { calcBtn(it) }
        calc_btn_sign_multiply.setOnClickListener { calcBtn(it) }
        calc_btn_num_1.setOnClickListener { calcBtn(it) }
        calc_btn_num_2.setOnClickListener { calcBtn(it) }
        calc_btn_num_3.setOnClickListener { calcBtn(it) }
        calc_btn_sign_minus.setOnClickListener { calcBtn(it) }
        calc_btn_num_0.setOnClickListener { calcBtn(it) }
        calc_btn_sign_point.setOnClickListener { calcBtn(it) }
        calc_btn_sign_equal.setOnClickListener { calcBtn(it) }
        calc_btn_sign_add.setOnClickListener { calcBtn(it) }
    }

    private fun initData() {
        calcExp.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val exp = calcExp.text.toString()
                calcExpResult.text = getRsStr(exp, true)
                // 显示算式
                val fExp: String = try {
                    filterExp(exp)
                } catch (e: Exception) {
                    exp
                }
                calcExpAll.text = "Debug Mode : $fExp = ${getRsStr(exp, false)}"
            }
        })

        // 从配置文件中读取 BufferValue
        val bufStr = CalculatorHelper.getCalcBuffer(this)
        calcBufferValue = try {
            java.lang.Double.valueOf(bufStr)
        } catch (e: Exception) {
            0.0
        }

        refreshBuffer()
        checkSwitchShow()

        calcExpResult.text = "0"
        calcExpAll.text = "0"
        calcExp.text = ""
    }

    private fun calcBtn(btn: View) {
        if (btn is Button) {
            var exp = calcExp.text.toString()
            when (btn.id) {
                R.id.calc_btn_function_backspace -> {
                    if (exp.isNotEmpty()) {
                        exp = exp.substring(0, exp.length - 1)
                    }
                    calcExp.text = exp
                }
                R.id.calc_btn_function_clear -> {
                    calcExp.text = ""
                }
                R.id.calc_btn_sign_equal -> {
                    calcExp.text = getRsStr(exp, true)
                }
                R.id.calc_btn_function_mc -> {
                    calcBufferValue = 0.0
                    refreshBuffer()
                }
                R.id.calc_btn_function_madd -> {
                    calcBufferValue += getRoundDoubleValue(getRs(exp))
                    refreshBuffer()
                }
                R.id.calc_btn_function_mminus -> {
                    calcBufferValue -= getRoundDoubleValue(getRs(exp))
                    refreshBuffer()
                }
                R.id.calc_btn_function_mr -> {
                    calcExp.text = calcMr.text
                }
                R.id.calc_btn_function_copy -> {
                    ClipboardUtil.putText(this, calcExp.text.toString())
                    ToastUtil.showShort("算术表达式已成功复制到剪贴板")
                }
                R.id.calc_btn_function_paste -> {
                    calcExp.text = ClipboardUtil.getText(this)
                }
                R.id.calc_btn_function_switch -> {
                    // 切换调试开关
                    showDebugInfo = !showDebugInfo
                    checkSwitchShow()
                }
                else -> {
                    val signStr = btn.text.toString()
                    val signChar = signStr[0]// 由于这些都是单个字符，直接取出字符
                    if (showDebugInfo) {
                        // 开启检测模式时，对输入的字符进行简单的正确性校验
                        if (TextUtils.isEmpty(exp)) {
                            if (isNumChar(signChar) || signChar == '(' || signChar == 'π' || signChar == '-') {
                                calcExp.text = signStr
                            } else if (signChar == '.') {
                                calcExp.text = "0."
                            }
                        } else {
                            val lastChar = exp[exp.length - 1]
                            if (isNumChar(signChar)) {
                                if ("0" == exp) {
                                    calcExp.text = signStr
                                } else if (lastChar == ')' || lastChar == 'π') {
                                    return
                                } else {
                                    calcExp.text = exp + signStr
                                }
                            } else if (signChar == '.') {
                                if (isNumChar(lastChar)) {
                                    calcExp.text = exp + signStr
                                } else if (lastChar == '(' || isMathSignChar(lastChar)) {
                                    calcExp.text = exp + "0."
                                }
                            } else if (isMathSignChar(signChar)) {
                                if (isNumChar(lastChar) || lastChar == ')' || lastChar == 'π') {
                                    calcExp.text = exp + signStr
                                } else if (signChar == '-' && (lastChar == '+' || lastChar == '×' || lastChar == '÷')) {
                                    // 这里的'-'表示负号，不表示运算符
                                    calcExp.text = exp + signStr
                                }
                            } else if (signChar == '(') {
                                if (lastChar == '(' || isMathSignChar(lastChar)) {
                                    calcExp.text = exp + signStr
                                }
                            } else if (signChar == ')') {
                                if (isNumChar(lastChar) || lastChar == ')' || lastChar == 'π') {
                                    calcExp.text = exp + signStr
                                }
                            } else if (signChar == 'π') {
                                if (lastChar == '(' || isMathSignChar(lastChar)) {
                                    calcExp.text = exp + signStr
                                }
                            } else {
                                calcExp.text = exp + signStr
                            }
                        }
                    } else {
                        calcExp.text = exp + signStr
                    }
                }
            }
        }
    }

    private fun isMathSignChar(c: Char): Boolean {
        return c == '+' || c == '-' || c == '×' || c == '÷'
    }

    private fun isNumChar(c: Char): Boolean {
        return c in '0'..'9'
    }

    private fun checkSwitchShow() {
        if (showDebugInfo) {
            calc_btn_function_switch.text = "●"
            calcExpAll.visibility = View.GONE
        } else {
            calc_btn_function_switch.text = "○"
            calcExpAll.visibility = View.VISIBLE
        }
    }

    private fun refreshBuffer() {
        var bufferStr = calcBufferValue.toString()
        if (bufferStr.endsWith(".0"))
            bufferStr = bufferStr.substring(0, bufferStr.indexOf(".0"))
        calcMr.text = bufferStr
        // 保存到配置文件中
        CalculatorHelper.saveCalcBuffer(this, bufferStr)
    }

    /***
     * @param expression     算数表达式
     * @param isRound 是否对结果精确度进行修正
     * @return 根据表达式返回结果
     */
    private fun getRsStr(expression: String, isRound: Boolean): String {
        var exp = expression
        if (TextUtils.isEmpty(exp)) {
            return "0"
        }
        val result: Number
        try {
            exp = filterExp(exp)
            result = CalculatorHelper.eval(exp) as Number
            exp = if (isRound) {
                getRoundDoubleValue(result).toString() + ""
            } else {
                result.toDouble().toString() + ""
            }
        } catch (e: Exception) {
            return "计算出错"
        }
        if (exp.endsWith(".0"))
            exp = exp.substring(0, exp.indexOf(".0"))
        return exp
    }

    /***
     * @param expression 算数表达式
     * @return 根据表达式返回结果
     */
    private fun getRs(expression: String): Number {
        var exp = expression
        if (TextUtils.isEmpty(exp)) {
            return 0
        }
        var result: Number
        try {
            exp = filterExp(exp)
            result = CalculatorHelper.eval(exp) as Number
        } catch (e: Exception) {
            result = 0
        }
        return result
    }

    private fun getRoundDoubleValue(number: Number): Double {
        val num = number.toDouble()
        try {
            // 解决精确度问题，如：2.3-1=1.2999999999998;2.2-1=1.2000000000000002
            var numStr = num.toString()
            // 若小数点后位数较多，将最后一位四舍五入
            val pointIndex = numStr.lastIndexOf(".")
            if (pointIndex >= 0 && numStr.length - pointIndex - 1 > LIMIT_SERIAL_NUM) {
                val lastNum = numStr[numStr.length - 1]
                if (lastNum < '5') {
                    // 四舍
                    numStr = numStr.substring(0, numStr.length - 1)
                } else {
                    // 五入
                    var sb = StringBuilder(numStr)
                    sb.deleteCharAt(sb.length - 1)
                    while (sb.isNotEmpty()) {
                        var c = sb[sb.length - 1]
                        if (c in '0'..'8') {
                            c = (c.toInt() + 1).toChar()
                            sb.setCharAt(sb.length - 1, c)
                            break
                        } else if (c == '9') {
                            sb.deleteCharAt(sb.length - 1)
                        } else if (c == '.') {
                            val intNum = sb.substring(0, sb.length - 1)
                            val isPositive = sb[0] != '-'
                            var n = Integer.valueOf(intNum)
                            n = if (isPositive) n + 1 else n - 1
                            sb = StringBuilder()
                            sb.append(n)
                            break
                        } else {
                            // 不存在这个情况
                            break
                        }
                    }
                    numStr = sb.toString()
                }
            }
            val newNum = java.lang.Double.valueOf(numStr)
            if (num != newNum) {
                LogUtil.d("CalculatorActivity", "对数值进行了精度转换：把“$num”转成“$newNum”")
            }
            return newNum
        } catch (e: Exception) {
            e.printStackTrace()
            return num
        }
    }

    /**
     * 因为计算过程中,全程需要有小数参与,所以需要过滤一下
     *
     * @param expression 算数表达式
     * @return 格式化后的算术表达式
     */
    private fun filterExp(expression: String): String {
        var exp = expression
        exp = exp.replace("π", "3.14159265359")
        exp = exp.replace('×', '*')
        exp = exp.replace('÷', '/')

        val num = exp.split("".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()// 取出所有的字符
        var temp: String
        var begin = 0
        var end: Int
        for (i in 1 until num.size) {
            temp = num[i]
            if (temp.matches("[+-/()*]".toRegex())) {// 匹配+-*/().几个字符[备注：这个正则表达式是可以匹配到.的]
                // 跳过匹配到的.字符
                if (temp == ".") {
                    continue
                }
                // 判断两个相邻符号之间的数字为整数时，后面加上".0"
                end = i - 1
                temp = exp.substring(begin, end)
                if (temp.trim { it <= ' ' }.isNotEmpty() && temp.indexOf(".") < 0) {
                    num[i - 1] = num[i - 1] + ".0"
                }
                begin = end + 1
            }
        }
        // 将修改后的所有字符的数组再组合起来
        return num.contentToString().replace("[\\[\\], ]".toRegex(), "")
    }
}
