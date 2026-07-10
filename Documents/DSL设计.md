# DSL 节点

```
{
  "type": "组件类型",    // 必填: Column | Row | Box | Text | Image | Button
  "id": "唯一ID",       // 可选
  "style": { ... },           // 可选: 通用样式
  "children": [ ... ],        // 仅容器组件有(Column/Row/Box): 子节点列表
  "props": { ... },           // 可选: 组件特有属性
  "state": { ... },           // 可选: 生存状态属性
  "action": { ... }           // 可选: 交互行为属性
}
```

# 节点下style字段

```
"style": {
  // 布局尺寸
  "width": "match_parent",           // match_parent | wrap_content | 300.0 (dp数值使用字符串)
  "height": "wrap_content",          // 同上

  // 边距
  "padding": {                       // 内边距, 单位 dp
    "top": 16,
    "bottom": 16,
    "left": 20,
    "right": 20
  },
  "margin": {                        // 外边距, 单位 dp
    "top": 8,
    "bottom": 8,
    "left": 0,
    "right": 0
  },

  // 背景
  "backgroundColor": "#FFFFFF",      // #RRGGBB 或 #AARRGGBB (优先级低于 backgroundGradient)
  "backgroundGradient": {            // 渐变色，优先级高于 backgroundColor
    "direction": "vertical",         // vertical | horizontal | diagonal
    "colors": ["#1A1A2E", "#2D2D44"]
  },

  // 圆角
  "borderRadius": 12,                // 圆角, 单位 dp (支持数字或四个角独立对象)

  // 子元素在其父容器内的对齐方式
  "alignment": "center"              // Column: start|center|end 
                                     // Row: top|center|bottom 
                          // Box: "center" | "top-left" | top-right" | "bottom-left" |"bottom-right"
}
```

# 节点下props字段

## Text

```
"props": {
  "text": "Hello World",           // 文字内容
  "fontSize": 16,                  // 字号, 单位 sp
  "textColor": "#333333",          // 文字颜色, #RRGGBB
  "fontWeight": "bold",            // normal | medium | bold
  "textDecoration": "underline",   // line-through | underline
  "lineHeight": 24                 // 行高, 单位 sp
}
```

## Button

```
"props": {
  "text": "string",                  // 按钮文字
  "fontSize": 14,                 // 字号(sp)
  "textColor": "#FFFFFF"          // 文字颜色
  "icon": "res://ic_check",         // 可选: 图标地址
}
```

## Image

```
"props": {
  "url": "https://example.com/photo.jpg",  // 图片地址
  "contentScale": "crop"                   // crop | fit | fill（裁剪 | 适应 | 拉伸填充）
}
```



# 节点下animation字段

```
 "animation":{
  	"enter": {  			// 入场动画（组件首次出现时）
  		// 动画定义对象（核心结构）
    	"type": "fadeIn",        // 动画类型 [必填] 
    						   // [fadeIn | scaleIn | slideInUp | slideInDown | NumRolling]
    						   
  	  	"duration": 300,         // 动画时长(ms) [可选]
      	"delay": 0,              // 延迟开始时间(ms) [可选]
      	"easing": "linear",     // 缓动曲线 [可选]。[linear | easeIn | easeOut | easeInOut]
      	"repeatCount": 0,        // 重复次数 [可选]。[0=不重复，-1=无限循环]
      	"repeatMode": "restart", // 重复模式 [可选]。[restart | reverse]
    },        
  	"exit": { ... },         // 退场动画[可选]（组件移除时）[先不做]
  	"onTap": { ... },        // 点击动画[可选]（交互反馈）[先不做]
  	"loop": { ... }          // 循环动画[可选]（持续播放，如加载转圈）[先不做]
  },
  
  "animation": {
  	"loop": {
    	"type": "marquee", // 轮播
    	"props": {
      		"pauseOnTouch": true,  // [新] 触摸/鼠标悬停时是否暂停
      		"direction": "left"    // 预留方向控制
    		}
  		}
	}
```



# 节点下state字段

```
"state": {
  "visible": "{isLoggedIn}",       // 布尔值或表达式，控制显隐
  "enabled": true                  // 是否可交互
}
```

# 节点下action字段

```
"action": {
  "onTap": {                               // 触发方式：点击事件
    "type": "navigate",                    // 动作类型：页面跳转
      // 可选值: navigate(跳转) | back(返回) | dismiss(关闭) | showToast(提示) | custom(自定义)
    
    "target": "capcut://member_center",    // 跳转目标URL
  }
}
```

# 总结

