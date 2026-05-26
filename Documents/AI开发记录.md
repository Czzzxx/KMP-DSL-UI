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

### **构建一个最简易的JSON 格式的 DSL（领域特定语言）**

用于描述一个**订阅挽留弹窗的 UI 界面**

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

### 渲染映射

1.渲染映射引擎 (Renderer)：在 app 模块中实现了 DynamicRenderer.kt。它能完美解析UiStyle并映射为Compose 的 Modifier，支持圆角、背景色、内边距、外边距、对齐等全部 12 种样式。
2.多态组件支持：支持 Box, Column, Row, Text, Image, Button 的递归渲染。
3.Coil 集成：已接入 Coil 库，支持 JSON 中定义的图片 URL 异步加载。
4.Assets 加载：MainActivity 现在会自动读取 assets/retain_popup.json 并渲染出挽留弹窗界面。

![image-20260524205543882](C:\Users\Czzzxx\AppData\Roaming\Typora\typora-user-images\image-20260524205543882.png)

## 第五步

### 配置解析与校验

1.引入 UnknownNode（降级节点）：
在 Models.kt 中增加了一个特殊的节点。如果 JSON 里的 type 是你不认识的（比如你写的 Text111），系统会自动把它识别为 UnknownNode。

2.编写了“智能解析器” (UiNodeSerializer)：
这个解析器在解析每一行 JSON 时都会进行“安检”。
它会检查 type 字段，如果不在我们的 Box, Text, Image, Button 名单里，它会优雅地返回一个 UnknownNode，而不是直接抛出异常让 App 闪退。

3.增加了“颜色安全检查” (safeParseColor)：
如果有人在 JSON 里写了非法的颜色（比如 "backgroundColor": "呵呵"），渲染器会捕获这个错误并降级为透明色，确保 parseColor 不会崩溃。

4.实现了“错误占位 UI”：
在 DynamicRenderer.kt 中为 UnknownNode 专门画了一个红色的“未知组件”提示框。

结构类型校验

发生阶段：JSON 反序列化阶段（在 Models.kt 的 UiNodeSerializer 中）
触发条件：
组件 type 写错了（如 "Text111"）。
组件必填字段丢了（如 Text 没写 "text"）。
处理结果：生成一个 UnknownNode (未知组件)。

用户视觉：界面上会出现一个深红色的占位块，写着“❌ 未知组件：[错误原因]”。

逻辑用意：因为整个组件的“骨架”都坏了，系统无法猜测你想画什么，所以直接显示报错块。

第二层防御：属性/样式校验 (Property Level)
发生阶段：Compose 渲染阶段（在 DynamicRenderer.kt 的 applyUiStyle 中）

触发条件：
颜色格式不对（如 "textColor": "呵呵"）。
枚举值填错（如 "horizontalAlignment": "111"）。
数值单位不对（如宽度填了字母）。
处理结果：组件照常渲染（使用保底默认样式，比如黑色文字），同时将错误塞进 localErrors 列表。
用户视觉：组件的右边会出现一个黄色的小感叹号 !，点击即可看到具体的样式报错。
逻辑用意：因为组件的“骨架”是好的，只是“衣服”穿错了。为了不破坏整体布局，我们让它用默认样式显示，并提示开发者去修改样式。
总结流程图：
1.输入 JSON
2.解析器安检：
	骨架坏了？👉 生成 UnknownNode 👉 显示红色大报错。
	骨架好的？👉 生成对应 Node (如 TextNode) 👉 进入下一步。
3.渲染器施工：
	发现颜色/对齐写错了？👉 记录错误 👉 显示原组件 + 黄色感叹号。
全都写对了？👉 显示完美组件。

必填字段

![image-20260525125129834](C:\Users\Czzzxx\AppData\Roaming\Typora\typora-user-images\image-20260525125129834.png)

错误实例展示：

![image-20260525125521746](C:\Users\Czzzxx\AppData\Roaming\Typora\typora-user-images\image-20260525125521746.png)



## 第六步

### 点击交互系统

目标：将 DSL (JSON) 中定义的抽象 actionId 映射为 Android 原生的功能逻辑，实现端侧交互闭环。

