package com.example.capcut_project.ui.renderer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.capcut_project.ui.model.CornerRadius
import com.example.capcut_project.ui.model.Spacing
import com.example.capcut_project.ui.model.UiStyle
import kotlinx.serialization.json.*

/**
 * 样式解析辅助工具 (Layered DSL 2.0 终极版)
 * 
 * 它是做什么的：这是一个“物理翻译官”，负责把 JSON 里冰冷的文字描述转换成真实的物理参数。
 */

fun String?.parseSize(): Dp? {
    return when (this) {
        "match_parent" -> null 
        "wrap_content" -> null
        null -> null
        else -> this.toDoubleOrNull()?.dp ?: 0.dp
    }
}

fun Spacing?.toPaddingValues(): PaddingValues {
    if (this == null) return PaddingValues(0.dp)
    return PaddingValues(top = top.dp, bottom = bottom.dp, start = left.dp, end = right.dp)
}

fun CornerRadius?.toShape(): RoundedCornerShape {
    if (this == null) return RoundedCornerShape(0.dp)
    return RoundedCornerShape(
        topStart = topLeft.dp, topEnd = topRight.dp, 
        bottomEnd = bottomRight.dp, bottomStart = bottomLeft.dp
    )
}

fun parseColorSafely(name: String, colorStr: String?, default: Color): Color {
    if (colorStr.isNullOrBlank()) return default
    if (colorStr.equals("Transparent", ignoreCase = true)) return Color.Transparent
    return try { 
        Color(android.graphics.Color.parseColor(colorStr)) 
    } catch (e: Exception) { 
        default 
    }
}

@Composable
fun String.resolveUrl(): Any {
    if (this.isBlank()) return ""
    if (this.startsWith("http") || this.startsWith("file:///")) return this
    if (this.startsWith("res://")) {
        val context = LocalContext.current
        val resName = this.removePrefix("res://")
        val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
        return if (resId != 0) resId else this
    }
    return "file:///android_asset/images/$this"
}

// 属性读取扩展已迁移至 shared 模块的 PropsMapper.kt 中

fun UiStyle?.getBoxAlignment(): Alignment {
    val align = this?.alignment?.lowercase() ?: ""
    return when {
        align == "center" -> Alignment.Center
        align.contains("top") && align.contains("center") -> Alignment.TopCenter
        align.contains("bottom") && align.contains("center") -> Alignment.BottomCenter
        align.contains("start") || align.contains("left") -> {
            if (align.contains("top")) Alignment.TopStart else if (align.contains("bottom")) Alignment.BottomStart else Alignment.CenterStart
        }
        align.contains("end") || align.contains("right") -> {
            if (align.contains("top")) Alignment.TopEnd else if (align.contains("bottom")) Alignment.BottomEnd else Alignment.CenterEnd
        }
        else -> Alignment.Center // 默认居中
    }
}

fun UiStyle?.getHorizontalAlignment(): Alignment.Horizontal {
    val align = this?.alignment?.lowercase() ?: ""
    return when {
        align.contains("end") || align.contains("right") -> Alignment.End
        align.contains("start") || align.contains("left") -> Alignment.Start
        else -> Alignment.CenterHorizontally // 默认居中
    }
}

fun UiStyle?.getVerticalAlignment(): Alignment.Vertical {
    val align = this?.alignment?.lowercase() ?: ""
    return when {
        align.contains("bottom") -> Alignment.Bottom
        align.contains("top") -> Alignment.Top
        else -> Alignment.CenterVertically // 默认居中
    }
}

fun UiStyle?.getHorizontalArrangement(spacing: Double): Arrangement.Horizontal {
    val align = this?.alignment?.lowercase() ?: ""
    return when {
        align.contains("space-between") -> Arrangement.SpaceBetween
        align.contains("space-evenly") -> Arrangement.SpaceEvenly
        align.contains("center") -> Arrangement.Center
        align.contains("end") || align.contains("right") -> Arrangement.End
        else -> Arrangement.spacedBy(spacing.dp)
    }
}

fun UiStyle?.getVerticalArrangement(spacing: Double): Arrangement.Vertical {
    val align = this?.alignment?.lowercase() ?: ""
    return when {
        align.contains("space-between") -> Arrangement.SpaceBetween
        align.contains("space-evenly") -> Arrangement.SpaceEvenly
        align.contains("center") -> Arrangement.Center
        align.contains("bottom") || align.contains("end") -> Arrangement.Bottom
        else -> Arrangement.spacedBy(spacing.dp)
    }
}
