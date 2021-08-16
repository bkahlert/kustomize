package com.bkahlert.kustomize.os

import com.bkahlert.kommons.unit.Size

class OperatingSystemMock(
    override val name: String,
    override val fullName: String = OperatingSystems.RiscTestOS.fullName,
    override val downloadUrl: String = OperatingSystems.RiscTestOS.downloadUrl,
    override val approximateImageSize: Size = OperatingSystems.RiscTestOS.approximateImageSize,
    override val defaultCredentials: OperatingSystem.Credentials = OperatingSystems.RiscTestOS.defaultCredentials,
) : OperatingSystem {
    override fun toString(): String = fullName
}
