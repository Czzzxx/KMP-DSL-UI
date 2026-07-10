package com.example.capcut_project.ui.model

import com.example.capcut_project.engine.ExpressionEvaluator
import kotlinx.serialization.json.*

/**
 * 额外属性解析
 * 从 props 字典中掏出数据，并处理默认值和表达式求值。
 */

// 属性读取扩展
fun Map<String, JsonElement>?.getString(key: String, default: String = ""): String = 
    this?.get(key)?.jsonPrimitive?.content ?: default

fun Map<String, JsonElement>?.getDouble(key: String, default: Double = 0.0): Double = 
    this?.get(key)?.jsonPrimitive?.doubleOrNull ?: default

fun Map<String, JsonElement>?.getBoolean(key: String, default: Boolean = false): Boolean = 
    this?.get(key)?.jsonPrimitive?.booleanOrNull ?: default


// Text属性映射
fun TextNode.getText(context: JsonObject): String = 
    ExpressionEvaluator.evaluate(props.getString("text", "Missing Text"), context)?.toString() ?: "Missing Text"

val TextNode.fontSize: Double get() = props.getDouble("fontSize", 14.0)
val TextNode.textColorStr: String get() = props.getString("textColor")
val TextNode.fontWeightStr: String get() = props.getString("fontWeight")
val TextNode.decorationStr: String get() = props.getString("textDecoration")
val TextNode.lineHeight: Double get() = props.getDouble("lineHeight")

//Image属性映射
fun ImageNode.getUrl(context: JsonObject): String = 
    (ExpressionEvaluator.evaluate(props.getString("url"), context)?.toString() ?: "")

val ImageNode.contentScaleStr: String get() = props.getString("contentScale")

// Button属性映射
fun ButtonNode.getText(context: JsonObject): String = 
    ExpressionEvaluator.evaluate(props.getString("text", ""), context)?.toString() ?: ""

val ButtonNode.fontSize: Double get() = props.getDouble("fontSize", 16.0)
val ButtonNode.textColorStr: String get() = props.getString("textColor")
val ButtonNode.fontWeightStr: String get() = props.getString("fontWeight")
val ButtonNode.iconUrl: String get() = props.getString("icon")
val ButtonNode.iconSize: Double get() = props.getDouble("iconSize", 18.0)

// 容器属性映射
val BoxNode.scrollable: String get() = props.getString("scrollable")
val ColumnNode.spacing: Double get() = props.getDouble("spacing")
val RowNode.spacing: Double get() = props.getDouble("spacing")

// 动画特有属性映射
val AnimationSpec.interval: Double get() = props.getDouble("interval", 100.0)
val AnimationSpec.pauseOnTouch: Boolean get() = props.getBoolean("pauseOnTouch", false)
val AnimationSpec.direction: String get() = props.getString("direction", "left")
