package com.example.capcut_project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    private val jsonFormatter = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val jsonString = assets.open("retain_popup.json").bufferedReader().use { it.readText() }
        val rootNode = jsonFormatter.decodeFromString<UiNode>(jsonString)

        setContent {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DynamicRenderer(rootNode)
            }
        }
    }
}
