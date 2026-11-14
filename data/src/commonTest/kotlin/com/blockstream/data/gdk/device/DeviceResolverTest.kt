// Verifies that DeviceResolver hands the hardware wallet the Liquid asset registry metadata
// for the outputs of a sign_tx request, looked up through the session's AssetsProvider.
package com.blockstream.data.gdk.device

import com.blockstream.data.devices.DeviceModel
import com.blockstream.data.gdk.JsonConverter
import com.blockstream.data.gdk.data.Account
import com.blockstream.data.gdk.data.Asset
import com.blockstream.data.gdk.data.Device
import com.blockstream.data.gdk.data.DeviceRequiredData
import com.blockstream.data.gdk.data.DeviceResolvedData
import com.blockstream.data.gdk.data.DeviceSupportsAntiExfilProtocol
import com.blockstream.data.gdk.data.DeviceSupportsLiquid
import com.blockstream.data.gdk.data.InputOutput
import com.blockstream.data.gdk.data.LiquidAssets
import com.blockstream.data.gdk.data.Network
import com.blockstream.data.gdk.params.AssetsParams
import com.blockstream.data.gdk.params.GetAssetsParams
import com.blockstream.data.managers.AssetsProvider
import com.blockstream.jade.api.AssetInfo
import com.blockstream.jade.api.IssuancePrevout
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceResolverTest {

    private class RecordingHardwareWallet : GdkHardwareWallet() {
        var receivedAssetInfo: List<AssetInfo>? = null

        override val disconnectEvent: StateFlow<Boolean>? = null
        override val firmwareVersion: String? = null
        override val model = DeviceModel.BlockstreamJade
        override val device = Device(
            name = "Jade",
            supportsArbitraryScripts = true,
            supportsLowR = true,
            supportsHostUnblinding = true,
            supportsExternalBlinding = true,
            supportsLiquid = DeviceSupportsLiquid.Lite,
            supportsAntiExfilProtocol = DeviceSupportsAntiExfilProtocol.Optional
        )

        override fun signTransaction(
            network: Network,
            transaction: String,
            inputs: List<InputOutput>,
            outputs: List<InputOutput>,
            transactions: Map<String, String>?,
            useAeProtocol: Boolean,
            hwInteraction: HardwareWalletInteraction?,
            assetInfo: List<AssetInfo>
        ): SignTransactionResult {
            receivedAssetInfo = assetInfo
            return SignTransactionResult(signatures = listOf("00"), signerCommitments = null)
        }

        override fun getXpubs(network: Network, paths: List<List<Int>>, hwInteraction: HardwareWalletInteraction?): List<String> = error("unused")
        override fun signMessage(path: List<Int>, message: String, useAeProtocol: Boolean, aeHostCommitment: String?, aeHostEntropy: String?, hwInteraction: HardwareWalletInteraction?): SignMessageResult = error("unused")
        override fun getBlindingFactors(inputs: List<InputOutput>, outputs: List<InputOutput>, hwInteraction: HardwareWalletInteraction?): BlindingFactorsResult = error("unused")
        override fun getMasterBlindingKey(hwInteraction: HardwareWalletInteraction?): String = error("unused")
        override fun getBlindingNonce(pubKey: String, scriptHex: String, hwInteraction: HardwareWalletInteraction?): String = error("unused")
        override fun getBlindingKey(scriptHex: String, hwInteraction: HardwareWalletInteraction?): String = error("unused")
        override fun getGreenAddress(network: Network, account: Account, path: List<Long>, csvBlocks: Long, hwInteraction: HardwareWalletInteraction?): String = error("unused")
        override fun disconnect() {}
    }

    private class FixedAssetsProvider(private val registry: Map<String, Asset>) : AssetsProvider {
        override suspend fun refreshAssets(params: AssetsParams) {}
        override suspend fun getAssets(params: GetAssetsParams) = LiquidAssets(assets = registry.filterKeys { it in params.assets })
    }

    private val policyAsset = "144c654344aa716d6f3abcc1ca90e5641e4e2a7f633bc09fe3baf64585819a49"
    private val testAssetId = "38fca2d939696061a8f76d4e6b5eecd54e3b4221c846f24a6b279e79952850a5"

    private val network = Network(
        network = Network.ElectrumTestnetLiquid,
        name = "Testnet Liquid",
        isMainnet = false,
        isLiquid = true,
        isDevelopment = false,
        policyAsset = policyAsset
    )

    private val testAsset = Asset(
        name = "Testnet Asset",
        assetId = testAssetId,
        precision = 3,
        ticker = "TEST",
        contract = Json.parseToJsonElement("""{"entity":{"domain":"liquidtestnet.com"},"name":"Testnet Asset","precision":3,"ticker":"TEST","version":0}""").jsonObject,
        issuancePrevout = IssuancePrevout(txid = "0e19e938c74378ae83b549213a12be88ede6e32e1407bfdf50c4ec3f927408ec", vout = 0)
    )

    private fun signTxRequest(wallet: GdkHardwareWallet) = DeviceRequiredData(
        action = "sign_tx",
        device = wallet.device,
        transaction = "0f",
        transactionInputs = listOf(),
        transactionOutputs = listOf(
            InputOutput(assetId = testAssetId, satoshi = 1000),
            InputOutput(assetId = policyAsset, satoshi = 500)
        )
    )

    @Test
    fun passes_registry_asset_info_for_sign_tx_outputs_to_the_wallet() = runTest {
        val wallet = RecordingHardwareWallet()
        val resolver = DeviceResolver(wallet, assetsProvider = FixedAssetsProvider(mapOf(testAssetId to testAsset)))

        val resolved: String = resolver.requestDataFromDevice(network, signTxRequest(wallet))

        assertEquals(listOf(testAssetId), wallet.receivedAssetInfo!!.map { it.assetId })
        assertEquals(testAsset.contract, wallet.receivedAssetInfo!![0].contract)
        assertEquals(listOf("00"), JsonConverter.JsonDeserializer.decodeFromString(DeviceResolvedData.serializer(), resolved).signatures)
    }

    @Test
    fun passes_no_asset_info_without_an_assets_provider() = runTest {
        val wallet = RecordingHardwareWallet()
        val resolver = DeviceResolver(wallet)

        resolver.requestDataFromDevice(network, signTxRequest(wallet))

        assertEquals(listOf(), wallet.receivedAssetInfo)
    }
}
