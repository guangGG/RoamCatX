@file:Suppress("DEPRECATION")

package gapp.season.sudoku

import android.app.ProgressDialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import gapp.season.util.sys.ClipboardUtil
import gapp.season.util.task.OnTaskValue
import gapp.season.util.tips.ToastUtil
import gapp.season.util.view.ThemeUtil
import kotlinx.android.synthetic.main.sudoku_activity.*
import java.util.*

class SudokuActivity : AppCompatActivity() {
    companion object {
        private const val DEFAULT_STR = "000000000000000000000000000000000000000000000000000000000000000000000000000000000"
    }

    private var selectDialog: AlertDialog? = null
    private var inputDialog: SudokuInputDialog? = null
    private var progressDialog: ProgressDialog? = null

    private var sudokuStr: String? = DEFAULT_STR //当前处理中的数独原始数据
    private val sudokuNums: Array<Array<SudokuCell>> = Array(9) { Array(9) { SudokuCell() } }

    private var calculateLevel = 3// 运算等级(对应运算时的算法强度,取1-3)
    private var calculateCounts: Int = 0// 运算次数

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtil.setTheme(this, 0)
        setContentView(R.layout.sudoku_activity)
        initView()
        showSelectDialog()
    }

    private fun initView() {
        sudokuBack.setOnClickListener { finish() }
        sudokuCopy.setOnClickListener { copySudokuStr() }
        sudokuMenu.setOnClickListener { showSelectDialog() }
        sudokuCalculateLevel.setOnClickListener {
            //设置运算等级
            calculateLevel++
            if (calculateLevel > 3) calculateLevel = 1
            showTipInfo()
        }
        sudokuCalculateOnce.setOnClickListener {
            goToCalculate() //单次运算：运行一次全循环
        }
        sudokuCalculateOperation.setOnClickListener {
            operation() //全部运算(耗时)
        }
        sudokuCalculateRestore.setOnClickListener {
            initData() //还原当局
        }

        sudokuView.setCellClickListener { _, cellIndex, longClick ->
            val row = cellIndex / sudokuNums.size
            val col = cellIndex % sudokuNums.size
            val item = sudokuNums[row][col]
            if (longClick) {
                // 手动设置单元格
                if (!item.isOriginal) {
                    val numStrs = ArrayList<Int>()
                    if (item.isSingleNum()) {
                        for (i in 1..9) {
                            numStrs.add(i)
                        }
                    } else {
                        numStrs.addAll(item.getPossibleValues())
                        numStrs.sort()
                    }
                    val nums = arrayOfNulls<String>(numStrs.size)
                    for (i in nums.indices) {
                        nums[i] = numStrs[i].toString()
                    }
                    val builder = AlertDialog.Builder(this)
                    builder.setItems(nums) { _, which ->
                        // 手动填写单元格
                        item.hintNumbers = null
                        item.number = (Integer.parseInt(nums[which]!!))
                        updateUI() //刷新UI
                    }
                    builder.show()
                }
            } else {
                // 运算单元格
                if (!item.isOriginal) {
                    try {
                        CalculateCell(row, col).invoke()
                    } catch (e: SudokuCell.SudokuCellEmptyException) {
                        e.printStackTrace()
                        ToastUtil.showLong("运算出错：" + e.message)
                    }
                    updateUI() //刷新UI
                }
            }
            return@setCellClickListener true
        }
    }

    private fun updateUI() {
        val list = mutableListOf<SudokuCell>()
        for (i in 0..80) {
            val row = i / sudokuNums.size
            val col = i % sudokuNums.size
            val cell = sudokuNums[row][col]
            list.add(cell)
        }
        sudokuView.setCells(list)
    }

    private fun initData() {
        initData(true)
    }

    private fun initData(isChangeUI: Boolean) {
        if (sudokuStr != null && sudokuStr!!.length == sudokuNums.size * sudokuNums.size) {
            for (i in 0 until sudokuStr!!.length) {
                val num: Int = try {
                    Integer.valueOf(sudokuStr!![i].toString())
                } catch (e: Exception) {
                    0
                }

                val row = i / sudokuNums.size
                val col = i % sudokuNums.size
                val cell = SudokuCell()
                cell.isOriginal = num != 0
                cell.number = num
                sudokuNums[row][col] = cell
            }
            calculateCounts = 0

            if (isChangeUI) {
                updateUI()
                showTipInfo()

                //判断初始数独字符串是否合法
                if (!isSudokuLegal()) {
                    ToastUtil.showLong("字符串不符合数独规则：$sudokuStr")
                }
            }
        } else {
            // 数独字符串不正确
            if (isChangeUI)
                ToastUtil.showLong("数独字符串格式错误：$sudokuStr")
        }
    }

    private fun isSudokuLegal(): Boolean {
        for (i in 0..8) {
            for (j in 0..8) {
                val cell = sudokuNums[i][j]
                if (cell.isSingleNum()) {
                    for (k in 0..8) {
                        // 行
                        if (k != j) {
                            if (sudokuNums[i][k].isSingleNum()) {
                                if (cell.number == sudokuNums[i][k].number) {
                                    return false
                                }
                            }
                        }
                        // 列
                        if (k != i) {
                            if (sudokuNums[k][j].isSingleNum()) {
                                if (cell.number == sudokuNums[k][j].number) {
                                    return false
                                }
                            }
                        }
                        // 宫
                        val rowP = i / 3
                        val colP = j / 3
                        val k1 = k / 3
                        val k2 = k % 3
                        val cellOther = sudokuNums[rowP * 3 + k1][colP * 3 + k2]
                        if (cellOther != cell) {
                            if (cellOther.isSingleNum()) {
                                if (cell.number == cellOther.number) {
                                    return false
                                }
                            }
                        }
                    }
                }
            }
        }
        return true
    }

    private fun showSelectDialog() {
        if (selectDialog == null) {
            val builder = AlertDialog.Builder(this)
            builder.setItems(Sudokus.getInstance().sudokuTipArray) { _, which ->
                val keys = Sudokus.getInstance().sudokuKeys
                when (val key = keys[which]) {
                    "copy" -> {
                        copySudokuStr()
                    }
                    "paste" -> {
                        val cs = ClipboardUtil.getText(this)
                        sudokuStr = cs?.toString()
                        sudokuTitle.text = Sudokus.getInstance().sudokuTips[which]
                        initData()
                    }
                    "input" -> {
                        sudokuTitle.text = Sudokus.getInstance().sudokuTips[which]
                        showInputDialog()
                    }
                    "generate" -> {
                        sudokuTitle.text = Sudokus.getInstance().sudokuTips[which]
                        showLoading()
                        // 首先生成随机的全数独
                        sudokuStr = DEFAULT_STR
                        operation(true, OnTaskValue { b ->
                            if (b) {
                                // 取出数独字符
                                val strAll = getCurrentStr()
                                // 随后随机选择显示的数字
                                val num = SudokuHelper.getRandomNum(20, 30)
                                val indexShow = SudokuHelper.getRandomPosition(81, num)
                                val sb = StringBuilder(DEFAULT_STR)
                                for (i in indexShow.indices) {
                                    sb.deleteCharAt(indexShow[i])
                                    sb.insert(indexShow[i], strAll[indexShow[i]])
                                }
                                sudokuStr = sb.toString()
                            }
                            initData()
                            hideLoading()
                        })
                    }
                    else -> showSelectDetailDialog(key)
                }
            }
            selectDialog = builder.create()
        }
        selectDialog!!.show()
    }

    private fun copySudokuStr() {
        val str = getCurrentStr()
        ClipboardUtil.putText(this, str)
        ToastUtil.showLong("复制数独字符串到粘贴板成功：$str")
    }

    private fun showLoading() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
            progressDialog!!.setMessage("处理中……")
            progressDialog!!.setCancelable(false)
        }
        progressDialog?.show()
    }

    private fun hideLoading() {
        progressDialog?.dismiss()
    }

    private fun getCurrentStr(): String {
        // 复制当前数独内容
        val sb = StringBuilder()
        for (i in 0..8) {
            for (j in 0..8) {
                val cell = sudokuNums[i][j]
                if (cell.isSingleNum()) {
                    sb.append(cell.number)
                } else {
                    sb.append(0)
                }
            }
        }
        return sb.toString()
    }

    private fun showInputDialog() {
        if (inputDialog == null) {
            inputDialog = SudokuInputDialog(this)
            inputDialog!!.setCallBack(object : SudokuInputDialog.CallBack {
                override fun onInputChange(str: String) {
                    sudokuStr = str
                    initData()
                }
            })
        }
        inputDialog!!.show()
    }

    private fun showSelectDetailDialog(key: String) {
        AlertDialog.Builder(this)
                .setItems(Sudokus.getInstance().getSudokusTipArray(key)) { _, which ->
                    val sudokus = Sudokus.getInstance().getSudokusValueList(key)
                    sudokuStr = sudokus[which]
                    sudokuTitle.text = Sudokus.getInstance().getSudokusTipList(key)[which]
                    initData()
                }.show()
    }

    private fun goToCalculate() {
        try {
            var isModify = false
            for (i in 0..8) {
                for (j in 0..8) {
                    val cCell = CalculateCell(i, j).invoke()
                    isModify = cCell.isModify || isModify
                    //if (cCell.is()) continue;
                }
            }

            if (isModify) {
                calculateCounts++
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtil.showLong("运算出错：" + e.message)
        }

        // 刷新UI
        updateUI()
        showTipInfo()
    }

    private var lastOperationUITimeStamp: Long = 0//记录最近一次刷新UI的时间(优化刷新)
    private fun operation() {
        operation(false, null)
    }

    private fun operation(isSilent: Boolean, listener: OnTaskValue<Boolean>?) {
        if (!isSilent)
            showLoading()
        initData()
        SudokuHelper.executorService?.execute(Runnable {
            val maxTimes = 20000
            var times = 0
            for (t in 0 until maxTimes) {
                times++
                try {
                    var isModify = false
                    for (i in 0..8) {
                        for (j in 0..8) {
                            val cCell = CalculateCell(i, j).invoke()
                            isModify = cCell.isModify || isModify
                        }
                    }

                    if (!isModify) {
                        if (isAllSingle()) {
                            val finalTimes = times
                            sudokuView.post {
                                updateUI()
                                if (!isSilent) {
                                    hideLoading()
                                    ToastUtil.showLong("运算成功，循环次数：" + (finalTimes - 1))
                                }
                                listener?.onTaskDone(true) //(finalTimes - 1)
                            }
                            return@Runnable
                        } else if (t < maxTimes - 1) {
                            // 随机处理非single单元格
                            val noSingleCells = ArrayList<SudokuCell>()
                            for (i in 0..8) {
                                for (j in 0..8) {
                                    val cell = sudokuNums[i][j]
                                    if (!cell.isSingleNum()) {
                                        noSingleCells.add(cell)
                                    }
                                }
                            }
                            noSingleCells.sortWith(Comparator { lhs, rhs ->
                                lhs.count() - rhs.count()
                            })
                            val cell = noSingleCells[0] //取数字最少的第一个格子做随机运算
                            val mutableList = mutableListOf<Int>()
                            mutableList.addAll(cell.getPossibleValues())
                            if (!mutableList.isNullOrEmpty()) {
                                val index = (Math.random() * mutableList.size).toInt()
                                cell.setSingleNum(mutableList[index])
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    initData(false)
                }

                // 刷新UI
                sudokuView.post {
                    // 0.1秒刷新一次UI
                    if (System.currentTimeMillis() - lastOperationUITimeStamp >= 100) {
                        updateUI()
                        lastOperationUITimeStamp = System.currentTimeMillis()
                    }
                }
            }
            // 刷新UI
            val finalTimes1 = times
            sudokuView.post {
                updateUI()
                if (!isSilent) {
                    hideLoading()
                    ToastUtil.showLong("运算未成功，循环次数：$finalTimes1")
                }
                listener?.onTaskDone(false) //finalTimes1
            }
        })
    }

    private fun isAllSingle(): Boolean {
        for (i in 0..8) {
            for (j in 0..8) {
                val cell = sudokuNums[i][j]
                if (!cell.isSingleNum()) {
                    return false
                }
            }
        }
        return true
    }

    private fun showTipInfo() {
        sudokuCalculateLevel.text = String.format("运算等级：%d", calculateLevel)
        sudokuCalculateInfo.text = String.format("共进行了%d次运算", calculateCounts)
    }

    private inner class CalculateCell(private val i: Int, private val j: Int) {
        private var myResult: Boolean = false
        var isModify: Boolean = false
            private set

        @Throws(SudokuCell.SudokuCellEmptyException::class)
        operator fun invoke(): CalculateCell {
            val cell = sudokuNums[i][j]
            if (cell.isOriginal) {
                myResult = true
                return this
            }

            // 第一步，区块(行列宫)排除
            for (k in 0..8) {
                // 行
                if (k != j) {
                    if (sudokuNums[i][k].isSingleNum()) {
                        if (cell.removeHintValue(sudokuNums[i][k].number)) {
                            isModify = true
                        }
                    }
                }
                // 列
                if (k != i) {
                    if (sudokuNums[k][j].isSingleNum()) {
                        if (cell.removeHintValue(sudokuNums[k][j].number)) {
                            isModify = true
                        }
                    }
                }
                // 宫
                val rowP = i / 3
                val colP = j / 3
                val k1 = k / 3
                val k2 = k % 3
                val cellOther = sudokuNums[rowP * 3 + k1][colP * 3 + k2]
                if (cellOther != cell) {
                    if (cellOther.isSingleNum()) {
                        if (cell.removeHintValue(cellOther.number)) {
                            isModify = true
                        }
                    }
                }
            }

            if (calculateLevel < 2) {
                myResult = true
                return this
            }
            // 第二步，区块唯一性校验，区块内其他8个位置都不含一个数，则此位置就是这个数
            for (numS in 1..9) {
                var isOnlyNum = true
                // 行
                for (k in 0..8) {
                    if (k != j) {
                        if (sudokuNums[i][k].isContainsNum(numS)) {
                            isOnlyNum = false
                            break
                        }
                    }
                }
                if (!isOnlyNum) {
                    isOnlyNum = true
                    // 列
                    for (k in 0..8) {
                        if (k != i) {
                            if (sudokuNums[k][j].isContainsNum(numS)) {
                                isOnlyNum = false
                                break
                            }
                        }
                    }
                }
                if (!isOnlyNum) {
                    isOnlyNum = true
                    // 宫
                    for (k in 0..8) {
                        val rowP = i / 3
                        val colP = j / 3
                        val k1 = k / 3
                        val k2 = k % 3
                        val cellOther = sudokuNums[rowP * 3 + k1][colP * 3 + k2]
                        if (cellOther != cell) {
                            if (cellOther.isContainsNum(numS)) {
                                isOnlyNum = false
                                break
                            }
                        }
                    }
                }
                // 确定唯一的数字
                if (isOnlyNum) {
                    if (cell.setSingleNum(numS)) {
                        isModify = true
                        break
                    }
                }
            }


            if (calculateLevel < 3) {
                myResult = true
                return this
            }
            // 第三步，成对排除(例如，区块内若有两个位置都是49，则这两个位置为一对，可排除区块内其他位置的49。[同理，若3个位置都是236，则排除其他位置的236])
            // 行
            val rowList = ArrayList<SudokuCell>()
            for (k in 0..8) {
                if (k != j) {
                    if (sudokuNums[i][k].count() == 2)
                        rowList.add(sudokuNums[i][k])
                }
            }
            if (exclude(cell, rowList)) {
                isModify = true
            }
            // 列
            val colList = ArrayList<SudokuCell>()
            for (k in 0..8) {
                if (k != i) {
                    if (sudokuNums[k][j].count() == 2)
                        colList.add(sudokuNums[k][j])
                }
            }
            if (exclude(cell, colList)) {
                isModify = true
            }
            // 宫
            val blockList = ArrayList<SudokuCell>()
            for (k in 0..8) {
                val rowP = i / 3
                val colP = j / 3
                val k1 = k / 3
                val k2 = k % 3
                val cellOther = sudokuNums[rowP * 3 + k1][colP * 3 + k2]
                if (cellOther != cell && cellOther.count() == 2) {
                    blockList.add(cellOther)
                }
            }
            if (exclude(cell, blockList)) {
                isModify = true
            }
            myResult = false
            return this
        }
    }

    @Throws(SudokuCell.SudokuCellEmptyException::class)
    private fun exclude(cell: SudokuCell, blockList: ArrayList<SudokuCell>): Boolean {
        var isExclude = false
        if (blockList.size > 1) {
            for (i in blockList.indices) {
                for (j in i + 1 until blockList.size) {
                    val iSet = blockList[i].getPossibleValues()
                    val jSet = blockList[j].getPossibleValues()
                    if (iSet.containsAll(jSet)) {
                        // 两个一样的单元格,同区块的其他单元格可移除对应数字
                        for (num in iSet) {
                            if (cell.removeHintValue(num)) {
                                isExclude = true
                            }
                        }
                        break
                    }
                }
            }
        }
        return isExclude
    }
}
