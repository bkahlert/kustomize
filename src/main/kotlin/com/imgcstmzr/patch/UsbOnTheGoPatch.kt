package com.imgcstmzr.patch

import com.imgcstmzr.cli.KeyValueDocument
import com.imgcstmzr.patch.ini.IniDocument
import java.nio.file.Path

class UsbOnTheGoPatch : Patch { // TOOD vcgencmd get_config <config>: this displays a specific config value, e.g. vcgencmd get_config arm_freq
    override val name = "USB On-The-Go (provides ethernet, webcam, etc. via single USB port)"
    override val actions: List<PathAction>
        get() = listOf(
            //_prompt "Activate USB--on-the-go?" "Y n" "dwc2"
            PathAction(Path.of("/boot/config.txt"), containsContent("dtoverlay=dwc2")) { path ->
                val iniDocument = IniDocument(path)
                iniDocument.createKeyIfMissing("dtoverlay", "dwc2", sectionName = "all")
                iniDocument.save(path)
            },//_prompt "Activate USB--on-the-go?" "Y n" "dwc2"
            PathAction(Path.of("/boot/cmdline.txt"), containsValue("modules-load", "dwc2")) { path ->
                with(KeyValueDocument(path)) {
                    addValue("modules-load", "dwc2")
                    save(path)
                }
            },//_prompt "Activate virtual ethernet?" "Y n" "g_ether"
            PathAction(Path.of("/boot/cmdline.txt"), containsValue("modules-load", "dwc2", "g_ether")) { path ->
                with(KeyValueDocument(path)) {
                    addValue("modules-load", "dwc2")
                    addValue("modules-load", "g_ether")
                    save(path)
                }
            },//_prompt "Activate virtual webcam?" "y N" "g_webcam"
            PathAction(Path.of("/boot/cmdline.txt"), containsValue("modules-load", "dwc2", "g_webcam")) { path ->
                with(KeyValueDocument(path)) {
                    addValue("modules-load", "dwc2")
                    addValue("modules-load", "g_webcam")
                    save(path)
                }
            },
        )

    private fun containsContent(string: String): (Path) -> Unit {
        return {
            require(it.toFile().canRead()) { "$it can't be read" }
            require(it.toFile().readLines().any { line -> line.contains(string) }) { "$it does not contain $string " }
        }
    }

    private fun containsValue(key: String, vararg values: String): (Path) -> Unit {
        return { it: Path ->
            require(it.toFile().canRead()) { "$it can't be read" }
            require(it.toFile().canRead() && values.all { value ->
                KeyValueDocument(it).containsValue(key,
                    value)
            }) { "$it does not contain ${values.toList()} " }
        }
    }
}
