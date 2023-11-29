package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.BundleCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockstream.common.extensions.logException
import com.blockstream.common.gdk.data.Account
import com.blockstream.green.databinding.SelectUtxosBottomSheetBinding
import com.blockstream.green.ui.items.UtxoListItem
import com.blockstream.green.ui.send.SendViewModel
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.itemanimators.SlideDownAlphaAnimator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KLogging

// WIP
class SelectUtxosBottomSheetDialogFragment : WalletBottomSheetDialogFragment<SelectUtxosBottomSheetBinding, SendViewModel>() {
    override val screenName = "SelectUTXO"

    override fun inflate(layoutInflater: LayoutInflater) = SelectUtxosBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val account: Account? = BundleCompat.getParcelable(requireArguments(), ACCOUNT, Account::class.java)

        val utxoAdapter = ItemAdapter<UtxoListItem>()

        lifecycleScope.launch(context = Dispatchers.IO + logException(countly)){

            val accounts = if(account != null){
                listOf(account)
            }else{
                session.accounts.value
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
        const val ACCOUNT = "ACCOUNT"

        fun show(account: Account? = null, fragmentManager: FragmentManager){
            show(SelectUtxosBottomSheetDialogFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putParcelable(ACCOUNT, account)
                }
            }, fragmentManager)
        }
    }
}