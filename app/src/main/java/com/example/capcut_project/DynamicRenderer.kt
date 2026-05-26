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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/**
 * 样式解析辅助
 */
fun String?.toDpOrReport(name: String, errors: SnapshotStateList<String>, default: Dp = 0.dp): Dp {
    if (this == null) return default
    val value = this.toIntOrNull()
    return if (value != null) value.dp else {
        if (name == "圆角" && this.contains(",")) return default
        errors.add("非法${name}: '$this'")
        default
    }
}

fun parseCornerRadiusOrReport(value: String?, errors: SnapshotStateList<String>): Shape {
    if (value == null) return RoundedCornerShape(0.dp)
    val parts = value.split(",")
    return try {
        if (parts.size == 1) RoundedCornerShape(parts[0].trim().toInt().dp)
        else if (parts.size == 4) RoundedCornerShape(
            topStart = parts[0].trim().toInt().dp,
            topEnd = parts[1].trim().toInt().dp,
            bottomEnd = parts[2].trim().toInt().dp,
            bottomStart = parts[3].trim().toInt().dp
        )
        else { errors.add("非法圆角格式: '$value'"); RoundedCornerShape(0.dp) }
    } catch (e: Exception) {
        errors.add("非法圆角数值: '$value'")
        RoundedCornerShape(0.dp)
    }
}

fun parsePaddingOrReport(name: String, value: String?, errors: SnapshotStateList<String>): PaddingValues {
    if (value == null) return PaddingValues(0.dp)
    val parts = value.split(",")
    return try {
        if (parts.size == 1) PaddingValues(parts[0].trim().toInt().dp)
        else if (parts.size == 4) PaddingValues(
            top = parts[0].trim().toInt().dp,
            end = parts[1].trim().toInt().dp,
            bottom = parts[2].trim().toInt().dp,
            start = parts[3].trim().toInt().dp
        )
        else { errors.add("非法${name}格式: '$value'"); PaddingValues(0.dp) }
    } catch (e: Exception) {
        errors.add("非法${name}数值: '$value'"); PaddingValues(0.dp)
    }
}

fun String.resolveUrl(): String {
    if (this.isBlank() || this.startsWith("http") || this.startsWith("file:///")) return this
    return "file:///android_asset/images/$this"
}

fun parseColorSafely(name: String, colorStr: String?, default: Color, errorCollector: SnapshotStateList<String>): Color {
    if (colorStr == null) return default
    if (colorStr == "Transparent") return Color.Transparent
    return try { Color(android.graphics.Color.parseColor(colorStr)) } 
    catch (e: Exception) { errorCollector.add("非法${name}颜色: '$colorStr'"); default }
}

