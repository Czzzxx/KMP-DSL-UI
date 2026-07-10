/**
 * Modifier(修饰器) 扩展，简化 Compose UI 样式代码的编写
 */

package com.example.capcut_project.ui.renderer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import com.example.capcut_project.ui.model.UiStyle



fun Modifier.applyCommonStyle(style: UiStyle?): Modifier {
    if (style == null) return this
    var m = this
    
    // 1. 处理尺寸
    val finalWMod = when (val w = style.width) {
        "match_parent" -> Modifier.fillMaxWidth()
        "wrap_content" -> Modifier.wrapContentWidth()
        null -> Modifier
        else -> w.parseSize()?.let { Modifier.width(it) } ?: Modifier
    }

    val finalHMod = when (val h = style.height) {
        "match_parent" -> Modifier.fillMaxHeight()
        "wrap_content" -> Modifier.wrapContentHeight()
        null -> Modifier
        else -> h.parseSize()?.let { Modifier.height(it) } ?: Modifier
    }

    m = m.then(finalWMod).then(finalHMod)
    
    // 2. 处理外边距 (Margin)
    if (style.margin != null) {
        m = m.padding(style.margin.toPaddingValues())
    }

    return m
}
