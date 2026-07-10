package com.example.capcut_project.ui.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement

/**
 * 基础 UI 辅助模型
 */

@Serializable
data class Spacing(
    val top: Double = 0.0,
    val bottom: Double = 0.0,
    val left: Double = 0.0,
    val right: Double = 0.0
)

@Serializable
data class CornerRadius(
    @SerialName("top-left") val topLeft: Double = 0.0,
    @SerialName("top-right") val topRight: Double = 0.0,
    @SerialName("bottom-left") val bottomLeft: Double = 0.0,
    @SerialName("bottom-right") val bottomRight: Double = 0.0
)

@Serializable
data class UiAction(
    val type: String,   // navigate | back | dismiss | showToast | id | custom
    val target: String? = null
)

/**
 * 动画定义对象 (核心结构)
 */
@Serializable
data class AnimationSpec(
    val type: String,               // 动画类型: fadeIn | scaleIn | slideInUp | slideInDown | numRolling
    val duration: Int = 300,        // 动画时长(ms)
    val delay: Int = 0,             // 延迟开始时间(ms)
    val easing: String = "linear",  // 缓动曲线: linear | easeIn | easeOut | easeInOut
    val repeatCount: Int = 0,       // 重复次数
    val repeatMode: String = "restart", // 重复模式: restart | reverse
    val props: Map<String, JsonElement>? = null
)

/**
 * 动效表现层 (Animation)
 */
@Serializable
data class UiAnimation(
    val enter: AnimationSpec? = null, // 入场动画
    val exit: AnimationSpec? = null,  // 退场动画 [预留]
    val onTap: AnimationSpec? = null, // 点击动画 [预留]
    val loop: AnimationSpec? = null   // 循环动画
)

/**
 * 逻辑状态层 (State)
 */
@Serializable
data class UiState(
    val visible: String? = "true",
    val enabled: Boolean? = true
)

/**
 * 渐变色定义对象
 */
@Serializable
data class GradientSpec(
    val direction: String = "vertical", // vertical | horizontal
    val colors: List<String>
)

/**
 * 通用样式模型 (Style)
 */
@Serializable
data class UiStyle(
    val width: String? = null,
    val height: String? = null,
    val padding: Spacing? = null,
    val margin: Spacing? = null,
    val backgroundColor: String? = null,
    val backgroundGradient: GradientSpec? = null,
    val borderRadius: CornerRadius? = null,
    val alignment: String? = null,
    val weight: String? = null,
    val backgroundImage: String? = null
)