```
{
  // 基础节点结构  
  {
    "type": "Unknown", 组件基础身份定义[必填]。[Box | Column | Row | Text | Image | Button | Unknown]
    "id": "", 唯一识别码[可选]
    "style": "...", 定义组件的视觉属性[可选] [盒模型样式层]
    "props": "...", 定义组件的特有属性[可选] [业务属性层]
    "state": "...", 控制组件在运行时的生存状态[可选] [逻辑状态层]
    "animation":"..." 定义组件的动画效果[可选] [动效表现层]
    "action": "...", 定义用户操作后触发的指令映射[可选] [交互行为层]
    "children": "..." 存放当前容器组件内部包含的子节点列表[可选] [结构嵌套层]
  },
  
 // 盒模型样式层
  "style": {
    "width": "wrap_content", // 控件宽度[可选]。[match_parent | wrap_content | 具体数字(dp)]
    "height": "wrap_content", // 控件高度[可选]。[match_parent | wrap_content | 具体数字(dp)]
    "backgroundColor": "Transparent", // 背景填充颜色[可选]。[#RRGGBB | #AARRGGBB | Transparent(透明)]
    "backgroundImage": "", // 背景图片[可选]。[网络URL | res://本地资源]
    
    "borderRadius": 0, // 盒子的圆角弧度[可选]。
    				 // [数字(dp) | 对象{top-left,top-right,bottom-left,bottom-right}]
    				 
    "padding": 0, // 组件内边距[可选]。[数字 | 对象{top,right,bottom,left}]
    "margin": 0, // 组件外边距[可选]。[数字 | 对象{top,right,bottom,left}]
    
    "alignment": "center", // 对齐策略[可选]。
    					// [Column: start | center | end] 
                          // [Row: top | center | bottom] 
                          // [Box:  center
                          //	 top-left | top-center | top-right | 
                          //	 bottom-left | bottom-center| bottom-right]
    
    "weight": "0.0" // 布局权重[可选]。[浮点数字符串, 如 "1.0"]
  },

  // 业务属性层
  "props": {
    "Text": {
      "text": "Missing Text", // 显示的文字内容[必填]
      "fontSize": "14", // 文本的字号大小(sp)[可选]
      "textColor": "", // 文本的颜色值[可选]。
      "fontWeight": "normal", // 文字的粗细程度[可选]。[normal | medium | bold]
      "textDecoration": "none", // 文字修饰效果[可选]。[none | underline | line-through]
      "lineHeight": 0 // 文本的行高[可选]。[数字(sp)]
    },
    
    "Image": {
      "url": "", // 图片的获取来源[必填]。[URL | res://资源名]
      "contentScale": "fit", // 图片的缩放适配模式[可选][crop(裁剪) | fit(适应) | fill(拉伸)]
    },
    
    "Button": {
      "text": "", // 按钮上显示的标签文字[必填]。
      "fontSize": 16, // 按钮文字的大小[可选]。[数字(sp)]
      "textColor": "#FFFFFF", // 按钮文字的颜色[可选]。[十六进制格式]
      "icon": "", // 按钮文字旁的图标路径[可选]。[res://资源名]
    },
    
    "Column": {
      "spacing": 0 // 容器内子组件之间的固定间距[可选]。[数字(dp)]
    },
    
    "Row": {
      "spacing": 0 // 容器内子组件之间的固定间距[可选]。[数字(dp)]
    }
  },
  
  // 动效表现层
  "animation":{
  	"enter": {  			// 入场动画（组件首次出现时）
  		// 动画定义对象（核心结构）
    	"type": "fadeIn",        // 动画类型 [必填] 
    						   // [fadeIn | scaleIn | slideInUp | slideInDown | numRolling]
    						   
  	  	"duration": 300,         // 动画时长(ms) [可选]
      	"delay": 0,              // 延迟开始时间(ms) [可选]
      	"easing": "linear",     // 缓动曲线 [可选]。[linear | easeIn | easeOut | easeInOut]
      	"repeatCount": 0,        // 重复次数 [可选]。[0=不重复，-1=无限循环]
      	"repeatMode": "restart", // 重复模式 [可选]。[restart | reverse]
    },        
  	"exit": { ... },         // 退场动画[可选]（组件移除时）[未做]
  	"onTap": { ... },        // 点击动画[可选]（交互反馈）[未做]
  	"loop": {				// 循环动画[可选]
         "type": "marquee", // 动画类型 [必填] [marquee]
         "duration": 3000,
         "props": {
         "pauseOnTouch": true,
         "direction": "left"
         }
   }          
  },
  
  // 逻辑状态层
  "state": {
    "visible": "true", // 控制组件是否在屏幕上渲染[可选]。[true | false]
    "enabled": true // 控制组件是否允许交互[可选]。[true | false]
  },
  
  // 交互行为层
 "action": {
    "onTap": {...}, // 用户点击时触发的指令[可选]。[动作对象 | 动作对象数组]
    // onTap内部结构
    {
      "type": "id", // 动作的分类指令[必填]。[navigate(跳转) | back | dismiss | showToast | id]
      "target": "" // 动作的目标参数[可选]。[路径URL | Toast文本 | 动作ID] 
    }
  }
}
```

