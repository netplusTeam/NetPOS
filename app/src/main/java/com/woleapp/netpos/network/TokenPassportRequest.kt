package com.woleapp.netpos.network

import org.simpleframework.xml.Element
import org.simpleframework.xml.Path
import org.simpleframework.xml.Root

@Root(strict = false, name = "tokenPassportRequest")
data class TokenPassportRequest(
    @field:Path("terminalInformation")
    @field:Element(name = "merchantId", required = false)
    var merchantId: String,

    @field:Path("terminalInformation")
    @field:Element(name = "terminalId", required = false)
    var terminalId: String
)
