package com.example.capcut_project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import coil.compose.AsyncImage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * 核心校验辅助：尝试转换数字，失败则举报
 */
fun String?.toDpOrReport(name: String, errors: MutableList<String>, default: Dp = 0.dp): Dp {
    if (this == null) return default
    val value = this.toIntOrNull()
    return if (value != null) {
        value.dp
    } else {
        errors.add("非法${name}: '$this' (需为纯数字)")
        default
    }
}

/**
 * 核心校验辅助：解析复合值（如 "10,20,10,20"），失败则举报
 */
fun parsePaddingOrReport(name: String, value: String?, errors: MutableList<String>): PaddingValues {
    if (value == null) return PaddingValues(0.dp)
    val parts = value.split(",")
    return try {
        if (parts.size == 1) {
            val v = parts[0].trim().toInt().dp
            PaddingValues(v)
        } else if (parts.size == 4) {
            PaddingValues(
                top = parts[0].trim().toInt().dp,
                end = parts[1].trim().toInt().dp,
                bottom = parts[2].trim().toInt().dp,
                start = parts[3].trim().toInt().dp
            )
        } else {
            errors.add("非法${name}格式: '$value' (需为单值或4值)")
            PaddingValues(0.dp)
        }
    } catch (e: Exception) {
        errors.add("非法${name}数值: '$value' (包含非数字)")
        PaddingValues(0.dp)
    }
}

/**
 * 样式解析前缀识别（针对本地资源）
 */
fun String.resolveUrl(): String {
    if (this.isBlank()) return ""
    if (this.startsWith("http") || this.startsWith("file:///")) return this
    return "file:///android_asset/images/$this"
}

/**
 * 样式枚举校验
 */
fun validateStyle(name: String, value: String?, validValues: List<String>): String? {
    if (value == null) return null
    return if (value in validValues) null else "非法$name: '$value' (预期: ${validValues.joinToString("/")})"
}

/**
 * 安全解析颜色
 */
fun parseColorSafely(name: String, colorStr: String?, default: Color, errorCollector: MutableList<String>): Color {
    if (colorStr == null) return default
    return try {
        Color(android.graphics.Color.parseColor(colorStr))
    } catch (e: Exception) {
        errorCollector.add("非法${name}颜色格式: '$colorStr'")
        default
    }
}

/**
 * 黄色感叹号错误指示器
 */
@Composable
fun StyleErrorIndicator(messages: List<String>) {
    var showTooltip by remember { mutableStateOf(false) }
    Box(modifier = Modifier.padding(start = 4.dp)) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(Color(0xFFFFD600), CircleShape)
                .border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape)
                .clickable { showTooltip = !showTooltip },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "!", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
        if (showTooltip) {
            Popup(alignment = Alignment.TopStart, onDismissRequest = { showTooltip = false }) {
                Box(modifier = Modifier.padding(top = 20.dp).border(1.dp, Color.Red, RoundedCornerShape(4.dp)).background(Color.White).padding(8.dp)) {
                    Column {
                        Text(text = "样式配置错误:", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        messages.forEach { msg -> Text(text = "• $msg", color = Color.Red, fontSize = 10.sp) }
                    }
                }
            }
        }
    }
}

/**
 * 应用样式并严格拦截错误
 */
