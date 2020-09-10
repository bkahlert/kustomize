package com.imgcstmzr.patch.ini

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(ExecutionMode.CONCURRENT)
@Suppress("RedundantInnerClassModifier")
internal class RegexElementTest {

    @Test
    internal fun `should get all fields`() {
        val editor = KeyValueEditor(" key='value'")
        expectThat(editor.margin).isEqualTo(" ")
        expectThat(editor.name).isEqualTo("key")
        expectThat(editor.assignmentSymbol).isEqualTo("=")
        expectThat(editor.value).isEqualTo("'value'")
    }


    @Test
    internal fun `should set all fields`() {
        val editor = KeyValueEditor(" key='value'")
        editor.margin = "\t"
        editor.name = "foo"
        editor.assignmentSymbol = ":"
        editor.value = "bar"
        expectThat(editor.toString()).isEqualTo("\tfoo:bar")
    }
}

private class KeyValueEditor(input: String) : RegexElement(input, true) {
    var margin by regex("\\s*")
    var name by regex("[\\w]+")
    var assignmentSymbol by regex("[:=]")
    var value by regex("[\"\']?\\w*[\"\']?")
}
