## 第一步：

### 1.创建 :shared 模块：

建立了标准 KMP 结构：commonMain (共享逻辑), androidMain, iosMain。
配置了 shared/build.gradle.kts，引入了 Kotlin Multiplatform 和 Android Library 插件。

### 2.配置依赖环境：

在 libs.versions.toml 中添加了 kotlinx-serialization (JSON 解析) 和 KMP 相关插件。
执行了 Gradle Sync，确保工程结构已被 Android Studio 正确识别。

## 第二步：

### 定义初步 IR 模型：

在 shared/src/commonMain/kotlin/com/example/capcut_project/Models.kt 中创建了基础的 UiNode 树结构（Box, Column, Row, Text, Image），并集成了 @Serializable 注解，为后续 JSON 解析做准备。

在目前的 shared/src/commonMain/kotlin/com/example/capcut_project/Models.kt 文件中，定义了一套 IR（Intermediate Representation，中间表示）模型。
之所以叫 IR，是因为它是连接“原始 JSON 字符串”和“最终 Compose 界面”的桥梁。
下面是这些模型的具体构成和它们的作用：

1. **核心基类：UiNode**
  这是一个 sealed class（密封类）。在 Kotlin 中，密封类像是一个“增强版枚举”，它定义了 UI 树中所有节点的通用特征：
  type: 节点的类型名称（如 "Box", "Text"）。
  styles: 样式表（Map<String, String>）。比如 {"color": "#FF0000", "fontSize": "16"}。
  props: 属性表（Map<String, String>）。存放一些非样式的配置，比如 {"id": "submit_btn"}。
  children: 子节点列表。通过这个列表，你可以把节点嵌套起来，形成一棵树。

2. **基础组件模型**
  实现了最核心的 6 个组件模型：
  Box: 基础容器，支持子节点堆叠。
  Column: 垂直布局容器。
  Row: 水平布局容器。
  Text: 文本组件，包含 text 字段。
  Image: 图片组件，包含 url 字段。
  Button: 按钮组件，包含 text（文字）和 actionId（点击事件 ID）。

  **样式能力：**

  尺寸与布局 (Size & Layout)
  	width: 宽度（如 "100", "fill", "wrap"）。
  	height: 高度（如 "50", "fill", "wrap"）。
  	padding: 内边距（支持单值 "16" 或多值 "10,20,10,20"）。
  	margin: 外边距（规则同上）。
  颜色与形状 (Color & Shape)
  	backgroundColor: 背景颜色（如 "#FFFFFF", "#FF2C55"）。
  	borderRadius: 圆角大小（如 "8"）。

   文本样式 (Typography)
  	textColor: 文字颜色（如 "#333333"）。
  	fontSize: 字号大小（如 "14", "18"）。
  	fontWeight: 字体粗细（支持 "Normal", "Bold"）。
   对齐方式 (Alignment)
  	horizontalAlignment: 水平对齐（"Start", "Center", "End"）
  	verticalAlignment: 垂直对齐（"Top", "Center", "Bottom"）

  **扩展能力**
  	props: 每个组件都带有一个 Map<String, String>?，用于存放不属于通用样式的特殊业务属性。
  	多态解析: 已经通过 @SerialName 实现了 JSON 自动识别类型，你可以直接给解析器一段带有 "type": "Button" 的 JSON，它就能自动转成 ButtonNode 对象。

  

3. 模型里的关键点（为什么这么设计？）
  @Serializable 注解： 这是为了配合 kotlinx-serialization 库使用的。有了它，我们以后只需要一行代码：Json.decodeFromString<UiNode>(jsonString)，就能把后端的 JSON 变成这些 Kotlin 对象。

  **用库把 JSON 字符串 "解释" (解析/转换) 成一个 Kotlin 对象 (实例)。**

  

4. 类型安全： 比如 TextNode 必须有 text 字段，如果你解析的 JSON 里 Text 组件没给文字，解析器就会报错。这比直接操作原始 JSON 对象要安全得多。

5. 递归嵌套： 因为 children 又是 List<UiNode>，所以你可以无限嵌套。比如 Column 嵌套 Row，Row 嵌套 Text。

## 第三步

**构建一个最简易的JSON 格式的 DSL（领域特定语言）**，用于描述一个**订阅挽留弹窗的 UI 界面**

┌─────────────────────────────────┐
│                                 │
│                             🖼️ 图片                                          │  
│                                 │	
│                 确认放弃 7 天免费试用吗？                      	│  ← 主标题
│                                 │
│    开通即可解锁 100+ 专业特效、无水印导出等权益   │  
│                                                                                  	│
│                                                                                  	│
│  ┌─────────────────────────────┐	│
│  │                         继续试用                                   │	│  ← 红色按钮（主要操作）
│  └─────────────────────────────┘	│
│  ┌─────────────────────────────┐	│
│  │                           放弃权益                                  │	│  ← 灰色按钮（次要操作）
│  └─────────────────────────────┘	│
│                                													 │
└─────────────────────────────────┘

## 第四步

1.渲染映射引擎 (Renderer)：在 app 模块中实现了 DynamicRenderer.kt。它能完美解析UiStyle并映射为Compose 的 Modifier，支持圆角、背景色、内边距、外边距、对齐等全部 12 种样式。
2.多态组件支持：支持 Box, Column, Row, Text, Image, Button 的递归渲染。
3.Coil 集成：已接入 Coil 库，支持 JSON 中定义的图片 URL 异步加载。
4.Assets 加载：MainActivity 现在会自动读取 assets/retain_popup.json 并渲染出挽留弹窗界面。

![image-20260524205543882](C:\Users\Czzzxx\AppData\Roaming\Typora\typora-user-images\image-20260524205543882.png)







