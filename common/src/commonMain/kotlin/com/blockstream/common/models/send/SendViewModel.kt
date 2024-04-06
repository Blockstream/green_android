package com.blockstream.common.models.send

import com.blockstream.common.data.GreenWallet
import com.blockstream.common.events.Event
import com.blockstream.common.extensions.ifConnected
import com.blockstream.common.extensions.launchIn
import com.blockstream.common.extensions.previewWallet
import com.blockstream.common.gdk.TwoFactorResolver
import com.blockstream.common.gdk.data.Network
import com.blockstream.common.gdk.params.AddressParams
import com.blockstream.common.gdk.params.CreateTransactionParams
import com.blockstream.common.models.GreenViewModel
import com.blockstream.common.sideeffects.SideEffect
import com.blockstream.common.utils.UserInput
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

abstract class SendViewModelAbstract(greenWallet: GreenWallet) :
    GreenViewModel(greenWalletOrNull = greenWallet) {
    override fun screenName(): String = "Send"

    override fun segmentation(): HashMap<String, Any>? {
        return countly.sessionSegmentation(session = session)
    }

    @NativeCoroutinesState
    abstract val note: MutableStateFlow<String>
    @NativeCoroutinesState
    abstract val address: MutableStateFlow<String>
    @NativeCoroutinesState
    abstract val amount: MutableStateFlow<String>
}

class SendViewModel(greenWallet: GreenWallet) : SendViewModelAbstract(greenWallet = greenWallet) {

    override val note: MutableStateFlow<String> = MutableStateFlow(session.pendingTransaction?.second?.memo ?: "")

    override val address: MutableStateFlow<String> = MutableStateFlow("")

    private val network: MutableStateFlow<Network?> = MutableStateFlow(null)
    override val amount: MutableStateFlow<String> = MutableStateFlow("")

    private val checkTransactionMutex = Mutex()

    class LocalEvents {
        data class SetNote(val note: String) : Event
        data class SignTransaction(
            val broadcastTransaction: Boolean = true,
            val twoFactorResolver: TwoFactorResolver
        ) : Event
    }

    class LocalSideEffects {
        object DeviceAddressValidation: SideEffect
    }

    init {

        session.ifConnected {
            address.onEach {
                network.value = session.parseInput(it)?.first
                // check if address creates
            }.launchIn(this)

            network.onEach {
//                if(!account.network.isSameNetwork(checkedInput.first)){
//                    session.allAccounts.value.find { it.network.isSameNetwork(checkedInput.first) }?.also {
//                        account = it
//                    }
//                }
            }.launchIn(this)

        }

        bootstrap()
    }

    private suspend fun createTransactionParams(network: Network): CreateTransactionParams {
        val isGreedy = false

        return if(network.isLightning) {
            val satoshi = UserInput.parseUserInputSafe(
                session = session,
                input = amount.value,
                denomination = denomination.value
            ).getBalance()?.satoshi

            AddressParams(
                address = address.value,
                satoshi = satoshi ?: 0
            ).let { params ->
                CreateTransactionParams(
                    addressees = listOf(params.toJsonElement()),
                    addresseesAsParams = listOf(params)
                )
            }

        } else {

            val satoshi = if(isGreedy) 0 else UserInput.parseUserInputSafe(session = session, input = amount.value, assetId = accountAsset.value?.assetId, denomination = denomination.value)
                .getBalance()?.satoshi

            val unspentOutputs = accountAsset.value?.account?.let { session.getUnspentOutputs(it) }

             AddressParams(
                address = address.value,
                 satoshi = satoshi ?: 0,
                 isGreedy = isGreedy,
                 assetId = accountAsset.value?.assetId?.takeIf { network.isLiquid }
             ).let { params ->
                 CreateTransactionParams(
                     subaccount = accountAsset.value?.account?.pointer,
                     addressees = listOf(params.toJsonElement()),
                     addresseesAsParams = listOf(params),
                     feeRate = getFeeRate(),
                     utxos = unspentOutputs?.unspentOutputsAsJsonElement
                 )
             }
        }
    }

    // TODO FIX
    fun getFeeRate(): Long = network.value?.defaultFee ?: 0

    private suspend fun checkTransaction(){
        checkTransactionMutex.withLock {

            network.value?.let {
                val params = createTransactionParams(network = it)
                val tx = session.createTransaction(it, params)

                // tx.addressees.first().hasLockedAmount



            }
        }
    }
}

class SendViewModelPreview(greenWallet: GreenWallet) :
    SendViewModelAbstract(greenWallet = greenWallet) {

    override val note: MutableStateFlow<String> = MutableStateFlow("")
    override val address: MutableStateFlow<String> = MutableStateFlow("")
    override val amount: MutableStateFlow<String> = MutableStateFlow("")

    companion object {
        fun preview() = SendViewModelPreview(previewWallet())
    }
}