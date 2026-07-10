package com.example.capcut_project.ui.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement

/**
 * 具体 UI 组件实现类
 *存放所有具体的 UI 组件模型。
 */

@Serializable
@SerialName("Unknown")
data class UnknownNode(
    override val id: String? = null,
    val originalType: String? = null,
    val errorMessage: String? = null,
    override val style: UiStyle? = null,
    override val props: Map<String, JsonElement>? = null,
    override val state: UiState? = null,
    override val action: Map<String, UiAction>? = null,
    override val animation: UiAnimation? = null,
    override val children: List<UiNode>? = null
) : UiNode()

@Serializable
@SerialName("Box")
data class BoxNode(
    override val id: String? = null,
    override val style: UiStyle? = null,
    override val props: Map<String, JsonElement>? = null,
    override val state: UiState? = null,
    override val action: Map<String, UiAction>? = null,
    override val animation: UiAnimation? = null,
    override val children: List<UiNode>? = null
) : UiNode()

@Serializable
@SerialName("Text")
data class TextNode(
    override val id: String? = null,
    override val style: UiStyle? = null,
    override val props: Map<String, JsonElement>? = null,
    override val state: UiState? = null,
    override val action: Map<String, UiAction>? = null,
    override val animation: UiAnimation? = null
) : UiNode() {
    override val children: List<UiNode>? = null
}

@Serializable
@SerialName("Image")
data class ImageNode(
    override val id: String? = null,
    override val style: UiStyle? = null,
    override val props: Map<String, JsonElement>? = null,
    override val state: UiState? = null,
    override val action: Map<String, UiAction>? = null,
    override val animation: UiAnimation? = null
) : UiNode() {
    override val children: List<UiNode>? = null
}

@Serializable
@SerialName("Button")
data class ButtonNode(
    override val id: String? = null,
    override val style: UiStyle? = null,
    override val props: Map<String, JsonElement>? = null,
    override val state: UiState? = null,
    override val action: Map<String, UiAction>? = null,
    override val animation: UiAnimation? = null,
    override val children: List<UiNode>? = null
) : UiNode()

@Serializable
@SerialName("Column")
data class ColumnNode(
    override val id: String? = null,
    override val style: UiStyle? = null,
    override val props: Map<String, JsonElement>? = null,
    override val state: UiState? = null,
    override val action: Map<String, UiAction>? = null,
    override val animation: UiAnimation? = null,
    override val children: List<UiNode>? = null
) : UiNode()

@Serializable
@SerialName("Row")
data class RowNode(
    override val id: String? = null,
    override val style: UiStyle? = null,
    override val props: Map<String, JsonElement>? = null,
    override val state: UiState? = null,
    override val action: Map<String, UiAction>? = null,
    override val animation: UiAnimation? = null,
    override val children: List<UiNode>? = null
) : UiNode()
