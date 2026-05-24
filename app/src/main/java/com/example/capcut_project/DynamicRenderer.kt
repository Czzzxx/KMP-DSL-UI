package com.example.capcut_project

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.serialization.json.Json

/**
 * 将 UiStyle 转换为 Compose Modifier
 */
//把 JSON 里的样式转成 Compose 能用的 Modifier
fun Modifier.applyUiStyle(style: UiStyle?): Modifier {
    if (style == null) return this
    var modifier = this

    // 宽高
    val w = style.width
    val widthMod = when (w) {
        "fill" -> Modifier.fillMaxWidth()
        "wrap" -> Modifier.wrapContentWidth()
        null -> Modifier
        else -> Modifier.width(w.toIntOrNull()?.dp ?: 0.dp)
    }
    val h = style.height
    val heightMod = when (h) {
        "fill" -> Modifier.fillMaxHeight()
        "wrap" -> Modifier.wrapContentHeight()
        null -> Modifier
        else -> Modifier.height(h.toIntOrNull()?.dp ?: 0.dp)
    }
    modifier = modifier.then(widthMod).then(heightMod)

    // 背景与圆角
    if (style.backgroundColor != null || style.borderRadius != null) {
        val color = style.backgroundColor?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.Transparent
        val radius = style.borderRadius?.toIntOrNull()?.dp ?: 0.dp
        modifier = modifier.background(color, RoundedCornerShape(radius))
    }

    // 内边距 Padding
    style.padding?.let {
        val parts = it.split(",")
        if (parts.size == 1) {
            modifier = modifier.padding(it.toIntOrNull()?.dp ?: 0.dp)
        } else if (parts.size == 4) {
            modifier = modifier.padding(
                start = parts[3].trim().toIntOrNull()?.dp ?: 0.dp,
                top = parts[0].trim().toIntOrNull()?.dp ?: 0.dp,
                end = parts[1].trim().toIntOrNull()?.dp ?: 0.dp,
                bottom = parts[2].trim().toIntOrNull()?.dp ?: 0.dp
            )
        }
    }

    // 外边距 Margin (在 background 之前应用)
    style.margin?.let {
        val parts = it.split(",")
        if (parts.size == 1) {
            modifier = Modifier.padding(it.toIntOrNull()?.dp ?: 0.dp).then(modifier)
        } else if (parts.size == 4) {
            modifier = Modifier.padding(
                start = parts[3].trim().toIntOrNull()?.dp ?: 0.dp,
                top = parts[0].trim().toIntOrNull()?.dp ?: 0.dp,
                end = parts[1].trim().toIntOrNull()?.dp ?: 0.dp,
                bottom = parts[2].trim().toIntOrNull()?.dp ?: 0.dp
            ).then(modifier)
        }
    }

    return modifier
}

