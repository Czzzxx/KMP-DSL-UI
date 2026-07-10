package com.example.capcut_project.ui

import com.example.capcut_project.ui.model.*

/**
 * 进行静态扫描，返回节点与错误信息的映射表。
 */
object UiValidator {

    private val colorRegex = Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$")

    // 白名单定义
    private val VALID_WEIGHTS = setOf("normal", "medium", "bold")
    private val VALID_SCALES = setOf("fit", "fill", "crop")
    private val VALID_DECORATIONS = setOf("none", "underline", "line-through")
    private val VALID_SCROLL = setOf("none", "vertical")
    private val VALID_ACTION_TYPES = setOf("navigate", "back", "dismiss", "showToast", "id")
    
    // 动画白名单
    private val VALID_ANIM_TYPES = setOf("fadeIn", "scaleIn", "slideInUp", "slideInDown", "numRolling", "marquee")
    private val VALID_EASINGS = setOf("linear", "easeIn", "easeOut", "easeInOut")
    private val VALID_REPEAT_MODES = setOf("restart", "reverse")
    
    // 基础关键词，用于模糊匹配校验
    private val ALIGN_KEYWORDS = setOf("center", "start", "left", "end", "right", "top", "bottom")


    fun validate(node: UiNode, report: MutableMap<UiNode, List<String>> = mutableMapOf()): Map<UiNode, List<String>> {
        val errors = mutableListOf<String>()

        // 检查样式层 (Style)
        node.style?.let { style ->
            // 背景颜色格式校验
            style.backgroundColor?.let { color ->
                if (color != "Transparent" && !color.startsWith("{") && !colorRegex.matches(color)) {
                    errors.add("属性 'backgroundColor' 格式非法: '$color'。示例: '#FF2C55' 或表达式 '{...}'")
                }
            }
            
            // 对齐策略合法性校验
            style.alignment?.lowercase()?.let { align ->
                val parts = align.split("-", " ")
                if (!parts.all { it in ALIGN_KEYWORDS }) {
                    errors.add("属性 'alignment' 取值 '$align' 无效。可用关键词: $ALIGN_KEYWORDS")
                }
            }
        }

        // 检查业务属性层 (Props)
        node.props?.let { props ->
            // 文字颜色校验
            val textColor = props["textColor"]?.toString()?.replace("\"", "") ?: ""
            if (textColor.isNotEmpty() && !colorRegex.matches(textColor)) {
                errors.add("属性 'textColor' 格式非法: '$textColor'")
            }

            // 枚举类字段校验 (白名单模式)
            checkEnum(props, "fontWeight", VALID_WEIGHTS, errors)
            checkEnum(props, "contentScale", VALID_SCALES, errors)
            checkEnum(props, "textDecoration", VALID_DECORATIONS, errors)
            checkEnum(props, "scrollable", VALID_SCROLL, errors)
        }

        // 检查动画表现层 (Animation)
        node.animation?.let { anim ->
            anim.enter?.let { spec ->
                if (spec.type !in VALID_ANIM_TYPES) errors.add("入场动画类型 '${spec.type}' 无效。建议: $VALID_ANIM_TYPES")
                if (spec.easing !in VALID_EASINGS) errors.add("动画曲线 '${spec.easing}' 无效。建议: $VALID_EASINGS")
                if (spec.repeatMode !in VALID_REPEAT_MODES) errors.add("动画模式 '${spec.repeatMode}' 无效。建议: $VALID_REPEAT_MODES")
                
                // 检查 props 内部字段
                spec.props?.let { animProps ->
                    if (spec.type == "numRolling") {
                        if (animProps.containsKey("interval") && animProps["interval"] !is kotlinx.serialization.json.JsonPrimitive) {
                            errors.add("动画属性 'interval' 必须是数字数值")
                        }
                    }
                    if (spec.type == "marquee") {
                        if (animProps.containsKey("pauseOnTouch") && animProps["pauseOnTouch"] !is kotlinx.serialization.json.JsonPrimitive) {
                            errors.add("动画属性 'pauseOnTouch' 必须是布尔值")
                        }
                    }
                }
            }
            anim.loop?.let { spec ->
                if (spec.type !in VALID_ANIM_TYPES) errors.add("循环动画类型 '${spec.type}' 无效")
            }
        }

        // 检查交互行为层 (Action)
        node.action?.forEach { (trigger, action) ->
            if (action.type !in VALID_ACTION_TYPES) {
                errors.add("交互 '$trigger' 的 type '${action.type}' 非法。建议: $VALID_ACTION_TYPES")
            }
        }

        // 如果发现错误，记入报告
        if (errors.isNotEmpty()) {
            report[node] = errors
        }

        // 递归处理子节点
        node.children?.forEach { validate(it, report) }

        return report
    }

    private fun checkEnum(props: Map<String, kotlinx.serialization.json.JsonElement>, key: String, validSet: Set<String>, errors: MutableList<String>) {
        val value = props[key]?.toString()?.replace("\"", "")?.lowercase() ?: ""
        if (value.isNotEmpty() && value !in validSet) {
            errors.add("属性 '$key' 的取值 '$value' 无效。可选范围: ${validSet.joinToString(" | ")}")
        }
    }
}