1. 核心设计思路
采用 “观察者模式 + 动作解耦” 架构：
协议层 (DSL)：只负责声明“我想做什么”（例如："actionId": "keep_trial"）。
引擎层 (Renderer)：负责将点击信号透传，不参与具体的业务逻辑。
宿主层 (App)：负责监听信号并执行真正的 Android 原生代码（如 Toast、跳转、关闭页面）。



2. 关键代码实现流程
    1.接口注入：在 DynamicRenderer 函数中增加高阶函数参数 onAction: (String) -> Unit，建立跨层级的通信桥梁。
    2.组件绑定：
    在渲染 ButtonNode 和 IconButtonNode 时，将 Compose 的 onClick 回调指向 onAction(node.actionId)。
    通过递归调用，确保嵌套在任何层级的按钮点击都能成功上报。

  

3. 原生映射 (MainActivity)：
    在 Activity 中实现 handleAction(actionId: String) 方法。
    使用 when 语句进行 ID 匹配，从而触发不同的原生能力（如调用 Toast.makeText 或 Activity.finish()）。

  

4. 技术价值
    逻辑解耦：JSON 文件的维护者不需要懂 Kotlin 代码，只需按照约定的 ID 填入即可改变点击效果。
    安全性：即使 JSON 填入了一个不存在的 ID，App 也只会显示“未知动作”日志，而不会发生闪退，保证了线上稳定性。
    灵活性：未来新增功能（如“跳转会员页”）仅需在 Activity 中增加一个分支，无需改动底层的渲染引擎。

  

5. 落地场景验证
    场景：剪映挽留弹窗。
    验证：点击“继续试用”弹出欢迎语；点击“放弃权益”自动关闭弹窗页面。

## 第七步

### 动态内容注入

目标：打破 UI 模板的硬编码限制，实现 JSON 模板与实时业务数据的解耦。
1. 核心需求背景
在初始版本中，弹窗的文字（如“确认放弃 7 天免费试用吗？”）是写死在 JSON 里的。为了适应不同的营销活动（如 7 天变 15 天），我们需要一种能让 UI 模板在保持结构不变的同时，动态修改文案内容的能力。
2. 技术实现方案
我们在 Android App 模块中实现了一套轻量级的 “字符串插值 (String Interpolation)” 系统：
占位符协议：约定使用大括号 {key} 作为数据的标记符。例如："确认放弃 {free_days} 天"。
数据上下文注入：
在渲染入口处传入一个 Map<String, String> 类型的业务数据字典。
利用 Compose 渲染器的递归特性，将这套数据像“输血管”一样全量透传给每一个子节点。
字符串扩展函数 (bindData)：
实现：为 String 类型编写了 bindData 扩展方法，利用正则表达式遍历 Map，寻找并替换所有匹配的占位符。
鲁棒性：如果 Map 中找不到对应的 Key，系统会保留原有的 {key} 而非清空或报错，保证了容错性。
3. 核心应用点
文本绑定 (TextNode)：实现用户名称、折扣数值、天数等信息的个性化显示。
交互绑定 (ButtonNode)：让按钮文字（如“立即领取 {discount} 折扣”）随活动动态变化。
资源绑定 (ImageNode)：支持图片 URL 的动态化，可根据数据下发不同的 CDN 图片链接。
4. 技术价值
    内容灵活性：实现了“视图结构 (DSL)”与“具体内容 (Data)”的物理分离。
    维护成本降低：后端运营仅需通过数据接口下发少量 Key-Value 对，即可全局修改弹窗的核心信息，无需反复修改并下发庞大的 JSON UI 全量包。
    平滑过渡：该功能作为 V1 版本，成功验证了数据注入的链路，为后续更复杂的“脚本表达式引擎”奠定了基础。

## 第八步

### 表达式支持

目标：将系统从“静态占位符替换”升级为“具备逻辑运算能力”的脚本引擎，支持嵌套数据读取、布尔判断及三元表达式。
1. 核心需求背景
在真实的“剪映挽留弹窗”业务中，UI 并非一成不变。例如：
差异化显示：只有 VIP 用户才显示“续费权益”，普通用户显示“开通试用”。
动态控制：根据用户的剩余试用天数，实时计算并显示不同的文案。
可见性逻辑：某些勋章或图标需要根据后台数据字段动态隐藏。

