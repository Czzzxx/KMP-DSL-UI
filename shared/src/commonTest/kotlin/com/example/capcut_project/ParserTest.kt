package com.example.capcut_project

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ParserTest {
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    @Test
    fun testButtonDeserialization() {
        val buttonJson = """
            {
                "type": "Button",
                "text": "Click Me",
                "actionId": "submit",
                "styles": {
                    "backgroundColor": "#FF0000",
                    "borderRadius": "8",
                    "padding": "12"
                }
            }
        """.trimIndent()

        val node = json.decodeFromString<UiNode>(buttonJson)
        assertIs<ButtonNode>(node)
        assertEquals("Click Me", node.text)
        assertEquals("submit", node.actionId)
        assertEquals("#FF0000", node.styles?.backgroundColor)
    }

    @Test
    fun testComplexLayoutDeserialization() {
        val layoutJson = """
            {
                "type": "Column",
                "styles": { "horizontalAlignment": "Center" },
                "children": [
                    {
                        "type": "Text",
                        "text": "Title",
                        "styles": { "fontSize": "20", "fontWeight": "Bold" }
                    },
                    {
                        "type": "Button",
                        "text": "Action",
                        "styles": { "marginTop": "10" }
                    }
                ]
            }
        """.trimIndent()

        val node = json.decodeFromString<UiNode>(layoutJson)
        assertIs<ColumnNode>(node)
        assertEquals(2, node.children?.size)
        assertIs<TextNode>(node.children?.get(0))
        assertIs<ButtonNode>(node.children?.get(1))
    }
}
