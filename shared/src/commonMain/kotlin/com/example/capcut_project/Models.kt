package com.example.capcut_project

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * 结构化样式能力
 */
@Serializable
data class UiStyle(
    val width: String? = null,              // 宽度
    val height: String? = null,             // 高度
    val backgroundColor: String? = null,    // 背景颜色
    val textColor: String? = null,          // 文字颜色
    val fontSize: String? = null,           // 字号
    val fontWeight: String? = null,         // 字体粗细
    val borderRadius: String? = null,       // 圆角半径
    val padding: String? = null,            // 内边距
    val margin: String? = null,             // 外边距
    val horizontalAlignment: String? = null, // 水平对齐: "Start", "Center", "End"
    val verticalAlignment: String? = null    // 垂直对齐: "Top", "Center", "Bottom"
)

/**
 * 基础 UI 节点模型 (IR)
 */
@Serializable
sealed class UiNode {
    abstract val styles: UiStyle?           // 样式对象
    abstract val props: Map<String, String>? // 业务属性扩展 Map
    abstract val children: List<UiNode>?     // 子节点列表
}

/**
 * Box 组件：基础容器，支持子节点堆叠
 */
@Serializable
@SerialName("Box")
data class BoxNode(
    override val styles: UiStyle? = null,
    override val props: Map<String, String>? = null,
    override val children: List<UiNode>? = null
) : UiNode()

/**
 * Text 组件：显示文本
 */
@Serializable
@SerialName("Text")
data class TextNode(
    val text: String,                       // 文本内容
    override val styles: UiStyle? = null,
    override val props: Map<String, String>? = null
) : UiNode() {
    override val children: List<UiNode>? = null
}

/**
 * Image 组件：显示图片
 */
@Serializable
@SerialName("Image")
data class ImageNode(
    val url: String,                        // 图片 URL 链接
    override val styles: UiStyle? = null,
    override val props: Map<String, String>? = null
) : UiNode() {
    override val children: List<UiNode>? = null
}

/**
 * Button 组件：交互按钮
 */
@Serializable
@SerialName("Button")
data class ButtonNode(
    val text: String,                       // 按钮文案
    val actionId: String? = null,           // 点击事件 ID
    override val styles: UiStyle? = null,
    override val props: Map<String, String>? = null
) : UiNode() {
    override val children: List<UiNode>? = null
}

/**
 * Column 组件：垂直布局容器
 */
@Serializable
@SerialName("Column")
data class ColumnNode(
    override val styles: UiStyle? = null,
    override val props: Map<String, String>? = null,
    override val children: List<UiNode>? = null
) : UiNode()

/**
 * Row 组件：水平布局容器
 */
@Serializable
@SerialName("Row")
data class RowNode(
    override val styles: UiStyle? = null,
    override val props: Map<String, String>? = null,
    override val children: List<UiNode>? = null
) : UiNode()