2. 技术实现方案
    我们在 shared 共享层实现了名为 ExpressionEvaluator 的核心解析器，其逻辑分为三个层次：
    路径解析 (Path Traversal)：支持通过 . 号读取嵌套对象。
    实现：将 JsonObject 作为上下文，利用字符串分割 (split(".")) 实现递归查找，从而获取 user.profile.name 这种深层数据。
    逻辑判定 (Truthiness)：定义了一套跨类型的布尔准则。
    规则：将 true 字符串、非零数字、非空对象视为 真；将 null、"false" 字符串、数值 0 视为 假。用于驱动组件的 visible 属性。
    简单运算 (Ternary Support)：实现了类似 JavaScript 的三元运算符：{条件 ? 结果A : 结果B}。
    实现：利用正则表达式捕获 ? 和 : 符号，动态计算条件结果并返回对应的分支值。
3. 核心代码变更
    Models.kt (协议层)：在 UiNode 基类中新增 visible: String? 字段。所有子组件（Text, Button, Image 等）自动继承此能力，实现了“逻辑驱动可见性”。
    ExpressionEvaluator.kt (引擎层)：纯 Kotlin 实现的解析引擎。它不依赖任何 UI 框架，保证了跨平台的一致性。
    DynamicRenderer.kt (执行层)：在渲染每一个 Compose 组件前，新增了 “可见性守卫 (Visibility Guard)”。
    Kotlin
    if (!ExpressionEvaluator.isTruthy(node.visible, context)) return
4. 业务场景验证
    我们通过一份复杂的 JSON 进行了全功能验证，实现了以下效果：
    动态称呼：读取 user.username 显示“欢迎，剪映大玩家！”。
    条件显隐：当 user.isVip 为 true 时，右上角自动浮现“尊贵会员”金色标识；为 false 时标识完全不占位。
    智能文案：按钮文案通过三元表达式 {user.isVip ? '续费' : '开通'} 实现了根据身份自动切换。
5. 技术价值
    极致解耦：业务逻辑从硬编码迁移到了 DSL 配置中，真正实现了“逻辑随配置下发”。
    性能卓越：采用轻量级的正则匹配而非笨重的脚本解析库，在保证功能的前提下将解析耗时控制在毫秒级。
    架构专业性：从简单的“填空题”进化为“逻辑引擎”，标志着该 SDUI 框架达到了工业级应用的标准。

# 第九步

实现 VIP / SVIP / SSVIP 三套不同风格 UI 的无缝切换，通过一套渲染引擎驱动多套业务模板。
1. 核心需求背景
在真实的会员运营场景中，需要针对不同价值的用户展示不同的权益包。
多样化：需要支持红、金、黑三套截然不同的 UI 风格。
交互化：用户可以在弹窗内自由点击切换，实时对比不同等级的权益差异。
高性能：切换过程需无卡顿，且必须复用现有的渲染组件。
2. 技术实现方案
我们在 Android 宿主层引入了 “页面路由状态机” 机制：
状态驱动 (State-Driven)：
在 Activity 中使用 Compose 的 mutableStateOf 定义 currentDsl 变量。
将该变量作为渲染逻辑的“总开关”，实现 “数据变 -> UI 变” 的响应式效果。
3. 动态解耦加载（状态机）：
  利用 remember(currentDsl) 块实现按需加载。只有当路由标识发生变化时，系统才会从 Assets 读取对应的 JSON 文件并进行二次解析。
  Action 路由映射：
  在 DSL 的导航按钮中配置 switch_vip 等专用指令。
  Activity 层的 onAction 回调接收到指令后，不执行业务逻辑，而是修改路由状态，驱动整个 UI 树的重构。
4. UI 视觉重绘
  分段导航栏：在 UI 树顶部注入 Row 容器，通过样式配置区分当前激活态。
  三档视觉体系：
  VIP：剪映经典红，面向入门用户。
  SVIP：奢华流沙金，面向进阶用户。
  SSVIP：铂金拉丝黑，面向顶级专业用户。