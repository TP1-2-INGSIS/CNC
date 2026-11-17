package cnc.printscript.format

import kotlin.test.Test
import kotlin.test.assertEquals

class SourceFormatterTest {
    @Test
    fun `null or blank config returns source unchanged`() {
        val src = "let x:number=1;"
        assertEquals(src, SourceFormatter.format(src, null))
        assertEquals(src, SourceFormatter.format(src, ""))
    }

    @Test
    fun `invalid json returns source unchanged`() {
        val src = "let x:number=1;"
        assertEquals(src, SourceFormatter.format(src, "not json"))
    }

    @Test
    fun `spacing around equals forces single spaces preserving the rest`() {
        val src = "let x: number=1;"
        assertEquals("let x: number = 1;", SourceFormatter.format(src, """{"enforce-spacing-around-equals": true}"""))
    }

    @Test
    fun `no spacing around equals removes spaces preserving the rest`() {
        val src = "let x: number = 1;"
        assertEquals("let x: number=1;", SourceFormatter.format(src, """{"enforce-no-spacing-around-equals": true}"""))
    }

    @Test
    fun `spacing after colon leaves before colon intact`() {
        val src = "let twice_thing : string = \"x\";"
        assertEquals(
            "let twice_thing : string = \"x\";",
            SourceFormatter.format(src, """{"enforce-spacing-after-colon-in-declaration": true}"""),
        )
    }

    @Test
    fun `spacing before colon leaves after colon intact`() {
        val src = "let x:number = 1;"
        assertEquals(
            "let x :number = 1;",
            SourceFormatter.format(src, """{"enforce-spacing-before-colon-in-declaration": true}"""),
        )
    }

    @Test
    fun `space surrounding operations`() {
        val src = "let result: number = 5+4*3/2;"
        assertEquals(
            "let result: number = 5 + 4 * 3 / 2;",
            SourceFormatter.format(src, """{"mandatory-space-surrounding-operations": true}"""),
        )
    }

    @Test
    fun `single space separation normalizes gaps and paren spacing`() {
        val src = "let something:      string=\"a\";\nprintln(something);"
        assertEquals(
            "let something : string = \"a\";\nprintln ( something );",
            SourceFormatter.format(src, """{"mandatory-single-space-separation": true}"""),
        )
    }

    @Test
    fun `line break after statement splits preserving inner spacing`() {
        val src = "let a: number = 1;let b :number=2;"
        assertEquals(
            "let a: number = 1;\nlet b :number=2;",
            SourceFormatter.format(src, """{"mandatory-line-break-after-statement": true}"""),
        )
    }

    @Test
    fun `println line breaks zero one two`() {
        val src = "println(\"a\");\nprintln(\"b\");"
        val zero = SourceFormatter.format(src, """{"line-breaks-after-println": 0}""")
        assertEquals("println(\"a\");\nprintln(\"b\");", zero)
        val one = SourceFormatter.format(src, """{"line-breaks-after-println": 1}""")
        assertEquals("println(\"a\");\n\nprintln(\"b\");", one)
        val two = SourceFormatter.format(src, """{"line-breaks-after-println": 2}""")
        assertEquals("println(\"a\");\n\n\nprintln(\"b\");", two)
    }

    @Test
    fun `if brace below and same line`() {
        val below = "let s: boolean = true;\nif (s) {\n  println(\"x\");\n}"
        assertEquals(
            "let s: boolean = true;\nif (s)\n{\n  println(\"x\");\n}",
            SourceFormatter.format(below, """{"if-brace-below-line": true}"""),
        )

        val same = "let s: boolean = true;\nif (s)\n{\n  println(\"x\");\n}"
        assertEquals(
            "let s: boolean = true;\nif (s) {\n  println(\"x\");\n}",
            SourceFormatter.format(same, """{"if-brace-same-line": true}"""),
        )
    }

    @Test
    fun `indent inside if recomputes indentation by brace depth`() {
        val src = "let s: boolean = true;\nif (s) {\n  if (s) {\n    println(\"x\");\n  }\n}"
        assertEquals(
            "let s: boolean = true;\nif (s) {\n    if (s) {\n        println(\"x\");\n    }\n}",
            SourceFormatter.format(src, """{"indent-inside-if": 4}"""),
        )
    }

    @Test
    fun `composable spacing rules apply together`() {
        val src = "let x:number=1;"
        val json = """{"space-before-colon": true, "space-after-colon": true, "space-around-equals": true}"""
        assertEquals("let x : number = 1;", SourceFormatter.format(src, json))
    }
}