@Composable
fun StyleErrorIndicator(messages: List<String>) {
    var showTooltip by remember { mutableStateOf(false) }
    Box(modifier = Modifier.padding(start = 4.dp)) {
        Box(modifier = Modifier.size(18.dp).background(Color(0xFFFFD600), CircleShape).clickable { showTooltip = !showTooltip }, contentAlignment = Alignment.Center) {
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
 * 分解后的 Modifier 应用：基础与背景
 */
fun Modifier.applyBaseAndBackground(style: UiStyle?, errors: SnapshotStateList<String>): Modifier {
    if (style == null) return this
    var m = this
    
    val wMod = when (val w = style.width) {
        "fill" -> Modifier.fillMaxWidth()
        "wrap" -> Modifier.wrapContentWidth()
        null -> Modifier
        else -> Modifier.width(w.toDpOrReport("宽度", errors))
    }
    val hMod = when (val h = style.height) {
        "fill" -> Modifier.fillMaxHeight()
        "wrap" -> Modifier.wrapContentHeight()
        null -> Modifier
        else -> Modifier.height(h.toDpOrReport("高度", errors))
    }
    m = m.then(wMod).then(hMod)
    if (style.margin != null) m = Modifier.padding(parsePaddingOrReport("外边距", style.margin, errors)).then(m)
    
    val shape = parseCornerRadiusOrReport(style.borderRadius, errors)
    val bgColor = parseColorSafely("背景", style.backgroundColor, Color.Transparent, errors)
    return m.background(bgColor, shape).clip(shape)
}

fun Modifier.applyPaddingOnly(style: UiStyle?, errors: SnapshotStateList<String>): Modifier {
    if (style?.padding == null) return this
    return this.padding(parsePaddingOrReport("内边距", style.padding, errors))
}

@Composable
fun DynamicRenderer(node: UiNode, context: JsonObject, onAction: (String) -> Unit) {
    if (!ExpressionEvaluator.isTruthy(node.visible, context)) return 
    val localErrors = remember(node) { mutableStateListOf<String>() }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f, fill = false)) {
            when (node) {
                is BoxNode, is ColumnNode, is RowNode -> {
                    Box(modifier = Modifier.applyBaseAndBackground(node.styles, localErrors)) {
                        val bgImageUrl = node.styles?.backgroundImage?.resolveUrl()
                        if (!bgImageUrl.isNullOrEmpty()) {
                            val painter = rememberAsyncImagePainter(model = bgImageUrl)
                            if (painter.state is AsyncImagePainter.State.Error) {
                                LaunchedEffect(bgImageUrl) { localErrors.add("背景图加载失败") }
                            }
                            androidx.compose.foundation.Image(
                                painter = painter, contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        val innerModifier = Modifier
                            .then(if (node.styles?.width == "fill") Modifier.fillMaxWidth() else Modifier)
                            .then(if (node.styles?.height == "fill") Modifier.fillMaxHeight() else Modifier)
                            .applyPaddingOnly(node.styles, localErrors)
                        
                        when (node) {
                            is BoxNode -> Box(modifier = innerModifier, contentAlignment = node.styles?.getBoxAlignment() ?: Alignment.TopStart) {
                                node.children?.forEach { DynamicRenderer(it, context, onAction) }
                            }
                            is ColumnNode -> Column(
                                modifier = innerModifier, 
                                horizontalAlignment = node.styles?.getHorizontalAlignment() ?: Alignment.Start,
                                verticalArrangement = node.styles?.getVerticalArrangement() ?: Arrangement.Top
                            ) {
                                node.children?.forEach { child ->
                                    val weightVal = child.styles?.weight?.toFloatOrNull() ?: 0f
                                    if (weightVal > 0f) Box(Modifier.weight(weightVal)) { DynamicRenderer(child, context, onAction) }
                                    else DynamicRenderer(child, context, onAction)
                                }
                            }
                            is RowNode -> Row(
                                modifier = innerModifier, 
                                verticalAlignment = node.styles?.getVerticalAlignment() ?: Alignment.Top,
                                horizontalArrangement = node.styles?.getHorizontalArrangement() ?: Arrangement.Start
                            ) {
                                node.children?.forEach { child ->
                                    if (child is UnknownNode && child.originalType == "BenefitItem") {
                                        Box(Modifier.weight(1f)) { DynamicRenderer(child, context, onAction) }
                                    } else {
                                        val weightVal = child.styles?.weight?.toFloatOrNull() ?: 0f
                                        if (weightVal > 0f) Box(Modifier.weight(weightVal)) { DynamicRenderer(child, context, onAction) }
                                        else DynamicRenderer(child, context, onAction)
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
                is TextNode -> {
                    val textValue = ExpressionEvaluator.evaluate(node.text, context)?.toString() ?: ""
                    Text(
                        text = textValue,
                        modifier = Modifier.applyBaseAndBackground(node.styles, localErrors).applyPaddingOnly(node.styles, localErrors),
                        color = parseColorSafely("文字", node.styles?.textColor, Color.Unspecified, localErrors),
                        fontSize = node.styles?.fontSize.toDpOrReport("字号", localErrors, 14.dp).value.sp,
                        fontWeight = if (node.styles?.fontWeight == "Bold") FontWeight.Bold else FontWeight.Normal,
                        textAlign = when(node.styles?.horizontalAlignment) {
                            "Center" -> androidx.compose.ui.text.style.TextAlign.Center
                            "End" -> androidx.compose.ui.text.style.TextAlign.End
                            else -> androidx.compose.ui.text.style.TextAlign.Start
                        }
                    )
                }
                is ImageNode -> {
                    val urlValue = (ExpressionEvaluator.evaluate(node.url, context)?.toString() ?: "").resolveUrl()
                    val painter = rememberAsyncImagePainter(model = urlValue)
                    if (painter.state is AsyncImagePainter.State.Error) {
                        LaunchedEffect(urlValue) { localErrors.add("图片加载失败") }
                    }
                    androidx.compose.foundation.Image(
                        painter = painter, contentDescription = null,
                        modifier = Modifier.applyBaseAndBackground(node.styles, localErrors).applyPaddingOnly(node.styles, localErrors),
                        contentScale = ContentScale.Fit
                    )
                }
                is ButtonNode -> {
                    val textValue = ExpressionEvaluator.evaluate(node.text, context)?.toString() ?: ""
                    Button(
                        onClick = { node.actionId?.let { onAction(it) } },
                        modifier = Modifier.applyBaseAndBackground(node.styles, localErrors),
                        colors = ButtonDefaults.buttonColors(containerColor = parseColorSafely("按钮背景", node.styles?.backgroundColor, ButtonDefaults.buttonColors().containerColor, localErrors)),
                        shape = parseCornerRadiusOrReport(node.styles?.borderRadius, localErrors),
                        contentPadding = parsePaddingOrReport("内边距", node.styles?.padding, localErrors)
                    ) {
                        Text(text = textValue, color = parseColorSafely("按钮文字", node.styles?.textColor, Color.White, localErrors), fontSize = node.styles?.fontSize.toDpOrReport("字号", localErrors, 16.dp).value.sp)
                    }
                }
                is IconButtonNode -> {
                    val iconUrlValue = (ExpressionEvaluator.evaluate(node.iconUrl, context)?.toString() ?: "").resolveUrl()
                    val painter = rememberAsyncImagePainter(model = iconUrlValue)
                    if (painter.state is AsyncImagePainter.State.Error) {
                        LaunchedEffect(iconUrlValue) { localErrors.add("图标加载失败") }
                    }
                    androidx.compose.material3.IconButton(
                        onClick = { node.actionId?.let { onAction(it) } },
                        modifier = Modifier.applyBaseAndBackground(node.styles, localErrors)
                    ) {
                        androidx.compose.foundation.Image(painter = painter, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                }
                is UnknownNode -> {
                    if (node.originalType == "BenefitItem") {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)).border(0.5.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))) {
                                val painter = rememberAsyncImagePainter(model = (node.props?.get("url") ?: "").resolveUrl())
                                androidx.compose.foundation.Image(painter = painter, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)), startY = 50f)))
                                Text(text = node.props?.get("text") ?: "", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp), maxLines = 1)
                            }
                        }
                    } else {
                        Box(modifier = Modifier.applyBaseAndBackground(node.styles, localErrors).background(Color(0xFFFFEBEE)).padding(8.dp), contentAlignment = Alignment.Center) {
                            Text(text = "❌ ${node.errorMessage ?: "未知错误"}", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        if (localErrors.isNotEmpty()) Box(modifier = Modifier.align(Alignment.Top)) { StyleErrorIndicator(localErrors) }
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

fun UiStyle.getHorizontalArrangement(): Arrangement.Horizontal {
    return when (horizontalAlignment) {
        "Start" -> Arrangement.Start
        "Center" -> Arrangement.Center
        "End" -> Arrangement.End
        else -> Arrangement.Start
    }
}

fun UiStyle.getVerticalArrangement(): Arrangement.Vertical {
    return when (verticalAlignment) {
        "Top" -> Arrangement.Top
        "Center" -> Arrangement.Center
        "Bottom" -> Arrangement.Bottom
        else -> Arrangement.Top
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
