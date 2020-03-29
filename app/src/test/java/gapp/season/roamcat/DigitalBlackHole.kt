package gapp.season.roamcat

import org.junit.Test

//卡普雷卡尔黑洞（重排求差黑洞）
class DigitalBlackHole {
    @Test
    fun bh2() {
        bh_n(2, listOf(9), 5)
        bh_n(2, listOf(27), 6)
        bh_n(2, listOf(45), 6)
        bh_n(2, listOf(63), 6)
        bh_n(2, listOf(81), 6)
    }

    @Test
    fun bh3() {
        bh_n(3, listOf(495), 6)
    }

    @Test
    fun bh4() {
        bh_n(4, listOf(6174), 7) //卡普雷卡尔常数:6174
    }

    @Test
    fun bh5() {
        //所有数字都会收敛到这3个内循环之中
        //内循环1: 74943,62964,71973,83952 (74943为6步收敛，其他累加1)
        //内循环2: 53955,59994 (都是6步内收敛)
        //内循环3: 63954,61974,82962,75933 (63954为6步收敛，其他累加1)

        bh_n(5, listOf(74943, 59994, 63954), 6)
        bh_n(5, listOf(74943, 53955, 63954), 6)

        bh_n(5, listOf(62964, 53955, 63954), 7)
        bh_n(5, listOf(71973, 53955, 63954), 8)
        bh_n(5, listOf(83952, 53955, 63954), 9)

        bh_n(5, listOf(74943, 53955, 61974), 7)
        bh_n(5, listOf(74943, 53955, 82962), 8)
        bh_n(5, listOf(74943, 53955, 75933), 9)
    }

    @Test
    fun bh6() {
        //所有数字都会收敛到这3个内循环之中
        //内循环1: 851742,750843,840852,860832,862632,642654,420876
        //内循环2: 631764
        //内循环3: 549945
        bh_n(6, listOf(631764, 549945, 851742), 14)
        bh_n(6, listOf(631764, 549945, 750843), 15)
        bh_n(6, listOf(631764, 549945, 840852), 16)
        bh_n(6, listOf(631764, 549945, 860832), 17)
        bh_n(6, listOf(631764, 549945, 862632), 18)
        bh_n(6, listOf(631764, 549945, 642654), 19)
        bh_n(6, listOf(631764, 549945, 420876), 18)
    }

    @Test
    fun bh7() {
        //内循环1: 8429652, 7619733, 8439552, 7509843, 9529641, 8719722, 8649432, 7519743
        bh_n(7, listOf(8429652), 13)
        bh_n(7, listOf(7619733), 14)
        bh_n(7, listOf(8439552), 15)
        bh_n(7, listOf(7509843), 16)
        bh_n(7, listOf(9529641), 17)
        bh_n(7, listOf(8719722), 18)
        bh_n(7, listOf(8649432), 19)
        bh_n(7, listOf(7519743), 20)
    }

    @Test
    fun bh8() {
        //内循环1: 86526432,64308654,83208762
        //内循环2: 63317664
        //内循环3: 97508421
        //内循环4: 86326632,64326654,43208766,85317642,75308643,84308652,86308632
        bh_n(8, listOf(97508421, 63317664, 86526432, 86326632), 19)
        bh_n(8, listOf(97508421, 63317664, 86526432, 64326654), 19)
        bh_n(8, listOf(97508421, 63317664, 86526432, 43208766), 19)
        bh_n(8, listOf(97508421, 63317664, 86526432, 85317642), 19)
        bh_n(8, listOf(97508421, 63317664, 86526432, 75308643), 20)
        bh_n(8, listOf(97508421, 63317664, 86526432, 84308652), 21)
        bh_n(8, listOf(97508421, 63317664, 86526432, 86308632), 21)

        bh_n(8, listOf(97508421, 63317664, 64308654, 86326632), 20)
        bh_n(8, listOf(97508421, 63317664, 83208762, 86326632), 21)
    }

    //bh9-内循环1: 864197532
    //……
    //bh10-内循环1: 9975084201
    //bh10-内循环2: 9775084221,9755084421,9751088421
    //bh10-内循环3: 8655264432,6431088654,8732087622
    //……


    private val stepMap = mutableMapOf<Int, Int>() //缓存步数，用于优化性能
    private fun bh_n(digit: Int, holeNum: List<Int>, expectStep: Int) {
        var maxNum = ""
        var skipNum = ""
        for (j in 0 until digit) {
            maxNum += "9"
            skipNum += "1"
        }
        var maxStep = 0
        stepMap.clear()
        for (i in 0..maxNum.toInt()) {
            if (i % skipNum.toInt() != 0) {
                var step = 0
                var num = i
                val hashList = mutableListOf<Int>()
                while (!holeNum.contains(num)) {
                    val numMin = hashMin(num, digit)
                    hashList.add(numMin)
                    val stepTemp = stepMap[numMin]
                    if (stepTemp != null) {
                        step += stepTemp
                        if (step > expectStep) {
                            throw RuntimeException("数字${i}重排求差步数超过${expectStep}步(holeNum=$holeNum)")
                        }
                        break
                    } else {
                        val numMax = hashMax(num, digit)
                        num = numMax - numMin
                        step++
                        if (step > expectStep) {
                            throw RuntimeException("数字${i}重排求差步数超过${expectStep}步(holeNum=$holeNum)")
                        }
                    }
                }
                hashList.forEachIndexed { index, numHash ->
                    stepMap[numHash] = step - index
                }
                //println("num $i step = $step stepMap.size=${stepMap.size}")
                if (maxStep < step) {
                    maxStep = step
                }
            }
        }
        stepMap.clear()
        println("maxStep = $maxStep (holeNum=$holeNum)")
        assert(maxStep <= expectStep)
    }

    //废弃
    private fun dif(num: Int, digit: Int): Int {
        val array = Array(digit) { 0 }
        val numStr = num.toString()
        for (i in 1..digit) {
            array[i - 1] = if (numStr.length >= i) numStr.substring(i - 1, i).toInt() else 0
        }
        array.sortWith(Comparator { o1, o2 -> o1 - o2 })
        var str1 = ""
        array.forEach {
            str1 += it.toString()
        }
        val num1 = str1.toInt()
        array.sortWith(Comparator { o1, o2 -> o2 - o1 })
        var str2 = ""
        array.forEach {
            str2 += it.toString()
        }
        val num2 = str2.toInt()
        return num2 - num1
    }

    private fun hashMin(num: Int, digit: Int): Int {
        val array = num.toString().toCharArray()
        array.sort()
        var str = ""
        array.forEach { str += it }
        //println("num:$num hashMin = $str")
        return str.toInt()
    }

    private fun hashMax(num: Int, digit: Int): Int {
        val array = num.toString().toCharArray()
        array.sort()
        var str = ""
        array.forEach { str = "" + it + str }
        while (str.length < digit) {
            str = "${str}0"
        }
        //println("num:$num hashMax = $str")
        return str.toInt()
    }
}