//渲染器，根据节点类型画出对应的 UI
@Composable
fun DynamicRenderer(node: UiNode) {
    when (node) {
        is BoxNode -> {
            Box(
                modifier = Modifier.applyUiStyle(node.styles),
                contentAlignment = node.styles?.getBoxAlignment() ?: Alignment.TopStart
            ) {
                node.children?.forEach { DynamicRenderer(it) }
            }
        }
        is ColumnNode -> {
            Column(
                modifier = Modifier.applyUiStyle(node.styles),
                horizontalAlignment = node.styles?.getHorizontalAlignment() ?: Alignment.Start
            ) {
                node.children?.forEach { DynamicRenderer(it) }
            }
        }
        is RowNode -> {
            Row(
                modifier = Modifier.applyUiStyle(node.styles),
                verticalAlignment = node.styles?.getVerticalAlignment() ?: Alignment.Top
            ) {
                node.children?.forEach { DynamicRenderer(it) }
            }
        }
        is TextNode -> {
            Text(
                text = node.text,
                modifier = Modifier.applyUiStyle(node.styles),
                color = node.styles?.textColor?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.Unspecified,
                fontSize = node.styles?.fontSize?.toIntOrNull()?.sp ?: 14.sp,
                fontWeight = if (node.styles?.fontWeight == "Bold") FontWeight.Bold else FontWeight.Normal
            )
        }
        is ImageNode -> {
            AsyncImage(
                model = node.url,
                contentDescription = null,
                modifier = Modifier.applyUiStyle(node.styles),
                contentScale = ContentScale.Fit
            )
        }
        is ButtonNode -> {
            Button(
                onClick = { /* TODO: Handle actionId */ },
                modifier = Modifier.applyUiStyle(node.styles),
                colors = ButtonDefaults.buttonColors(
                    containerColor = node.styles?.backgroundColor?.let { Color(android.graphics.Color.parseColor(it)) } ?: ButtonDefaults.buttonColors().containerColor
                ),
                shape = RoundedCornerShape(node.styles?.borderRadius?.toIntOrNull()?.dp ?: 0.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = node.text,
                    color = node.styles?.textColor?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color.White,
                    fontSize = node.styles?.fontSize?.toIntOrNull()?.sp ?: 16.sp,
                    fontWeight = if (node.styles?.fontWeight == "Bold") FontWeight.Bold else FontWeight.Normal
                )
            }
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

//预览函数，把 JSON 解析并显示出来
@Preview(showBackground = true)
@Composable
fun FullRetainPopupPreview() {
    val jsonFormatter = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }
    // Using a shorter version of the actual retain_popup.json for the preview test
    val jsonString = """
{
  "type": "Box",
  "styles": {
    "width": "320",
    "backgroundColor": "#FFFFFF",
    "borderRadius": "16",
    "padding": "24",
    "horizontalAlignment": "Center"
  },
  "children": [
    {
      "type": "Column",
      "styles": {
        "horizontalAlignment": "Center"
      },
      "children": [
        {
          "type": "Image",
          "url": "https://p16-va.lemon8cdn.com/tos-alisg-i-93f9u0uv91-sg/8f8e8e8e8e8e8e8e8e8e8e8e8e8e8e8e~tplv-93f9u0uv91-image.webp",
          "styles": {
            "width": "120",
            "height": "120"
          }
        },
        {
          "type": "Text",
          "text": "确认放弃 7 天免费试用吗？",
          "styles": {
            "fontSize": "18",
            "fontWeight": "Bold",
            "textColor": "#1A1A1A",
            "margin": "16,0,0,0"
          }
        },
        {
          "type": "Image",
          "url": "https://p16-va.lemon8cdn.com/tos-alisg-i-93f9u0uv91-sg/8f8e8e8e8e8e8e8e8e8e8e8e8e8e8e8e~tplv-93f9u0uv91-image.webp",
          "styles": {
            "width": "120",
            "height": "120"
          }
        },
        {
          "type": "Text",
          "text": "开通即可解锁 100+ 专业特效",
          "styles": {
            "fontSize": "14",
            "textColor": "#666666",
            "margin": "8,0,0,0"
          }
        },
        {
          "type": "Button",
          "text": "继续试用",
          "styles": {
            "width": "fill",
            "backgroundColor": "#FF2C55",
            "textColor": "#FFFFFF",
            "borderRadius": "25",
            "padding": "12",
            "margin": "24,0,0,0"
          }
        },
        {
          "type": "Button",
          "text": "放弃权益",
          "styles": {
            "width": "fill",
            "backgroundColor": "#F5F5F5",
            "textColor": "#999999",
            "borderRadius": "25",
            "padding": "12",
            "margin": "8,0,0,0"
          }
        }
      ]
    }
  ]
}
    """.trimIndent()
    val node = jsonFormatter.decodeFromString<UiNode>(jsonString)
    Box(modifier = Modifier.fillMaxSize().background(Color.Gray), contentAlignment = Alignment.Center) {
        DynamicRenderer(node)
    }
}
