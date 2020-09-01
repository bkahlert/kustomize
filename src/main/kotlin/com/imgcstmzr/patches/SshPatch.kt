package com.imgcstmzr.patches

import java.io.File
import java.nio.file.Path

interface Patch {
    val name: String
    fun actions(): List<Action>
}

class SshPatch : Patch {
    override val name = "Enabled SSH"
    override fun actions(): List<Action> = listOf(
        Action(Path.of("/boot/ssh"), { it.exists() }) { file -> file.createNewFile() },
    )
}

class UsbOnTheGoPatch : Patch {
    override val name = "USB On-The-Go (provides ethernet, webcam, etc. via single USB port)"
    override fun actions(): List<Action> = listOf(
        Action(Path.of("/boot/config.txt"), fileContains("dtoverlay=dwc2")) { file ->
            file.useLines { // patch frist line
            }
        },
        Action(Path.of("/boot/cmdline.txt"), fileContains("modules-load=dwc2")) { file -> file.createNewFile() },
    )

    private fun fileContains(string: String): (File) -> Boolean {
        return {
            it.canRead() && it.readLines().any { line -> line.contains(string) }
        }
    }
}

class Action(val path: Path, val verifier: (File) -> Boolean, val handler: (File) -> Unit) {
    fun perform(file: File) {
        if (verifier.invoke(file)) {
            handler.invoke(file)
            check(verifier.invoke(file)) { "Verification of $file failed" }
        }
    }
}

val patches = mapOf(
    "ssh.enabled" to SshPatch()
)

//_p
//_prompt "Activate virtual ethernet?" "Y n" "g_ether"
//case $REPLY in
//n)
//_p "Skipping."
//;;
//*)
//_p "Activating virtual ethernet... "
//_hasvalue "g_ether" "modules-load" <./cmdline.txt || echo -n "$(cat ./cmdline.txt)"",g_ether" >./cmdline.txt
//_p "Activated virtual ethernet successfully."
//;;
//esac
//
//_p
//_prompt "Activate virtual webcam?" "y N" "g_webcam"
//case $REPLY in
//y)
//_p "Activating virtual webcam... "
//_hasvalue "g_webcam" "modules-load" <./cmdline.txt || echo -n "$(cat ./cmdline.txt)"",g_webcam" >./cmdline.txt
//_p "Activated virtual ethernet successfully."
//;;
//*)
//_p "Skipping."
//;;
//esac
//;;
//esac
//else
//_warn "Cannot write to cmdline.txt. Skipping OTG (USB on-the-go) setup."
//fi
//else
//_warn "Cannot write to config.txt. Skipping OTG (USB on-the-go) setup."
//fi
