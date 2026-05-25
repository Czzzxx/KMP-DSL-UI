package com.example.capcut_project

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.serialization.json.*

class MainActivity : ComponentActivity() {
    private val jsonFormatter = Json {
        ignoreUnknownKeys = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 1. 展示的 DSL
        val dslFileName = "retain_popup_pro" 

        // 2. 加载 DSL 文件内容
        val jsonString = assets.open("DSL/$dslFileName.json").bufferedReader().use { it.readText() }
        
        // 3. 核心：解析自包含的 JSON 结构
        val jsonElement = jsonFormatter.parseToJsonElement(jsonString).jsonObject
        
        // 从 "data" 字段提取业务上下文
        val businessContext = jsonElement["data"]?.jsonObject ?: buildJsonObject {}
        
        // 从 "ui" 字段提取 UI 节点树
        val uiElement = jsonElement["ui"] ?: throw IllegalArgumentException("JSON 缺少 'ui' 根节点")
        val rootNode = jsonFormatter.decodeFromJsonElement<UiNode>(uiElement)

        setContent {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DynamicRenderer(
                    node = rootNode, 
                    context = businessContext,
                    onAction = { actionId ->
                        handleAction(actionId)
                    }
                )
            }
        }
    }

    /**
     * 核心逻辑：将 DSL 中的 actionId 映射为 Android 原生动作
     */
    private fun handleAction(actionId: String) {
        when (actionId) {
            "keep_trial" -> {
                Toast.makeText(this, "欢迎继续使用剪映！✨", Toast.LENGTH_SHORT).show()
            }
            "cancel_trial" -> {
                Toast.makeText(this, "期待下次与你相遇", Toast.LENGTH_SHORT).show()
                //finish() // 关闭当前页面
            }
            else -> {
                Toast.makeText(this, "触发未知动作: $actionId", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