fun Modifier.applyUiStyle(style: UiStyle?, errors: MutableList<String>): Modifier {
    if (style == null) return this
    var modifier = this

    // 1. 枚举验证
    validateStyle("水平对齐", style.horizontalAlignment, listOf("Start", "Center", "End"))?.let { errors.add(it) }
    validateStyle("垂直对齐", style.verticalAlignment, listOf("Top", "Center", "Bottom"))?.let { errors.add(it) }

    // 2. 宽高解析 (严格拦截)
    val widthMod = when (val w = style.width) {
        "fill" -> Modifier.fillMaxWidth()
        "wrap" -> Modifier.wrapContentWidth()
        null -> Modifier
        else -> Modifier.width(w.toDpOrReport("宽度", errors))
    }
    val heightMod = when (val h = style.height) {
        "fill" -> Modifier.fillMaxHeight()
        "wrap" -> Modifier.wrapContentHeight()
        null -> Modifier
        else -> Modifier.height(h.toDpOrReport("高度", errors))
    }
    modifier = modifier.then(widthMod).then(heightMod)

    // 3. 背景解析 (严格拦截)
    if (style.backgroundColor != null || style.borderRadius != null) {
        val bgColor = parseColorSafely("背景", style.backgroundColor, Color.Transparent, errors)
        val radius = style.borderRadius.toDpOrReport("圆角", errors)
        modifier = modifier.background(bgColor, RoundedCornerShape(radius))
    }

    // 4. 内外边距 (严格拦截)
    if (style.padding != null) {
        modifier = modifier.padding(parsePaddingOrReport("内边距", style.padding, errors))
    }
    if (style.margin != null) {
        // Compose 的 margin 实现为背景前的 padding
        modifier = Modifier.padding(parsePaddingOrReport("外边距", style.margin, errors)).then(modifier)
    }

    return modifier
}

