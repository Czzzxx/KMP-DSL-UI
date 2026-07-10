package com.example.capcut_project.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.capcut_project.ui.model.UiNode

/**
 * LocalUiErrors.kt —— 【隐式错误报告环境】
 * 
 * 职责：利用 Compose 的 CompositionLocal 机制，在不污染 Node 模型的前提下，
 * 将解析阶段发现的错误报告传递给每一个渲染组件。
 */
val LocalUiErrors = staticCompositionLocalOf<Map<UiNode, List<String>>> { 
    emptyMap() 
}
