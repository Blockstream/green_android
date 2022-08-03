package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.gdk.data.Account
import com.blockstream.gdk.data.NetworkLayer
import com.blockstream.gdk.data.belongsToLayer
import com.blockstream.green.databinding.SelectUtxosBottomSheetBinding
import com.blockstream.green.ui.items.UtxoListItem
import com.blockstream.green.ui.send.SendViewModel
import com.blockstream.green.extensions.logException
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KLogging

// WIP
@AndroidEntryPoint
class SelectUtxosBottomSheetDialogFragment : WalletBottomSheetDialogFragment<SelectUtxosBottomSheetBinding, SendViewModel>() {
    override val screenName = "SelectUTXO"

    override fun inflate(layoutInflater: LayoutInflater) = SelectUtxosBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val networkLayer: NetworkLayer? = arguments?.getParcelable(NETWORK_LAYER)
        val account: Account? = arguments?.getParcelable(ACCOUNT)

        val utxoAdapter = ItemAdapter<UtxoListItem>()


        lifecycleScope.launch(context = Dispatchers.IO + logException(countly)){

            val accounts = if(account != null){
                listOf(account)
            }else if(networkLayer != null){
                session.accounts.filter { it.belongsToLayer(networkLayer) }
            }else{
                session.accounts
            }

            session.getUnspentOutputs(accounts).unspentOutputs.map {

                it.value.map {
                    UtxoListItem(scope = lifecycleScope, utxo = it, session = session)
                }
            }.flatten().also {
                utxoAdapter.set(it)
            }
        }


//
//        viewModel.session.observable {
//            val unspentOutputs = it.getUnspentOutputs(
//                network,
//                BalanceParams(
//                    subaccount = account.subAccounts.first().pointer,
//                    confirmations = 0
//                )
//            )
//            unspentOutputs
//
//        }.subscribeBy(
//            onSuccess = {
//                utxoAdapter.set(
//                    it.unspentOutputs[viewModel.account.network.policyAsset]?.map {
//                        UtxoListItem(it)
//                    } ?: listOf())
//            },
//            onError = {
//                it.printStackTrace()
//
//            }
//        )

        binding.buttonClear.setOnClickListener {
            dismiss()
        }

        binding.buttonOK.setOnClickListener {
            dismiss()
        }

        val fastAdapter = FastAdapter.with(utxoAdapter)

//        fastAdapter.getSelectExtension().also {
//            it.isSelectable = true
//            it.multiSelect = true
//        }

        binding.recycler.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SlideDownAlphaAnimator()
            adapter = fastAdapter
            // addItemDecoration(SpaceItemDecoration(toPixels(24)))
        }
    }

    companion object : KLogging() {
        const val NETWORK_LAYER = "NETWORK_LAYER"
        const val ACCOUNT = "ACCOUNT"

        fun show(networkLayer: NetworkLayer? = null, account: Account? = null, fragmentManager: FragmentManager){
            show(SelectUtxosBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(NETWORK_LAYER, networkLayer)
                    bundle.putParcelable(ACCOUNT, account)
                }
            }, fragmentManager)
        }
    }
}