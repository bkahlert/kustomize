package com.imgcstmzr.patch

import com.imgcstmzr.cli.KeyValueDocument
import com.imgcstmzr.patch.Requirements.requireContainingContent
import com.imgcstmzr.patch.Requirements.requireContainingKeyValue
import com.imgcstmzr.patch.ini.IniDocument
import koodies.collections.Dictionary
import koodies.collections.dictOf

// TOOD vcgencmd get_config <config>: this displays a specific config value, e.g. vcgencmd get_config arm_freq
class UsbOnTheGoPatch(
    module: String,
    dict: Dictionary<String, String> = dictOf(
        "g_serial" to "Serial",
        "g_ether" to "Ethernet",
        "g_mass_storage" to "Mass storage",
        "g_midi" to "MIDI",
        "g_audio" to "Audio",
        "g_hid" to "Keyboard/Mouse",
        "g_acm_ms" to "Mass storage and Serial",
        "g_cdc" to "Ethernet and Serial",
        "g_multi" to "Multi",
        "g_webcam" to "Webcam",
        "g_printer" to "Printer",
        "g_zero" to "Gadget tester",
    ) { profile -> "Unknown ($profile)" },
) : Patch by buildPatch("Activate USB On-The-Go for Profile ${dict[module]}", {
    
    files {
        //_prompt "Activate USB--on-the-go?" "Y n" "dwc2"
        edit("/boot/config.txt", requireContainingContent("dtoverlay=dwc2")) { path ->
            val iniDocument = IniDocument(path)
            iniDocument.createKeyIfMissing("dtoverlay", "dwc2", sectionName = "all")
            iniDocument.save(path)
        }

        //_prompt "Activate USB--on-the-go?" "Y n" "dwc2"
        edit("/boot/cmdline.txt", requireContainingKeyValue("modules-load", "dwc2")) { path ->
            with(KeyValueDocument(path)) {
                addValue("modules-load", "dwc2")
                save(path)
            }
        }

        //_prompt "Activate ...?" "Y n" "..."
        edit("/boot/cmdline.txt", requireContainingKeyValue("modules-load", "dwc2", module)) { path ->
            with(KeyValueDocument(path)) {
                addValue("modules-load", module)
                save(path)
            }
        }
    }
})
