package com.blockstream.jade.data

import com.blockstream.jade.api.AuthRequest
import com.blockstream.jade.api.AuthRequestParams
import com.blockstream.jade.api.BooleanResponse
import com.blockstream.jade.api.EntropyRequest
import com.blockstream.jade.api.EntropyRequestParams
import com.blockstream.jade.api.HttpRequestNoDataResponse
import com.blockstream.jade.api.HttpRequestResponse
import com.blockstream.jade.api.JadeSerializer
import com.blockstream.jade.api.PinRequest
import com.blockstream.jade.api.PinRequestParams
import com.blockstream.jade.api.SignAttestationResponse
import com.blockstream.jade.api.SignTransactionRequest
import com.blockstream.jade.api.SignTransactionRequestParams
import com.blockstream.jade.api.StringResponse
import com.blockstream.jade.api.TxInput
import com.blockstream.jade.api.TxInputRequest
import com.blockstream.jade.api.VersionInfoRequest
import com.blockstream.jade.api.VersionInfoResponse
import com.blockstream.jade.api.XpubRequest
import com.blockstream.jade.api.XpubRequestParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.encoding.Base64


@OptIn(ExperimentalStdlibApi::class)
class JadeSerializationTest {

    @Test
    fun test_incremental_cbor() {
        val cbor =
            "A26269646634383739383466726573756C74AC6C4A4144455F56455253494F4E66312E302E3330724A4144455F4F54415F4D41585F4348554E4B1910006B4A4144455F434F4E46494763424C456A424F4152445F54595045644A4144456D4A4144455F46454154555245536253426B4944465F56455253494F4E6676352E312E336D434849505F46454154555245536833323030303030306845465553454D41436C3234364632383846363730346E424154544552595F535441545553046A4A4144455F5354415445664C4F434B45446D4A4144455F4E4554574F524B53644D41494E6C4A4144455F4841535F50494EF5".hexToByteArray()

        (0 .. cbor.size).forEach { i ->
            cbor.copyOfRange(0, i).let { it ->
                JadeSerializer.decodeOrNull<VersionInfoResponse>(it).also {
                    if (i < cbor.size) {
                        assertNull(it)
                    } else {
                        assertNotNull(it)
                        assertEquals("1.0.30", it!!.result.jadeVersion)

                        println(it)
                        println(it.toJson())
                    }
                }
            }
        }
    }

    @Test
    fun test_versionInfo() {
        VersionInfoRequest(
            id = "842801"
        ).toCborHex().also {
            assertEquals(
                "a362696466383432383031666d6574686f64706765745f76657273696f6e5f696e666f66706172616d73a0",
                it
            )
        }

        val cbor =
            "A2626964643130303066726573756C74AC6C4A4144455F56455253494F4E66312E302E3330724A4144455F4F54415F4D41585F4348554E4B1910006B4A4144455F434F4E46494763424C456A424F4152445F54595045644A4144456D4A4144455F46454154555245536253426B4944465F56455253494F4E6676352E312E336D434849505F46454154555245536833323030303030306845465553454D41436C3234364632383846363730346E424154544552595F535441545553036A4A4144455F5354415445664C4F434B45446D4A4144455F4E4554574F524B53644D41494E6C4A4144455F4841535F50494EF5"

        val versionInfo = JadeSerializer.decode<VersionInfoResponse>(cbor.hexToByteArray()).also {
            println(it)
            println(it.toJson())
        }

        assertEquals(
            "1.0.30",
            versionInfo.result.jadeVersion
        )
    }

    @Test
    fun test_xpub() {
        XpubRequest(
            id = "842801",
            params = XpubRequestParams(path = listOf(), network = "mainnet")
        ).also {
            println(it.toJson())
        }.toCborHex().also {
            assertEquals(
                "a362696466383432383031666d6574686f64686765745f7870756266706172616d73a2647061746880676e6574776f726b676d61696e6e6574",
                it
            )
        }

        var json =
            "A26269646638343238303166726573756C74786F787075623636314D794D774171526263476F62633657524863673754413631366746525A4A526A4B46316942555854726D4C4751785A6339555576464141724C517243594438444B78547A5A6A4A5072364E42676B673573546B3553466F71726E4632566B4445364B4C3658617977"

        val xpub = JadeSerializer.decode<StringResponse>(json.hexToByteArray()).also {
            println(it)
            println(it.toJson())
        }

        assertEquals(
            "xpub661MyMwAqRbcGobc6WRHcg7TA616gFRZJRjKF1iBUXTrmLGQxZc9UUvFAArLQrCYD8DKxTzZjJPr6NBgkg5sTk5SFoqrnF2VkDE6KL6Xayw",
            xpub.result
        )
    }

