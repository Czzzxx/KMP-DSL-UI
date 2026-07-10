package com.example.capcut_project.ui.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.capcut_project.ui.model.*
import com.example.capcut_project.ui.LocalUiErrors
import com.example.capcut_project.ui.components.StyleErrorIndicator
import kotlinx.serialization.json.JsonObject

/**
 * 终极动态渲染引擎 (Layered DSL 2.0 优化精简版)
 * 
 * 它是做什么的：这是最核心的界面渲染入口，负责将 UiNode 树转化为 Compose UI。
 */
@Composable
fun DynamicRenderer(
    node: UiNode, 
    context: JsonObject, 
    onAction: (String) -> Unit, 
    modifier: Modifier = Modifier
) {
    // 1. 状态层检查 (State)
    val visibleExpr = node.state?.visible
    if (!com.example.capcut_project.engine.ExpressionEvaluator.isTruthy(visibleExpr, context)) return 

    val style = node.style
    val nodeModifier = Modifier.applyCommonStyle(style)
    val shape = style?.borderRadius.toShape()

    // 2. [解耦] 从隐式环境获取该节点的错误报告
    val errors = LocalUiErrors.current[node] ?: emptyList()

    // 3. 应用入场动画包装 (由 AnimationRenderer 中枢执行)
    RenderWithAnimation(node.animation, modifier) { animModifier ->
        Box(modifier = animModifier) {
            if (node is ButtonNode) {
                // 按钮逻辑：自带背景和交互
                RenderButton(node, context, onAction, nodeModifier, shape)
            } else {
            // 通用容器/原子组件逻辑
            val bgModifier = when {
                style?.backgroundGradient != null -> {
                    val colors = style.backgroundGradient!!.colors.map { parseColorSafely("渐变", it, Color.Gray) }
                    val brush = if (style.backgroundGradient!!.direction == "horizontal") {
                        Brush.horizontalGradient(colors)
                    } else {
                        Brush.verticalGradient(colors)
                    }
                    Modifier.background(brush, shape)
                }
                style?.backgroundColor != null -> {
                    val evaluatedColor = com.example.capcut_project.engine.ExpressionEvaluator.evaluate(style.backgroundColor, context)?.toString()
                    Modifier.background(parseColorSafely("背景", evaluatedColor, Color.Transparent), shape)
                }
                else -> Modifier
            }
            Box(modifier = nodeModifier.then(bgModifier).clip(shape)) {
                    RenderBackgroundImage(style)
                    val innerMod = Modifier.padding(style?.padding.toPaddingValues())
                    
                    when (node) {
                        is BoxNode -> RenderBox(node, context, onAction, innerMod)
                        is ColumnNode -> RenderColumn(node, context, onAction, innerMod)
                        is RowNode -> RenderRow(node, context, onAction, innerMod)
                        is TextNode -> RenderText(node, context, innerMod)
                        is ImageNode -> RenderImage(node, context, innerMod)
                        is UnknownNode -> RenderUnknown(node, modifier)
                        else -> {}
                    }
                }
            }

            // 视觉查错工具：如果有错，就在组件右上角画出感叹号
            if (errors.isNotEmpty()) {
                Box(modifier = Modifier.align(Alignment.TopEnd)) { 
                    StyleErrorIndicator(errors)
                }
            }
        }
    }
}

