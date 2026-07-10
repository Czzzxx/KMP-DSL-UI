package com.example.capcut_project.ui.model

import kotlinx.serialization.Serializable
import com.example.capcut_project.ui.UiNodeSerializer
import kotlinx.serialization.json.JsonElement

/**
 * 基础 UI 节点模型
 */
@Serializable(with = UiNodeSerializer::class)
sealed class UiNode {
    abstract val id: String?
    abstract val style: UiStyle?
    abstract val props: Map<String, JsonElement>?
    abstract val state: UiState?
    abstract val action: Map<String, UiAction>?
    abstract val animation: UiAnimation?
    abstract val children: List<UiNode>?
}
