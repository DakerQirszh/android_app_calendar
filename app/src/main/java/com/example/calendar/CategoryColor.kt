package com.example.calendar

import androidx.compose.ui.graphics.Color

object CategoryColor {
    fun colorFor(category: Int): Color = when (category) {
        0 -> Color(0xFF4CAF50) // 工作（绿）
        1 -> Color(0xFF2196F3) // 学习（蓝）
        2 -> Color(0xFFFF9800) // 生活（橙）
        3 -> Color(0xFFE91E63) // 提醒（粉）
        else -> Color(0xFF9E9E9E) // 其他（灰）
    }
}