    @Test
    fun test_entroypy() {
        EntropyRequest(
            id = "1000",
            params = EntropyRequestParams(entropy = Base64.decode("9Gpwa/eYOJFzTnbLwVLRs/WpSx4LBEMIQusvdybRnHg="))
        ).also {
            println(it.toJson())
        }.toCborHex().also {
            assertEquals(
                "a36269646431303030666d6574686f646b6164645f656e74726f707966706172616d73a167656e74726f70795820f46a706bf7983891734e76cbc152d1b3f5a94b1e0b04430842eb2f7726d19c78",
                it
            )
        }
    }

    @Test
    fun test_booleanResponse() {
        listOf(
            "A2626964643130303066726573756C74F5" to true,
            "A2626964643130303066726573756C74F4" to false
        ).forEach {
            JadeSerializer.decode<BooleanResponse>(it.first.hexToByteArray()).also { response ->
                println(response)
                println(response.toJson())
                assertEquals(it.second, response.result)
            }
        }
    }

    @Test
    fun test_auth() {
        AuthRequest(
            params = AuthRequestParams(network = "mainet", epoch = 12312312)
        ).also {
            println(it.toJson())
        }.toCborHex().also {
            assertEquals(
                "a36269646431303030666d6574686f6469617574685f7573657266706172616d73a2676e6574776f726b666d61696e65746565706f63681a00bbdef8",
                it
            )
        }
    }

    @Test
    fun test_http_request() {

        val cbor = "A26269646639393633323466726573756C74A16C687474705F72657175657374A266706172616D73A46475726C7382782F68747470733A2F2F6A61646570696E2D73746167696E672E626C6F636B73747265616D2E636F6D2F6765745F70696E60666D6574686F6464504F535466616363657074646A736F6E6464617461A164646174617901084170436C4D6D5856727A76343355784D6833382F6C3773556C487569792B57726B4376597374734470614F364D6741414146666D31643739496B49337A47682B6C6873306E58714532707974424F755A39384D7049632F694D546A2B396336327873714D376F4372685367344C47693259564A576841344C76686A4667697430397747336D434A6C57666973455138355A596C4C67754B384B423767506861615A77526C797A6D78474646685A48654C3436457741313847555645656B5951636F51384C4B743138727661365135387365595A5A726675356B51484D6B4351366A374553346E7A746E446F377A744E44345656544D477736667A444D68756E43617375695079633D686F6E2D7265706C796370696E"
        val response = JadeSerializer.decode<HttpRequestResponse>(cbor.hexToByteArray()).also {
            println(it)
            println(it.toJson())
        }

        assertEquals("https://jadepin-staging.blockstream.com/get_pin", response.result?.httpRequest?.params?.urls?.first())
        assertEquals("ApClMmXVrzv43UxMh38/l7sUlHuiy+WrkCvYstsDpaO6MgAAAFfm1d79IkI3zGh+lhs0nXqE2pytBOuZ98MpIc/iMTj+9c62xsqM7oCrhSg4LGi2YVJWhA4LvhjFgit09wG3mCJlWfisEQ85ZYlLguK8KB7gPhaaZwRlyzmxGFFhZHeL46EwA18GUVEekYQcoQ8LKt18rva6Q58seYZZrfu5kQHMkCQ6j7ES4nztnDo7ztND4VVTMGw6fzDMhunCasuiPyc=", response.result!!.httpRequest.params.data?.get("data"))
    }

