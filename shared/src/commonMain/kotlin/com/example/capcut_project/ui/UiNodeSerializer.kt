/**
 * 把原始JSON翻译成规整的Kotlin对象。
 */

package com.example.capcut_project.ui

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import com.example.capcut_project.ui.model.*

/**
 * 多态解析器
 */
@OptIn(kotlinx.serialization.InternalSerializationApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)
object UiNodeSerializer : KSerializer<UiNode> {
    override val descriptor: SerialDescriptor = buildSerialDescriptor("UiNode", PolymorphicKind.SEALED)

    override fun deserialize(decoder: Decoder): UiNode {
        val input = decoder as? JsonDecoder ?: throw kotlinx.serialization.SerializationException("Only JSON supported")
        val sanitizedTree = sanitizeNode(input.decodeJsonElement())
        val json = sanitizedTree.jsonObject
        val type = json["type"]?.jsonPrimitive?.content ?: "Unknown"
        
        // 根据 type 动态选择序列化策略
        val strategy = when (type) {
            "Box" -> BoxNode.serializer()
            "Text" -> TextNode.serializer()
            "Image" -> ImageNode.serializer()
            "Button" -> ButtonNode.serializer()
            "Column" -> ColumnNode.serializer()
            "Row" -> RowNode.serializer()
            else -> {
                // 处理 UnknownNode 场景
                val unknown = UnknownNode(
                    originalType = type,
                    errorMessage = if (json["props"] == null) "未知组件类型: '$type'" else null,
                    style = json["style"]?.let { input.json.decodeFromJsonElement<UiStyle>(it) },
                    props = json["props"]?.jsonObject,
                    state = json["state"]?.let { input.json.decodeFromJsonElement<UiState>(it) },
                    action = json["action"]?.let { input.json.decodeFromJsonElement<Map<String, UiAction>>(it) }
                )
                return unknown
            }
        }

        return try {
            input.json.decodeFromJsonElement(strategy, sanitizedTree)
        } catch (e: Throwable) {
            UnknownNode(
                originalType = type,
                errorMessage = "属性解析失败: ${e.message}",
                style = json["style"]?.let { input.json.decodeFromJsonElement<UiStyle>(it) }
            )
        }
    }

    /**
     * 递归扫描 JSON，标准化样式并处理children子节点
     */
    private fun sanitizeNode(element: JsonElement): JsonElement {
        if (element !is JsonObject) return element
        return buildJsonObject {
            element.forEach { (key, value) ->
                when (key) {
                    "style" -> put("style", sanitizeStyle(value))
                    "children" -> if (value is JsonArray) put(key, JsonArray(value.map { sanitizeNode(it) })) else put(key, value)
                    else -> put(key, value)
                }
            }
        }
    }

    /**
     * 将 style 对象中简写的属性标准化为四方向对象格式
     */
    private fun sanitizeStyle(element: JsonElement): JsonElement {
        if (element !is JsonObject) return element
        return buildJsonObject {
            element.forEach { (key, value) ->
                when {
                    (key == "padding" || key == "margin") && value is JsonPrimitive -> {
                        val n = value.content.toDoubleOrNull() ?: 0.0
                        put(key, buildJsonObject { put("top", n); put("bottom", n); put("left", n); put("right", n) })
                    }
                    key == "borderRadius" && value is JsonPrimitive -> {
                        val content = value.content
                        val parts = content.split(",")
                        if (parts.size == 4) {
                            put(key, buildJsonObject {
                                put("top-left", parts[0].trim().toDoubleOrNull() ?: 0.0)
                                put("top-right", parts[1].trim().toDoubleOrNull() ?: 0.0)
                                put("bottom-right", parts[2].trim().toDoubleOrNull() ?: 0.0)
                                put("bottom-left", parts[3].trim().toDoubleOrNull() ?: 0.0)
                            })
                        } else {
                            val n = content.toDoubleOrNull() ?: 0.0
                            put(key, buildJsonObject { put("top-left", n); put("top-right", n); put("bottom-left", n); put("bottom-right", n) })
                        }
                    }
                    else -> put(key, value)
                }
            }
        }
    }

    override fun serialize(encoder: Encoder, value: UiNode) {
        val composite = encoder as? JsonEncoder ?: throw kotlinx.serialization.SerializationException("Only JSON supported")
        val element = when (value) {
            is BoxNode -> composite.json.encodeToJsonElement(BoxNode.serializer(), value)
            is TextNode -> composite.json.encodeToJsonElement(TextNode.serializer(), value)
            is ImageNode -> composite.json.encodeToJsonElement(ImageNode.serializer(), value)
            is ButtonNode -> composite.json.encodeToJsonElement(ButtonNode.serializer(), value)
            is ColumnNode -> composite.json.encodeToJsonElement(ColumnNode.serializer(), value)
            is RowNode -> composite.json.encodeToJsonElement(RowNode.serializer(), value)
            is UnknownNode -> composite.json.encodeToJsonElement(UnknownNode.serializer(), value)
        }
        composite.encodeJsonElement(element)
    }
}
