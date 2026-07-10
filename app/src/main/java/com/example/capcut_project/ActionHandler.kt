package com.example.capcut_project

import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity

/**
 * ActionHandler - 业务动作中枢
 * 
 * 职责：解耦 DSL 触发的 UI 指令与具体的业务执行逻辑。
 */
class ActionHandler(
    private val context: Context,
    private val onUpdateCurrentDsl: (String) -> Unit,
    private val onUpdateOverlayDsl: (String?) -> Unit,
    private val onUpdateUserStatus: (Boolean) -> Unit,
    private val getCurrentDsl: () -> String
) {

    /**
     * 处理来自 DynamicRenderer 的动作 ID
     */
    fun handle(actionId: String) {
        when (actionId) {
            // VIP / SVIP页面切换
            "switch_svip" -> onUpdateCurrentDsl("subscription_svip")
            "switch_vip" -> onUpdateCurrentDsl("subscription_vip")

            //弹窗控制
            "show_retention_popup" -> {
                val current = getCurrentDsl()
                val targetOverlay = if (current == "subscription_svip") {
                    "retain_popup_svip"
                } else {
                    "retain_popup_vip"
                }
                onUpdateOverlayDsl(targetOverlay)
            }
            "show_mis_popup" -> onUpdateOverlayDsl("retain_popup_mis")
            "close_popup" -> onUpdateOverlayDsl(null)

            //业务状态切换
            "set_new_user" -> onUpdateUserStatus(true)
            "set_old_user" -> onUpdateUserStatus(false)

            //具体业务逻辑
            "pay_vip", "pay_svip", "keep_trial" -> {
                showToast("恭喜，已成功开通！✨")
                onUpdateOverlayDsl(null)
            }
            
            "cancel_trial" -> {
                (context as? ComponentActivity)?.finish()
            }

            else -> {
                // 默认处理或记录未知动作
                android.util.Log.d("ActionHandler", "未处理的动作 ID: $actionId")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
