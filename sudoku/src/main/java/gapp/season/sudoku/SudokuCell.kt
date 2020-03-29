package gapp.season.sudoku

class SudokuCell {
    var isOriginal = false
    var number = 0
    var hintNumbers: MutableList<Int>? = null

    fun isEmpty(): Boolean {
        if (number > 0) return false
        if (hintNumbers?.size ?: 0 > 0) return false
        return true
    }

    /**
     * 是否确定了的数字
     */
    fun isSingleNum(): Boolean {
        return (number > 0)
    }

    /**
     * 是否包含某个数
     */
    fun isContainsNum(num: Int): Boolean {
        if (isSingleNum()) {
            return number == num
        } else {
            if (hintNumbers.isNullOrEmpty()) return true
            return hintNumbers!!.contains(num)
        }
    }

    fun count(): Int {
        if (isSingleNum()) return 1
        if (hintNumbers.isNullOrEmpty()) return 9
        return hintNumbers!!.size
    }

    fun setSingleNum(num: Int): Boolean {
        if (num in 1..9) {
            if (isSingleNum()) {
                if (number == num) {
                    return false
                } else {
                    number = num
                    return true
                }
            } else {
                number = num
                hintNumbers = null
                return true
            }
        }
        return false
    }

    fun removeHintValue(number: Int): Boolean {
        if (isSingleNum()) {
            if (this.number == number) {
                throw SudokuCellEmptyException()
            }
            return false
        } else {
            if (hintNumbers.isNullOrEmpty()) {
                if (hintNumbers == null) hintNumbers = mutableListOf()
                for (i in 1..9) {
                    if (number != i) hintNumbers!!.add(i)
                }
                return true
            } else {
                val b = hintNumbers!!.remove(number)
                if (hintNumbers!!.size == 0) {
                    throw SudokuCellEmptyException()
                } else {
                    if (hintNumbers!!.size == 1) {
                        this.number = hintNumbers!![0]
                        hintNumbers = null
                    }
                    return b
                }
            }
        }
    }

    var cacheSet: MutableSet<Int> = mutableSetOf()
    fun getPossibleValues(): Set<Int> {
        cacheSet.clear()
        if (isSingleNum()) {
            cacheSet.add(number)
        } else {
            if (hintNumbers.isNullOrEmpty()) {
                for (i in 1..9) {
                    cacheSet.add(i)
                }
            } else {
                cacheSet.addAll(hintNumbers!!)
            }
        }
        return cacheSet
    }

    /**
     * 单元格数字全部被移除时抛此移除
     */
    class SudokuCellEmptyException @JvmOverloads constructor(msg: String = "单元格数字冲突...") : Exception(msg)
}
