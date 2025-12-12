package com.example.calendar.data

import java.util.*

object Zodiac {

    private val zodiacArray = arrayOf(
        "摩羯座", "水瓶座", "双鱼座", "白羊座", "金牛座", "双子座",
        "巨蟹座", "狮子座", "处女座", "天秤座", "天蝎座", "射手座"
    )

    private val zodiacEdgeDay = intArrayOf(
        20, 19, 21, 21, 21, 22, 23, 23, 23, 23, 22, 22
    )

    fun getZodiac(calendar: Calendar): String {
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return if (day < zodiacEdgeDay[month])
            zodiacArray[(month + 11) % 12]
        else
            zodiacArray[month]
    }
}