@Composable
fun DynamicRenderer(node: UiNode, context: JsonObject, onAction: (String) -> Unit) {
    if (!ExpressionEvaluator.isTruthy(node.visible, context)) return 

    val localErrors = remember(node) { mutableStateListOf<String>() }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f, fill = false)) {
            when (node) {
                is BoxNode -> {
                    Box(
                        modifier = Modifier.applyUiStyle(node.styles, localErrors),
                        contentAlignment = node.styles?.getBoxAlignment() ?: Alignment.TopStart
                    ) {
                        node.children?.forEach { DynamicRenderer(it, context, onAction) }
                    }
                }
                is ColumnNode -> {
                    Column(
                        modifier = Modifier.applyUiStyle(node.styles, localErrors),
                        horizontalAlignment = node.styles?.getHorizontalAlignment() ?: Alignment.Start
                    ) {
                        node.children?.forEach { DynamicRenderer(it, context, onAction) }
                    }
                }
                is RowNode -> {
                    Row(
                        modifier = Modifier.applyUiStyle(node.styles, localErrors),
                        verticalAlignment = node.styles?.getVerticalAlignment() ?: Alignment.Top
                    ) {
                        node.children?.forEach { DynamicRenderer(it, context, onAction) }
                    }
                }
                is TextNode -> {
                    val color = parseColorSafely("文字", node.styles?.textColor, Color.Unspecified, localErrors)
                    val textValue = ExpressionEvaluator.evaluate(node.text, context)?.toString() ?: ""
                    val fontSize = node.styles?.fontSize.toDpOrReport("字号", localErrors, 14.dp).value.sp
                    
                    Text(
                        text = textValue,
                        modifier = Modifier.applyUiStyle(node.styles, localErrors),
                        color = color,
                        fontSize = fontSize,
                        fontWeight = if (node.styles?.fontWeight == "Bold") FontWeight.Bold else FontWeight.Normal
                    )
                }
                is ImageNode -> {
                    val urlValue = (ExpressionEvaluator.evaluate(node.url, context)?.toString() ?: "").resolveUrl()
                    AsyncImage(
                        model = urlValue,
                        contentDescription = null,
                        modifier = Modifier.applyUiStyle(node.styles, localErrors),
                        contentScale = ContentScale.Fit
                    )
                }
                is ButtonNode -> {
                    val btnBgColor = parseColorSafely("按钮背景", node.styles?.backgroundColor, ButtonDefaults.buttonColors().containerColor, localErrors)
                    val btnTxtColor = parseColorSafely("按钮文字", node.styles?.textColor, Color.White, localErrors)
                    val textValue = ExpressionEvaluator.evaluate(node.text, context)?.toString() ?: ""
                    val fontSize = node.styles?.fontSize.toDpOrReport("字号", localErrors, 16.dp).value.sp
                    
                    Button(
                        onClick = { node.actionId?.let { onAction(it) } },
                        modifier = Modifier.applyUiStyle(node.styles, localErrors),
                        colors = ButtonDefaults.buttonColors(containerColor = btnBgColor),
                        shape = RoundedCornerShape(node.styles?.borderRadius.toDpOrReport("圆角", localErrors, 0.dp)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(text = textValue, color = btnTxtColor, fontSize = fontSize)
                    }
                }
                is IconButtonNode -> {
                    val iconUrlValue = (ExpressionEvaluator.evaluate(node.iconUrl, context)?.toString() ?: "").resolveUrl()
                    androidx.compose.material3.IconButton(
                        onClick = { node.actionId?.let { onAction(it) } },
                        modifier = Modifier.applyUiStyle(node.styles, localErrors)
                    ) {
                        AsyncImage(model = iconUrlValue, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }
                is UnknownNode -> {
                    if (node.originalType == "BenefitItem") {
                        Column(modifier = Modifier.padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(65.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))) {
                                AsyncImage(model = (node.props?.get("url") ?: "").resolveUrl(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                                Text(text = node.props?.get("text") ?: "", color = Color.White, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
                            }
                        }
                    } else {
                        Box(modifier = Modifier.applyUiStyle(node.styles, localErrors).background(Color(0xFFFFEBEE)).padding(8.dp), contentAlignment = Alignment.Center) {
                            Text(text = "❌ ${node.errorMessage}", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        if (localErrors.isNotEmpty()) {
            StyleErrorIndicator(localErrors)
        }
    }
}

fun UiStyle.getBoxAlignment(): Alignment {
    return when (horizontalAlignment) {
        "Center" -> when (verticalAlignment) {
            "Center" -> Alignment.Center
            "Bottom" -> Alignment.BottomCenter
            else -> Alignment.TopCenter
        }
        "End" -> when (verticalAlignment) {
            "Center" -> Alignment.CenterEnd
            "Bottom" -> Alignment.BottomEnd
            else -> Alignment.TopEnd
        }
        else -> when (verticalAlignment) {
            "Center" -> Alignment.CenterStart
            "Bottom" -> Alignment.BottomStart
            else -> Alignment.TopStart
        }
    }
}

fun UiStyle.getHorizontalAlignment(): Alignment.Horizontal {
    return when (horizontalAlignment) {
        "Start" -> Alignment.Start
        "Center" -> Alignment.CenterHorizontally
        "End" -> Alignment.End
        else -> Alignment.Start
    }
}

fun UiStyle.getVerticalAlignment(): Alignment.Vertical {
    return when (verticalAlignment) {
        "Top" -> Alignment.Top
        "Center" -> Alignment.CenterVertically
        "Bottom" -> Alignment.Bottom
        else -> Alignment.Top
    }
}

@Preview(showBackground = true)
@Composable
fun ProRetainPopupPreview() {
    val jsonString = """
{
  "type": "Box",
  "styles": { "width": "320", "backgroundColor": "#FFFFFF", "borderRadius": "24" },
  "children": [
    {
      "type": "IconButton",
      "iconUrl": "file:///android_asset/images/ic_close.png",
      "styles": { "horizontalAlignment": "End", "padding": "12" }
    },
    {
      "type": "Column",
      "styles": { "padding": "0,24,24,24", "horizontalAlignment": "Center" },
      "children": [
        {
          "type": "Image",
          "url": "file:///android_asset/images/retain_main.png",
          "styles": { "width": "140", "height": "120" }
        },
        {
          "type": "Text",
          "text": "送你专属特权",
          "styles": { "fontSize": "20", "fontWeight": "Bold", "margin": "8,0,16,0" }
        },
        {
          "type": "Button",
          "text": "继续试用",
          "styles": { "width": "fill", "backgroundColor": "#FF2C55", "textColor": "#FFFFFF", "borderRadius": "28", "padding": "12" }
        }
      ]
    }
  ]
}
    """.trimIndent()
    val node = Json { ignoreUnknownKeys = true }.decodeFromString<UiNode>(jsonString)
    Box(modifier = Modifier.fillMaxSize().background(Color.Gray), contentAlignment = Alignment.Center) {
        DynamicRenderer(node, context = buildJsonObject {}, onAction = {})
    }
}
