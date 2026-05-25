package com.example.capcut_project

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 结构化样式能力
 */
@Serializable
data class UiStyle(
    val width: String? = null,
    val height: String? = null,
    val backgroundColor: String? = null,
    val textColor: String? = null,
    val fontSize: String? = null,
    val fontWeight: String? = null,
    val borderRadius: String? = null,
    val padding: String? = null,
    val margin: String? = null,
    val horizontalAlignment: String? = null,
    val verticalAlignment: String? = null
)

/**
 * 自定义多态解析器：实现DSL错误类型或字段拦截
 */
@OptIn(kotlinx.serialization.InternalSerializationApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)
object UiNodeSerializer : KSerializer<UiNode> {
    override val descriptor: SerialDescriptor = buildSerialDescriptor("UiNode", PolymorphicKind.SEALED)

    override fun deserialize(decoder: Decoder): UiNode {
        val input = decoder as? JsonDecoder 
            ?: throw kotlinx.serialization.SerializationException("仅支持 JSON 格式解析")
        val tree = input.decodeJsonElement()
        val jsonObject = tree.jsonObject
        val type = jsonObject["type"]?.jsonPrimitive?.content ?: "Unknown"
        
        // 1. 验证组件类型是否存在
        val strategy = when (type) {
            "Box" -> BoxNode.serializer()
            "Text" -> TextNode.serializer()
            "Image" -> ImageNode.serializer()
            "Button" -> ButtonNode.serializer()
            "IconButton" -> IconButtonNode.serializer()
            "Column" -> ColumnNode.serializer()
            "Row" -> RowNode.serializer()
            else -> {
                // 如果不认识，但带有 props，视为自定义业务组件，不直接判死刑
                return UnknownNode(
                    originalType = type,
                    errorMessage = if (jsonObject["props"] == null) "未知组件类型: '$type'" else null,
                    props = try { input.json.decodeFromJsonElement<Map<String, String>>(jsonObject) } catch(e: Exception) { null },
                    styles = tryExtractStyles(input, jsonObject)
                )
            }
        }

        return try {
            // 2. 尝试解析该组件的内部属性
            input.json.decodeFromJsonElement(strategy, tree)
        } catch (e: Throwable) {
            // 3. 属性解析失败
            val errorMsg = when (e) {
                is kotlinx.serialization.MissingFieldException -> "缺少必填字段: ${e.missingFields.joinToString()}"
                else -> "属性解析失败: ${e.message}"
            }
            UnknownNode(
                originalType = type,
                errorMessage = errorMsg,
                styles = tryExtractStyles(input, jsonObject)
            )
        }
    }

    private fun tryExtractStyles(input: JsonDecoder, json: JsonObject): UiStyle? {
        return try {
            json["styles"]?.let { input.json.decodeFromJsonElement<UiStyle>(it) }
        } catch (e: Exception) {
            null
        }
    }

    override fun serialize(encoder: Encoder, value: UiNode) {
        val composite = encoder as? JsonEncoder ?: throw kotlinx.serialization.SerializationException("仅支持 JSON")
        val element = when (value) {
            is BoxNode -> composite.json.encodeToJsonElement(BoxNode.serializer(), value)
            is TextNode -> composite.json.encodeToJsonElement(TextNode.serializer(), value)
            is ImageNode -> composite.json.encodeToJsonElement(ImageNode.serializer(), value)
            is ButtonNode -> composite.json.encodeToJsonElement(ButtonNode.serializer(), value)
            is IconButtonNode -> composite.json.encodeToJsonElement(IconButtonNode.serializer(), value)
            is ColumnNode -> composite.json.encodeToJsonElement(ColumnNode.serializer(), value)
            is RowNode -> composite.json.encodeToJsonElement(RowNode.serializer(), value)
            is UnknownNode -> composite.json.encodeToJsonElement(UnknownNode.serializer(), value)
        }
        composite.encodeJsonElement(element)
    }
}

/**
 * 基础 UI 节点模型 (IR)
 */
@Serializable(with = UiNodeSerializer::class)
sealed class UiNode {
    abstract val styles: UiStyle?
    abstract val props: Map<String, String>?
    abstract val children: List<UiNode>?
    abstract val visible: String?
}

/**
 * Unknown 组件：携带详细错误信息
 */
@Serializable
@SerialName("Unknown")
data class UnknownNode(
    val originalType: String? = null,
    val errorMessage: String? = null,
    override val styles: UiStyle? = null,
    override val props: Map<String, String>? = null,
    override val children: List<UiNode>? = null,
    override val visible: String? = null
) : UiNode()

@Serializable
@SerialName("Box")
data class BoxNode(
    override val styles: UiStyle? = null,
    override val props: Map<String, String>? = null,
    override val children: List<UiNode>? = null,
    override val visible: String? = null
) : UiNode()

@Serializable
@SerialName("Text")
data class TextNode(
    val text: String,
    override val styles: UiStyle? = null,
    override val props: Map<String, String>? = null,
    override val visible: String? = null
) : UiNode() {
    override val children: List<UiNode>? = null
}

@Serializable
@SerialName("Image")
data class ImageNode(
    val url: String,
    override val styles: UiStyle? = null,
    override val props: Map<String, String>? = null,
    override val visible: String? = null
) : UiNode() {
    override val children: List<UiNode>? = null
}

/**
 * Button 组件：交互按钮
 */
@Serializable
@SerialName("Button")
data class ButtonNode(
    val text: String,
    val actionId: String? = null,
    override val styles: UiStyle? = null,
    override val props: Map<String, String>? = null,
    override val visible: String? = null
) : UiNode() {
    override val children: List<UiNode>? = null
}

/**
 * IconButton 组件：纯图标按钮（如关闭按钮）
 */
@Serializable
@SerialName("IconButton")
data class IconButtonNode(
    val iconUrl: String,
    val actionId: String? = null,
    override val styles: UiStyle? = null,
    override val props: Map<String, String>? = null,
    override val visible: String? = null
) : UiNode() {
    override val children: List<UiNode>? = null
}

@Serializable
@SerialName("Column")
data class ColumnNode(
    override val styles: UiStyle? = null,
    override val props: Map<String, String>? = null,
    override val children: List<UiNode>? = null,
    override val visible: String? = null
) : UiNode()

@Serializable
@SerialName("Row")
data class RowNode(
    override val styles: UiStyle? = null,
    override val props: Map<String, String>? = null,
    override val children: List<UiNode>? = null,
    override val visible: String? = null
) : UiNode()
