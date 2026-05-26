package com.example.capcut_project

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.*

class MainActivity : ComponentActivity() {
    private val jsonFormatter = Json {
        ignoreUnknownKeys = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            // 状态机：控制当前显示哪套会员体系
            var currentDsl by remember { mutableStateOf("retain_popup_vip") }
            
            // 响应式加载：当 currentDsl 变化时，自动重读文件
            val rootAndContext = remember(currentDsl) {
                val jsonString = assets.open("DSL/$currentDsl.json").bufferedReader().use { it.readText() }
                val jsonElement = jsonFormatter.parseToJsonElement(jsonString).jsonObject
                val context = jsonElement["data"]?.jsonObject ?: buildJsonObject {}
                val ui = jsonElement["ui"] ?: throw IllegalArgumentException("JSON 缺少 'ui' 根节点")
                val node = jsonFormatter.decodeFromJsonElement<UiNode>(ui)
                node to context
            }

            Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.TopCenter) {
                DynamicRenderer(
                    node = rootAndContext.first, 
                    context = rootAndContext.second,
                    onAction = { actionId ->
                        // 处理页面切换逻辑
                        when (actionId) {
                            "switch_vip" -> currentDsl = "retain_popup_vip"
                            "switch_svip" -> currentDsl = "retain_popup_svip"
                            "switch_ssvip" -> currentDsl = "retain_popup_ssvip"
                            else -> handleAction(actionId)
                        }
                    }
                )
            }
        }
    }

    private fun handleAction(actionId: String) {
        when (actionId) {
            "keep_trial" -> Toast.makeText(this, "恭喜，已成功开通！✨", Toast.LENGTH_SHORT).show()
            "cancel_trial" -> finish()
            else -> Toast.makeText(this, "动作: $actionId", Toast.LENGTH_SHORT).show()
        }
    }
}