@Composable
private fun RenderButton(node: ButtonNode, context: JsonObject, onAction: (String) -> Unit, modifier: Modifier, shape: RoundedCornerShape) {
    val text = node.getText(context)
    val icon = node.iconUrl.resolveUrl()
    val bgColorStr = com.example.capcut_project.engine.ExpressionEvaluator.evaluate(node.style?.backgroundColor, context)?.toString()
    
    Button(
        onClick = { node.action?.get("onTap")?.let { onAction(it.target ?: "") } },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = parseColorSafely("按钮背景", bgColorStr, ButtonDefaults.buttonColors().containerColor)),
        shape = shape,
        contentPadding = node.style?.padding.toPaddingValues(),
        enabled = node.state?.enabled ?: true
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != "") androidx.compose.foundation.Image(painter = rememberAsyncImagePainter(icon), contentDescription = null, modifier = Modifier.size(node.iconSize.dp).padding(end = if (text != "") 4.dp else 0.dp) )
            val display = if (text == "" && icon == "") "Missing Text" else text
            if (display != "") Text(text = display, color = parseColorSafely("按钮文字", node.textColorStr, Color.White), fontSize = node.fontSize.sp, fontWeight = if (node.fontWeightStr.lowercase() == "bold") FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
private fun BoxScope.RenderBackgroundImage(style: UiStyle?) {
    val url = style?.backgroundImage?.resolveUrl()
    if (url != null && url != "") {
        androidx.compose.foundation.Image(painter = rememberAsyncImagePainter(url), contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun RenderBox(node: BoxNode, context: JsonObject, onAction: (String) -> Unit, modifier: Modifier) {
    val scroll = if (node.scrollable == "vertical") Modifier.verticalScroll(rememberScrollState()) else Modifier
    Box(modifier = modifier.then(scroll), contentAlignment = node.style.getBoxAlignment()) {
        node.children?.forEach { DynamicRenderer(it, context, onAction, Modifier.align(it.style.getBoxAlignment())) }
    }
}

@Composable
private fun RenderColumn(node: ColumnNode, context: JsonObject, onAction: (String) -> Unit, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = node.style.getHorizontalAlignment(), verticalArrangement = node.style.getVerticalArrangement(node.spacing)) {
        node.children?.forEach { child ->
            val weight = child.style?.weight?.toFloatOrNull()
            DynamicRenderer(child, context, onAction, if (weight != null) Modifier.weight(weight) else Modifier)
        }
    }
}

@Composable
private fun RenderRow(node: RowNode, context: JsonObject, onAction: (String) -> Unit, modifier: Modifier) {
    val marqueeSpec = node.animation?.loop?.takeIf { it.type == "marquee" }
    
    val rowContent = @Composable {
        Row(
            modifier = if (marqueeSpec != null) Modifier else modifier, 
            verticalAlignment = node.style.getVerticalAlignment(), 
            horizontalArrangement = node.style.getHorizontalArrangement(node.spacing)
        ) {
            node.children?.forEach { child ->
                val weight = child.style?.weight?.toFloatOrNull()
                DynamicRenderer(child, context, onAction, if (weight != null) Modifier.weight(weight) else Modifier)
            }
        }
    }

    if (marqueeSpec != null) {
        MarqueeContainer(spec = marqueeSpec, content = rowContent)
    } else {
        rowContent()
    }
}

@Composable
private fun RenderText(node: TextNode, context: JsonObject, modifier: Modifier) {
    val text = node.getText(context)
    val size = node.fontSize.sp
    val color = parseColorSafely("文字", node.textColorStr, Color.Unspecified)
    val weight = when(node.fontWeightStr.lowercase()) { "bold" -> FontWeight.Bold; "medium" -> FontWeight.Medium; else -> FontWeight.Normal }
    val deco = when(node.decorationStr.lowercase()) { "underline" -> TextDecoration.Underline; "line-through" -> TextDecoration.LineThrough; else -> TextDecoration.None }

    // 动效中枢派发逻辑
    if (node.animation?.enter?.type == "numRolling") {
        RenderRollingText(text, modifier, size, color, weight, node.animation!!.enter!!)
    } else {
        Text(text = text, modifier = modifier, color = color, fontSize = size, fontWeight = weight, textDecoration = deco, lineHeight = node.lineHeight.sp.takeIf { it.value > 0 } ?: androidx.compose.ui.unit.TextUnit.Unspecified)
    }
}

@Composable
private fun RenderImage(node: ImageNode, context: JsonObject, modifier: Modifier) {
    val url = node.getUrl(context).resolveUrl()
    val scale = when(node.contentScaleStr.lowercase()) { "fit" -> ContentScale.Fit; "fill" -> ContentScale.FillBounds; else -> ContentScale.Crop }
    AsyncImage(model = url, contentDescription = null, modifier = modifier, contentScale = scale)
}

@Composable
private fun RenderUnknown(node: UnknownNode, modifier: Modifier) {
    if (node.originalType == "BenefitItem") {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.width(80.dp)) {
            Box(modifier = Modifier.size(56.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp))) {
                androidx.compose.foundation.Image(painter = rememberAsyncImagePainter(node.props.getString("icon").resolveUrl()), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Text(text = node.props.getString("label"), color = Color(0xFF666666), fontSize = node.props.getDouble("labelSize", 10.0).sp, modifier = Modifier.padding(top = 8.dp), maxLines = 1)
        }
    } else {
        Box(modifier = modifier.background(Color(0xFFFFEBEE)).padding(8.dp), contentAlignment = Alignment.Center) {
            Text(text = "❌ ${node.errorMessage ?: "未知类型"}", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
