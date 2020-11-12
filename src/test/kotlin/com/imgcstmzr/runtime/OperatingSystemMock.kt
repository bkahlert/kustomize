package com.imgcstmzr.runtime

import com.bkahlert.koodies.unit.Size

class OperatingSystemMock(
    override val name: String,
    override val fullName: String = OperatingSystems.ImgCstmzrTestOS.fullName,
    override val downloadUrl: String = OperatingSystems.ImgCstmzrTestOS.downloadUrl,
    override val approximateImageSize: Size = OperatingSystems.ImgCstmzrTestOS.approximateImageSize,
    override val defaultCredentials: OperatingSystem.Credentials = OperatingSystems.ImgCstmzrTestOS.defaultCredentials,
) : OperatingSystem {
    override fun toString(): String = fullName
}
