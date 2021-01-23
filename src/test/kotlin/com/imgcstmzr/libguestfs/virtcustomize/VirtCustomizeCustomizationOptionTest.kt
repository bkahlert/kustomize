package com.imgcstmzr.libguestfs.virtcustomize

import koodies.shell.ShellScript
import strikt.api.Assertion
import kotlin.io.path.readText

class VirtCustomizeCustomizationOptionTest

val Assertion.Builder<VirtCustomizeCustomizationOption.FirstBootOption>.file
    get() = get("file %s") { path }

val Assertion.Builder<VirtCustomizeCustomizationOption.FirstBootOption>.script
    get() = get("script %s") { ShellScript(content = path.readText()) }

