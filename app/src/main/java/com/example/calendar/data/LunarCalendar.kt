package com.example.calendar.data

import java.util.*

object LunarCalendar {

    private val lunarInfo = longArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0,
        0x09ad0, 0x055d2, 0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540,
        0x0d6a0, 0x0ada2, 0x095b0, 0x14977, 0x04970, 0x0a4b0, 0x0b4b5, 0x06a50,
        0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970, 0x06566, 0x0d4a0,
        0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2,
        0x0a950, 0x0b557, 0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5d0, 0x14573,
        0x052d0, 0x0a9a8, 0x0e950, 0x06aa0, 0x0aea6, 0x0ab50, 0x04b60, 0x0aae4,
        0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0, 0x096d0, 0x04dd5,
        0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46,
        0x0ab60, 0x09570, 0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58,
        0x05ac0, 0x0ab60, 0x096d5, 0x092e0, 0x0c960, 0x0d954, 0x0d4a0, 0x0da50,
        0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5, 0x0a950, 0x0b4a0,
        0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260,
        0x0ea65, 0x0d530, 0x05aa0, 0x076a3, 0x096d0, 0x04bd7, 0x04ad0, 0x0a4d0,
        0x1d0b6, 0x0d250, 0x0d520, 0x0dd45, 0x0b5a0, 0x056d0, 0x055b2, 0x049b0,
        0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0
    )

    private val chineseMonth = arrayOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    )

    private val chineseDay = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    fun getLunarDate(calendar: Calendar): String {
        var year = calendar.get(Calendar.YEAR)
        var month = calendar.get(Calendar.MONTH)
        var day = calendar.get(Calendar.DAY_OF_MONTH)

        var offset = ((calendar.time.time - Date(1900 - 1900, 0, 31).time) / 86400000).toInt()
        var i = 1900
        var days = 0

        while (i < 2100 && offset > 0) {
            days = yearDays(i)
            offset -= days
            i++
        }
        if (offset < 0) {
            offset += days
            i--
        }
        year = i

        var leap = leapMonth(i)
        var isLeap = false

        i = 1
        while (i < 13 && offset > 0) {
            days = if (isLeap && i == leap + 1) leapDays(year) else monthDays(year, i)
            offset -= days
            if (isLeap && i == leap + 1) isLeap = false
            if (i == leap) isLeap = true
            i++
        }

        if (offset == 0 && leap > 0 && i == leap + 1) {
            if (isLeap) {
                i--
            }
        }
        if (offset < 0) {
            offset += monthDays(year, i - 1)
            i--
        }

        val monthStr = chineseMonth[i - 1]
        val dayStr = chineseDay[offset]

        return "$monthStr$dayStr"
    }

    private fun yearDays(y: Int): Int {
        var sum = 348
        var i = 0x8000
        while (i > 0x8) {
            sum += if (lunarInfo[y - 1900] and i.toLong() != 0L) 1 else 0
            i = i shr 1
        }
        return sum + leapDays(y)
    }

    private fun leapDays(y: Int): Int {
        return if (leapMonth(y) != 0)
            if (lunarInfo[y - 1900] and 0x10000 != 0L) 30 else 29
        else
            0
    }

    private fun leapMonth(y: Int): Int {
        return (lunarInfo[y - 1900] and 0xf).toInt()
    }

    private fun monthDays(y: Int, m: Int): Int {
        return if (lunarInfo[y - 1900] and ((0x10000 shr m).toLong()) != 0L) 30 else 29
    }
}
