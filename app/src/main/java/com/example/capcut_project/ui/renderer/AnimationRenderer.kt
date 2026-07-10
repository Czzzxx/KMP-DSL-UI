package com.example.capcut_project.ui.renderer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import com.example.capcut_project.ui.model.*
import com.example.capcut_project.ui.model.AnimationSpec as DslAnimationSpec
import kotlinx.coroutines.delay

/**
 * AnimationRenderer.kt —— 【动效执行中枢 - 丝滑流畅版】
 */

@Composable
fun RenderWithAnimation(
    animConfig: UiAnimation?,
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit
) {
    val enterSpec = animConfig?.enter ?: return content(modifier)
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 【修复 1】移除硬编码的 300ms，严格遵守 DSL 下发的 delay。让 Compose 原生引擎处理时序
        if (enterSpec.delay > 0) delay(enterSpec.delay.toLong())
        visible = true
    }

    val easing = enterSpec.easing.toComposeEasing()
    val repeatMode = if (enterSpec.repeatMode.lowercase() == "reverse") RepeatMode.Reverse else RepeatMode.Restart

    val floatSpec: FiniteAnimationSpec<Float> = if (enterSpec.repeatCount != 0) {
        repeatable(
            iterations = if (enterSpec.repeatCount == -1) Int.MAX_VALUE else enterSpec.repeatCount + 1,
            animation = tween(durationMillis = enterSpec.duration, easing = easing),
            repeatMode = repeatMode
        )
    } else {
        tween(durationMillis = enterSpec.duration, easing = easing)
    }

    val intOffsetSpec: FiniteAnimationSpec<IntOffset> = if (enterSpec.repeatCount != 0) {
        repeatable(
            iterations = if (enterSpec.repeatCount == -1) Int.MAX_VALUE else enterSpec.repeatCount + 1,
            animation = tween(durationMillis = enterSpec.duration, easing = easing),
            repeatMode = repeatMode
        )
    } else {
        tween(durationMillis = enterSpec.duration, easing = easing)
    }

    AnimatedVisibility(
        visible = visible,
        enter = when (enterSpec.type) {
            "fadeIn" -> fadeIn(animationSpec = floatSpec)
            "scaleIn" -> scaleIn(animationSpec = floatSpec) + fadeIn(animationSpec = floatSpec)
            "slideInUp" -> slideInVertically(animationSpec = intOffsetSpec) { it } + fadeIn(animationSpec = floatSpec)
            "slideInDown" -> slideInVertically(animationSpec = intOffsetSpec) { -it } + fadeIn(animationSpec = floatSpec)
            else -> EnterTransition.None
        },
        modifier = modifier
    ) {
        content(Modifier)
    }
}

@Composable
fun MarqueeContainer(
    spec: DslAnimationSpec,
    content: @Composable () -> Unit
) {
    val duration = spec.duration.takeIf { it > 0 } ?: 10000
    val pauseOnTouch = spec.pauseOnTouch

    var contentWidth by remember { mutableStateOf(0f) }
    val scrollAmount = remember { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(contentWidth, isPaused) {
        // 【修复 2】移除 600ms 的硬等待。只要宽度测算完成 (contentWidth > 0)，立刻开始滚动，拒绝发呆
        if (contentWidth <= 0f || isPaused) return@LaunchedEffect

        // 等待一帧，确保布局已经真正挂载到屏幕上，防止首帧跳跃
        withFrameNanos { }

        while (true) {
            val remainingDistance = contentWidth + scrollAmount.value
            val remainingDuration = (duration * (remainingDistance / contentWidth)).toInt().coerceAtLeast(10)

            scrollAmount.animateTo(
                targetValue = -contentWidth,
                animationSpec = tween(remainingDuration, easing = LinearEasing)
            )
            scrollAmount.snapTo(0f)
        }
    }

    val touchModifier = if (pauseOnTouch) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(onPress = {
                isPaused = true
                tryAwaitRelease()
                isPaused = false
            })
        }
    } else Modifier

    val fadeModifier = Modifier.graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Transparent, 0.05f to Color.Black,
                0.95f to Color.Black, 1f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }

    Box(
        modifier = touchModifier
            .fillMaxWidth()
            .clipToBounds()
            .then(fadeModifier)
    ) {
        SubcomposeLayout { constraints ->
            val placeable1 = subcompose("content_1") {
                Row(modifier = Modifier.wrapContentWidth(unbounded = true)) {
                    content()
                }
            }.map { it.measure(Constraints(maxWidth = Constraints.Infinity)) }.firstOrNull()

            val placeable2 = subcompose("content_2") {
                Row(modifier = Modifier.wrapContentWidth(unbounded = true)) {
                    content()
                }
            }.map { it.measure(Constraints(maxWidth = Constraints.Infinity)) }.firstOrNull()

            if (placeable1 == null || placeable2 == null) {
                return@SubcomposeLayout layout(0, 0) {}
            }

            contentWidth = placeable1.width.toFloat()

            layout(constraints.maxWidth, placeable1.height) {
                placeable1.placeRelative(scrollAmount.value.toInt(), 0)
                placeable2.placeRelative((scrollAmount.value + contentWidth).toInt(), 0)
            }
        }
    }
}

