package gapp.season.sudoku

import android.app.Activity
import android.content.Context
import android.content.Intent
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.min

object SudokuHelper {
    fun openSudoku(context: Context) {
        val intent = Intent(context, SudokuActivity::class.java)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    var executorService: ExecutorService? = null
        get() {
            if (field == null) field = Executors.newCachedThreadPool()
            return field
        }

    /**
     * 从totalNum个数中，随机选出selectNum个数
     *
     * @return 选出数的index
     */
    fun getRandomPosition(totalNum: Int, selectNum: Int): IntArray {
        if (selectNum in 1 until totalNum) {
            val list = LinkedList<Int>()
            for (i in 0 until totalNum) {
                list.add(i)
            }
            val random = Random()
            val res = IntArray(selectNum)
            for (i in 0 until selectNum) {
                val index = random.nextInt(list.size)
                res[i] = list.removeAt(index)
            }
            return res
        } else {
            throw IllegalArgumentException("Argument Error")
        }
    }

    /**
     * 获取随机数(包含最大和最小值)：[minNum,maxNum]
     */
    fun getRandomNum(minNum: Int, maxNum: Int): Int {
        val count = abs(maxNum - minNum) + 1
        val ranNum = (Math.random() * count).toInt()
        return min(minNum, maxNum) + ranNum
    }
}
