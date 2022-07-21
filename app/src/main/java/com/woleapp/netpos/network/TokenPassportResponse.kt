package com.woleapp.netpos.network

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(strict = false, name = "tokenPassportResponse")
data class TokenPassportResponse @JvmOverloads constructor(
    @field:Element(name = "responseMessage", required = false)
    var responseMessage: String = "",

    @field:Element(name = "responseCode", required = false)
    var responseCode: String = "",

    @field:Element(name = "token", required = false)
    var token: String = ""
)