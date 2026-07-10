package com.example.capcut_project

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.capcut_project.ui.LocalUiErrors
import com.example.capcut_project.ui.UiValidator
import com.example.capcut_project.ui.model.UiNode
import com.example.capcut_project.ui.renderer.DynamicRenderer
import kotlinx.serialization.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val jsonFormatter = Json {
        ignoreUnknownKeys = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            var currentDsl by remember { mutableStateOf("subscription_vip") }
            var overlayDsl by remember { mutableStateOf<String?>(null) }
            var isNewUser by remember { mutableStateOf(true) }

            // 创建动作处理器
            val actionHandler = remember {
                ActionHandler(
                    context = this,
                    onUpdateCurrentDsl = { currentDsl = it },
                    onUpdateOverlayDsl = { overlayDsl = it },
                    onUpdateUserStatus = { isNewUser = it },
                    getCurrentDsl = { currentDsl }
                )
            }
            
            //将数据解析移出UI主线程
            val mainDslState = produceState<Triple<UiNode?, JsonObject?, Map<UiNode, List<String>>>?>(initialValue = null, currentDsl, isNewUser) {
                value = loadDsl(currentDsl, isNewUser)
            }

            val overlayDslState = produceState<Triple<UiNode?, JsonObject?, Map<UiNode, List<String>>>?>(initialValue = null, overlayDsl, isNewUser) {
                value = overlayDsl?.let { loadDsl(it, isNewUser) }
            }

            val mainData = mainDslState.value
            val overlayData = overlayDslState.value

            Box(modifier = Modifier.fillMaxSize()) {
                //订阅页面
                if (mainData != null && mainData.first != null) {
                    CompositionLocalProvider(LocalUiErrors provides mainData.third) {
                        DynamicRenderer(
                            node = mainData.first!!, 
                            context = mainData.second ?: buildJsonObject {},
                            onAction = { actionId -> actionHandler.handle(actionId) }
                        )
                    }
                }

                // 2.挽留弹窗
                if (overlayDsl != null && overlayData != null && overlayData.first != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .then(Modifier.clickable(enabled = false) {})
                    )
                    
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        CompositionLocalProvider(LocalUiErrors provides overlayData.third) {
                            DynamicRenderer(
                                node = overlayData.first!!, 
                                context = overlayData.second ?: buildJsonObject {},
                                onAction = { actionId -> actionHandler.handle(actionId) }
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadDsl(name: String, isNewUser: Boolean): Triple<UiNode?, JsonObject?, Map<UiNode, List<String>>>? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = assets.open("DSL/$name.json").bufferedReader().use { it.readText() }
                val jsonElement = jsonFormatter.parseToJsonElement(jsonString).jsonObject
                val originalContext = jsonElement["data"]?.jsonObject ?: buildJsonObject {}

                val context = buildJsonObject {
                    originalContext.forEach { (k, v) -> put(k, v) }

                    if (originalContext.containsKey("user") && originalContext["user"] is JsonObject) {
                        val userObj = originalContext["user"]!!.jsonObject
                        put("user", buildJsonObject {
                            userObj.forEach { (k, v) -> put(k, v) }
                            put("is_new_user", JsonPrimitive(isNewUser))
                        })
                    } else {
                        put("is_new_user", JsonPrimitive(isNewUser))
                        put("user", buildJsonObject { put("is_new_user", JsonPrimitive(isNewUser)) })
                    }
                }

                val ui = jsonElement["ui"] ?: throw IllegalArgumentException("JSON 缺少 'ui' 根节点")
                val node = jsonFormatter.decodeFromJsonElement<UiNode>(ui)
                
                val errorMap = UiValidator.validate(node)
                Triple(node, context, errorMap)
            } catch (e: Exception) {
                android.util.Log.e("DSL_LOADER", "加载 $name 失败: ${e.message}")
                null
            }
        }
    }
}
