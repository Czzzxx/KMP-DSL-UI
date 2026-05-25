package com.example.capcut_project

import kotlinx.serialization.json.*

/**
 * 表达式计算器：处理 {user.name} 类似的路径读取及逻辑判断。
 */
object ExpressionEvaluator {

    /**
     * 解析并计算表达式结果
     */
    fun evaluate(expression: String?, context: JsonObject): Any? {
        val expr = expression ?: return null
        
        // 如果不包含大括号，直接返回原字符串
        if (!expr.contains("{") || !expr.contains("}")) {
            return expr
        }

        // 检查是否是单一完整表达式，例如 "{user.isVip}"
        val singleExprRegex = Regex("^\\s*\\{(.+?)\\}\\s*$")
        val match = singleExprRegex.find(expr)
        
        if (match != null) {
            val path = match.groupValues[1].trim()
            // 特殊逻辑：简单三元表达式支持 {a ? b : c}
            if (path.contains("?")) {
                return evaluateTernary(path, context)
            }
            return getValueByPath(path, context)
        }

        // 字符串插值替换
        var resultString = expr
        val interpolationRegex = Regex("\\{(.+?)\\}")
        interpolationRegex.findAll(expr).forEach { m ->
            val path = m.groupValues[1].trim()
            val value = getValueByPath(path, context)
            resultString = resultString.replace(m.value, value?.toString() ?: "")
        }
        return resultString
    }

    /**
     * 简单的三元表达式解析: {condition ? trueVal : falseVal}
     */
    private fun evaluateTernary(path: String, context: JsonObject): Any? {
        val parts = path.split("?")
        if (parts.size != 2) return null
        
        val conditionPath = parts[0].trim()
        val results = parts[1].split(":")
        if (results.size != 2) return null
        
        val conditionValue = getValueByPath(conditionPath, context)
        val isTrue = when (conditionValue) {
            is Boolean -> conditionValue
            is String -> conditionValue.lowercase() != "false" && conditionValue.isNotEmpty()
            null -> false
            else -> true
        }
        
        val resultPath = if (isTrue) results[0].trim() else results[1].trim()

        if ((resultPath.startsWith("'") && resultPath.endsWith("'")) || 
            (resultPath.startsWith("\"") && resultPath.endsWith("\""))) {
            return resultPath.substring(1, resultPath.length - 1)
        }
        
        return getValueByPath(resultPath, context) ?: resultPath
    }

    private fun getValueByPath(path: String, context: JsonObject): Any? {
        val parts = path.split(".")
        var current: JsonElement = context
        
        for (part in parts) {
            if (current !is JsonObject) return null
            current = current[part] ?: return null
        }

        return when (current) {
            is JsonPrimitive -> {
                if (current.isString) current.content
                else current.booleanOrNull ?: current.doubleOrNull ?: current.intOrNull ?: current.content
            }
            else -> current.toString()
        }
    }

    fun isTruthy(expression: String?, context: JsonObject): Boolean {
        if (expression == null) return true
        val value = evaluate(expression, context)
        return when (value) {
            null -> false
            is Boolean -> value
            is Number -> value.toDouble() != 0.0
            is String -> value.lowercase() != "false" && value.isNotEmpty()
            else -> true
        }
    }
}
