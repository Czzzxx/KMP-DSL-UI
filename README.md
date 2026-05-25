# CapCut-Project: 跨端动态 UI 渲染引擎 (SDUI)

本项目构建了一个完整的Server-Driven UI闭环系统，支持通过下发 JSON 协议实时控制 App 的界面布局、业务逻辑与视觉样式。

1. ### 动态组件库 (Component Library)

预置了 7 套标准组件：
布局容器：Box (堆叠)、Column (垂直)、Row (水平)。
基础内容：Text (文本)、Image (图片)。
交互控件：Button (按钮)、IconButton (图标按钮)。
业务扩展：支持 BenefitItem 等自定义业务插槽。

2. ### 精细化样式系统 (Styling System)

每个组件均支持多维度的样式配置：
尺寸控制：支持数值 ("100")、弹性 ("fill")、包裹 ("wrap")。
间距装饰：支持单值或 4 值格式的 padding 与 margin (e.g., "10,20,10,20")。
视觉细节：背景色解析、文本颜色、字号调节、字体加粗、圆角半径控制。
对齐体系：支持 9 宫格对齐（Start, Center, End / Top, Center, Bottom）。

3. ### 表达式引擎 (Logic Engine)

这是系统的“大脑”，实现了从“静态模板”到“动态逻辑”的跨越：

深路径读取：支持 {user.profile.name} 这种多层嵌套数据的精准抓取。
可见性控制：任何组件都拥有 visible 属性，通过 {user.isVip} 等表达式实现条件渲染。
三元运算符：支持在文案中直接写逻辑：{user.balance > 0 ? '立即续费' : '免费试用'}。
数据隔离：支持 Data 与 UI 的解耦，同一套 UI 模板可适配不同的业务数据集。

4. ### 稳定性与配置错误纠错 (Robustness)

结构级降级：若组件名写错或缺失必填项，系统自动将其转化为 UnknownNode，并在屏幕上显示红色占位符与报错原因，确保其他组件正常加载。
样式级纠错：若颜色或枚举值配置非法，组件右侧会浮现 黄色感叹号，点击可实时查看具体的配置建议，实现“边写 JSON 边调试”的效果。

5. ### 交互与资源协议

Action 映射：通过 actionId 建立 DSL 与 Android 原生能力的桥梁（如跳转、关闭页面、弹出 Toast）。
智能路径解析：支持 http 网络图与 android_asset 本地素材的自动混部与识别。

## 技术架构 (Technical Stack)

跨端层 (KMP)：使用纯 Kotlin 定义 IR (中间表示) 模型，确保 UI 协议在多端的一致性。
解析层 (Serialization)：深度定制多态解析器，实现组件级别的 try-catch 解析拦截。
渲染层 (Jetpack Compose)：利用响应式 UI 框架的高性能特性，将逻辑树实时映射为 GPU 加速的视图。
图片引擎 (Coil)：异步加载网络与 Assets 资源，支持图片圆角与遮罩。
DSL 协议结构示例
JSON
{
  "data": { "user": { "name": "Andy", "isVip": true } },
  "ui": {
 "type": "Column",
   "children": [
     {
        "type": "Text",
        "text": "欢迎回来, {user.name}",
        "styles": { "fontSize": "20", "fontWeight": "Bold" }
      },
      {
        "type": "Button",
        "visible": "{user.isVip}",
        "text": "进入 VIP 专区",
        "actionId": "goto_vip"
      }
    ]
     }
   }
   
  

开发记录摘要
1.阶段一：完成 KMP 环境搭建与 UiNode 模型多态解析。
2.阶段二：实现 DynamicRenderer 渲染引擎与多样式的 Modifier 映射。
3.阶段三：攻克正则表达式驱动的脚本引擎，支持深路径读取与逻辑判断。
4.阶段四：构建错误实时提示系统。