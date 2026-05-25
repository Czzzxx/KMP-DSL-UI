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
    fun testUnknownNodeFallback() {
        val unknownJson = """
            {
                "type": "NewCoolComponent",
                "props": { "foo": "bar" },
                "styles": { "backgroundColor": "#00FF00" }
            }
        """.trimIndent()

        val node = json.decodeFromString<UiNode>(unknownJson)
        assertIs<UnknownNode>(node)
    }
}
