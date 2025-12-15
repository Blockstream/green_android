package com.blockstream.data.gdk.params

import com.blockstream.data.gdk.GreenJson
import kotlinx.serialization.Serializable

@Serializable
data class RsaVerifyParams(
    val pem: String,
    val challenge: String,
    val signature: String
) : GreenJson<RsaVerifyParams>() {
    override fun encodeDefaultsValues(): Boolean = true
    override fun kSerializer() = serializer()

    companion object {
        val VerifyingAuthorityPubKey = """
            -----BEGIN PUBLIC KEY-----
            MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAyBnvF2+06j87PL4GztOf
            6OVPXoHObwU/fV3PJDWAY1kpWO2MRQUaM7xtb+XwEzt+Vw9it378nCVvREJ/4IWQ
            uVO8qQn2V1eASIoRtfM5HjERRtL4JUc7D1U2Vr4ecJEhQ1nSQuhuU9N2noo/tTxX
            nYIMiFOBJNPqzjWr9gTzcLdE23UjpasKMKyWEVPw0AGWl/aOGo8oAaGYjqB870s4
            29FBJeqOpaTHZqI/xp9Ac+R8gCP6H77vnSHGIxyZBIfcoPc9AFL83Ch0ugPLMQDf
            BsUzfi8gANHp6tKAjrH00wgHV1JC1hT7BRHffeqh9Tc7ERUmxg06ajBZf0XdWbIr
            tpNs6/YZJbv4S8+0VP9SRDOYigOuv/2nv16RyMO+TphH6PvwLQoRGixswICT2NBh
            oqTDi2kIwse51EYjLZ5Wi/n5WH+YtKs0O5cVY+0/mUMvknD7fBPv6+rvOr0OZu28
            1Qi+vZuP8it3qIdYybNmyD2FMGsYOb2OkIG2JC5GSn7YGwc+dRa87DGrG7S4rh4I
            qRCB9pudTntGoQNhs0G9aNNa36sUSp+FUAPB8r55chmQPVDv2Uqt/2cpfgy/UIPE
            DvMN0FWJF/3y6x0UOJiNK3VJKjhorYi6dRuJCmk6n+BLXHCaYvfLD7mEp0IEapo7
            VTWr98cwCwEqT+NTHm2FaNMCAwEAAQ==
            -----END PUBLIC KEY-----
        """.trimIndent()
    }
}