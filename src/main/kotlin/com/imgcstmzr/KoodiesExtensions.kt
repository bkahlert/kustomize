package com.imgcstmzr

import koodies.exec.IO
import koodies.logging.RenderingLogger

fun RenderingLogger?.logMeta(message: String) {
    this?.logLine { IO.Meta typed message }
}

fun RenderingLogger?.logInput(message: String) {
    this?.logLine { IO.Input typed message }
}

fun RenderingLogger?.logOutput(message: String) {
    this?.logLine { IO.Output typed message }
}

fun RenderingLogger?.logError(message: String) {
    this?.logLine { IO.Error typed message }
}