@Composable
fun RenderRollingText(
    text: String,
    modifier: Modifier,
    fontSize: TextUnit,
    fontColor: Color,
    fontWeight: FontWeight,
    spec: DslAnimationSpec
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        text.forEachIndexed { index, char ->
            key(index) {
                if (char.isDigit()) {
                    SingleDigitRolling(index, char, fontSize, fontColor, fontWeight, spec)
                } else {
                    Text(text = char.toString(), fontSize = fontSize, color = fontColor, fontWeight = fontWeight, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun SingleDigitRolling(
    index: Int,
    targetChar: Char,
    fontSize: TextUnit,
    fontColor: Color,
    fontWeight: FontWeight,
    spec: DslAnimationSpec
) {
    val duration = spec.duration.toLong()
    val baseInterval = spec.interval.toLong()
    val easing = spec.easing.toComposeEasing()
    val targetDigit = targetChar.digitToIntOrNull() ?: 0

    // 【修复 3】让它一开始显示一个随机数字，而不是目标数字！这是解决“停顿感”的灵魂所在
    val initialDigit = remember { (0..9).random() }
    var displayState by remember { mutableStateOf("${initialDigit}_0") }

    var currentFrameDelay by remember { mutableStateOf(baseInterval) }

    LaunchedEffect(Unit) {
        // 【修复 4】删掉了 800ms 的硬等待，仅保留基础的错位启动时间 (25ms间隔让数字滚动更有层次感)
        delay(spec.delay.toLong() + (index * 25L))

        val startTime = System.currentTimeMillis()
        var currentDigit = initialDigit
        var step = 0

        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            val fraction = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            val transformedFraction = easing.transform(fraction)
            if (fraction >= 1.0f) break

            // 【修复 5】全局在 0-9 之间随机，彻底解决目标值为 9 时滚动卡死的 Bug
            val pool = (0..9).filter { it != currentDigit }
            currentDigit = if (pool.isNotEmpty()) pool.random() else (0..9).random()

            step++
            displayState = "${currentDigit}_$step"
            currentFrameDelay = (baseInterval + (transformedFraction * 500)).toLong()
            delay(currentFrameDelay)
        }
        displayState = "${targetChar}_FINAL"
    }

    Box(
        modifier = Modifier
            .height(with(LocalDensity.current) { fontSize.toDp() * 1.4f })
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = displayState,
            transitionSpec = {
                val isFinalFrame = targetState.endsWith("_FINAL")
                val animEasing = if (isFinalFrame) LinearOutSlowInEasing else LinearEasing
                val animDuration = (currentFrameDelay * 0.8f).toInt().coerceAtLeast(10)
                ContentTransform(
                    targetContentEnter = slideInVertically(animationSpec = tween(animDuration, easing = animEasing)) { -it } + fadeIn(animationSpec = tween(animDuration)),
                    initialContentExit = slideOutVertically(animationSpec = tween(animDuration, easing = animEasing)) { it } + fadeOut(animationSpec = tween(animDuration)),
                    targetContentZIndex = if (isFinalFrame) 9999f else 1f
                )
            },
            label = "NumRolling"
        ) { stateString ->
            Text(text = stateString[0].toString(), fontSize = fontSize, color = fontColor, fontWeight = fontWeight, softWrap = false)
        }
    }
}

private fun String.toComposeEasing(): Easing = when (this.lowercase()) {
    "easein" -> FastOutLinearInEasing
    "easeout" -> LinearOutSlowInEasing
    "easeinout" -> FastOutSlowInEasing
    else -> LinearEasing
}