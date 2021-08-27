package com.bkahlert.kustomize.patch

import com.bkahlert.kommons.net.div
import com.bkahlert.kommons.net.ip4Of
import com.bkahlert.kommons.net.ipOf

class BluetoothPersonalAreaNetworkPatchTest {

    private val patch = BluetoothPersonalAreaNetworkPatch(
        dhcpRange = ip4Of("10.10.2.1") / 27,
        deviceAddress = ipOf("10.10.2.10"),
    )
}
