package com.focus.tomato.audio

enum class NoisePreset(val label: String, val emoji: String, val description: String) {
    RAIN("雨声", "🌧️", "细密雨幕，适合深度工作"),
    FOREST("森林", "🌲", "树叶与鸟鸣，呼吸感更强"),
    OCEAN("海浪", "🌊", "缓慢潮汐，放慢心跳"),
    CAFE("咖啡馆", "☕", "低语与杯碟，像在窗边工作"),
    FIRE("篝火", "🔥", "噼啪轻响，温暖而专注"),
    BROWN("棕色噪音", "🎧", "稳定低频，屏蔽环境杂音")
}