    @Test
    fun test_pin() {
        PinRequest(
            id = "1000",
            params = PinRequestParams(data = "69aO8U5hQITCorPsgOrHDjft01V3Ys5rjIRFgBXYbUxReXfwBUw8r2bL1J/mtYRJH68/B+PJxQu9L0Pk2ZFerZgvYPQH8TZ6zKVqY/0MTCNrHKn7dAu7wYeBYx4FCFgE")
        ).also {
            println(it.toJson())
        }.toCborHex().also {
            assertEquals(
                "a36269646431303030666d6574686f646370696e66706172616d73a1646461746178803639614f38553568514954436f725073674f7248446a667430315633597335726a495246674258596255785265586677425577387232624c314a2f6d7459524a4836382f422b504a785175394c30506b325a4665725a67765950514838545a367a4b5671592f304d54434e72484b6e3764417537775965425978344643466745",
                it
            )
        }
    }

    @Test
    fun test_sign_tx() {
        SignTransactionRequest(
            id = "1000",
            method = "sign_tx",
            params = SignTransactionRequestParams(
                network = "mainnet",
                txn = byteArrayOf(0xf),
                numInput = 1,
                useAeSignatures = true,
                change = listOf(null, ChangeOutput())
            )
        ).also {
            println(it.toJson())
        }.toCborHex().also {
            assertEquals(
                "a36269646431303030666d6574686f64677369676e5f747866706172616d73a5676e6574776f726b676d61696e6e65746374786e410f6a6e756d5f696e7075747301717573655f61655f7369676e617475726573f5666368616e676582f6a0",
                it
            )
        }
    }

    @Test
    fun test_tx_input() {
        TxInputRequest(
            id = "1000",
            method = "tx_input",
            params = TxInput(
                isWitness = true,
                inputTx = "input".toByteArray(),
                path = listOf(1,2,3),
            )
        ).also {
            println(it.toJson())
        }.toCborHex().also {
            assertEquals(
                "a36269646431303030666d6574686f646874785f696e70757466706172616d73a36a69735f7769746e657373f568696e7075745f747845696e707574647061746883010203",
                it
            )
        }
    }

    @Test
    fun test_handshake_init() {
        val bytes = "a2626964643130313166726573756c74a16c687474705f72657175657374a266706172616d73a46475726c7382782f68747470733a2f2f6a61646570696e2e626c6f636b73747265616d2e636f6d2f73746172745f68616e647368616b657855687474703a2f2f6d727278747136746a70626e626d377668356a74366d706a63746e3767677966793577656776626566663378376a727a6e7161776c6d69642e6f6e696f6e2f73746172745f68616e647368616b65666d6574686f6464504f535466616363657074646a736f6e646461746160686f6e2d7265706c796e68616e647368616b655f696e6974".hexToByteArray()

        val response: HttpRequestNoDataResponse = JadeSerializer.decode(bytes)

        assertEquals("handshake_init", response.result?.httpRequest?.onReply)
    }

