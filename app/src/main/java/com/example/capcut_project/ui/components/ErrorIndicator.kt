package com.example.capcut_project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup

/**
 * 样式错误提示组件
 */
@Composable
fun StyleErrorIndicator(messages: List<String>) {
    var showTooltip by remember { mutableStateOf(false) }
    Box(modifier = Modifier.padding(start = 4.dp)) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(Color(0xFFFFD600), CircleShape)
                .clickable { showTooltip = !showTooltip }, 
            contentAlignment = Alignment.Center
        ) {
            Text(text = "!", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        }
        if (showTooltip) {
            Popup(alignment = Alignment.TopStart, onDismissRequest = { showTooltip = false }) {
                Box(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .border(1.dp, Color.Red, RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .padding(8.dp)
                ) {
                    Column {
                        Text(text = "样式配置错误:", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        messages.forEach { msg -> Text(text = "• $msg", color = Color.Red, fontSize = 10.sp) }
                    }
                }
            }
        }
    }
}