    @Test
    fun test_sign_attestation() {
        val bytes = "a2626964643130323966726573756c74a3697369676e6174757265590200337c04880a0cb6655767d34f60f7120750b0ea7a1cf7c09786f10afc69a29abe157f376359aba17f3fdc14658251f013b500b54a10857b35689e4c4c28749bf2a83e00fb735d6bf04cf3e4b0c01c0fa87ad7bb77d01ac3951b586209c7b510f403adbd4bc59b250282014b54a1a6c57a62d292da2c3f665dc76f78a65005b15d30057a8e82f5eaab7e4ccde5325417d8b4073f0b76ef14d84d28d80472ba103985600121263277ee1e1af532d4593b20751127d07f7e525552c3c33ef7992dffc9ec734b098cfcd8dd8db9abbf89faeff975eacebe5ed3740db0254e4327ccc38e8c8bfb5060d3499ed92bcab8a668d039a1b1ebd2c457d6091ef9069e94cffaa8090ad1ebd6baadf821b3e584379e3dcdd27f6408765610bb5e3ae397543fb49d3373259d0bf32b47e4a7e8b124378cd5e994b1974dfde952887d139dcd98cdce70ada76cdd878ac95f370f42082380f2a3b3d108a88c784f23be03896104ee3738afad09b024fe7b13ec1d68bf5795d5f7401a3452cbc679d0bb65342d1c643ba8972ea979a999f8cb5384009d32d48debaf8618469de1f4081171fd3365bb83ed1eb5571097f1546633c8de041cc49ccabef3078dd18e364099dfa38d8a433faa67d09f8da500254df59edeaf611fa9967230619e6ca526261d1812202c2e72aea25030aa4869d8de110f292536a9b8aee052a30aa8b060f55572505fedc66a7075626b65795f70656d7903202d2d2d2d2d424547494e205055424c4943204b45592d2d2d2d2d0a4d494943496a414e42676b71686b6947397730424151454641414f43416738414d49494343674b434167454176784b5358446e69394f7a4f664b68447434634a0a7865655434416f306975384179773457314153527838593553304c6c766134516a6a684758475355376e6f464839614d7572505243476c6272322f632b467a6e0a71324134314843656a575249354b7a2b6265456e68717248597a4b5371446b693543336555716f49315161454a77674c56304963707741534a756536736a77690a352b303263766b4e4e49364b793837334841793763306b683873546545785a7935787964633134624e594f2f4c32667a4d614f53756a7374475232372f4c30660a785a41663258644b6156394734646b74323438752f644a394451333843344a69614f486e31335078637251684a4b514671665074666e57465a523435514331750a4a304a484179695347676c6643494d6e622f56766373494d4d4d324e366977566f573531337956624375564b78773962386c637a6b46336741634c4b6c75394a0a6f394e4b2b35625530622f4537744e654a6c64476b637730447136634347563830552f453146617051777848316151614c544a76697145304c63312f744f67420a6b5551716855336f56656c4a725a6f4a715143766f6f76703965536a6652447141487865586b3047366e41794c662f6c316956515650595878522b79356463650a4c75706b57354156785556394a694f7a754c5134307741487433375877355a475967686a30704b2b613451696641676b44552f4478507a373369503063596d690a3376323941636d7a58517544527436596c41734c684471746967594e6c614c45426f424469453231626f486e53526f367261304d303172713531562b44726f580a734a5833544f6d684774424634556164726948635279736a37504c4d785568542f374342783455376c34346f2f4f54384f71513678715565544b4b79314453680a7655427278417a4c48324238584d553668667144734b63434177454141513d3d0a2d2d2d2d2d454e44205055424c4943204b45592d2d2d2d2d0a6d6578745f7369676e61747572655902000f4505b59339866667142e2a941ca6c57b4a7088b5f3c0477b2cc076ad8cd4e9f68ae1a90b75982d8e5f0e5be7b62de0602b3f62566e685ff170fafa6495b9fac95b496909b8b3b95f5ac08bf2715ab279dcccbdd7d720038255f1243b8a34c6042a5177eb3aa98264fca994abb8f4007f741c6c9c10e7945b53f48a3a21f10717d32499d0a4ee12aba38eec37e11ed8852ac348bb38797031fa7af676ae8c3bbf109477e8fbe191b0e080824fedeb77f43d6c454b7ffe5b408fb476aed9fc6b1abd932fefc264db30a5da7c3cb989dd1c42cce2fd66ed9648abe87bd69bf686f3528a5535cf419d3c2ae2e6db7d4bb1b7ac644cd482c4190b3471c2599d041f5550cad85b30e6719ef6ecb1ac083fb2db23d83ed44e9b2f5a1127f67401937cead70868c06f698b4db98934dae9ccf79d1d6251de8e337f5a78bb1a139c7cb1cec624d10cda16bf513ffe5a76ca0e6a17faf8d803f9ce9c02a6eb638fae7a98e199272720f5714673f7ae4ff931fc6e4017a4ea79e55ad4d6e43f30a944d9b3de1a8b038c1e0aa660d836447562087f2d5854b9a7e721c5a5cc8c4ab42fa65230b1e3c00a3732f358b13ee4227584517df43a3615b5836c59c2d5f55ff8e08ad3212cc873cda8382c4c69cc21b4996fe4d6eba9823f1ac0721f57b9ebe258a96bdc70c12c0ad30384c14f4c8dac3181bc5fcc0b81f6f410c9312e9ffa32f8d8".hexToByteArray()

        val response: SignAttestationResponse = JadeSerializer.decode(bytes)

        assertTrue(response.result?.pubkeyPem?.startsWith("-----BEGIN PUBLIC KEY-----") == true)
    }

}
