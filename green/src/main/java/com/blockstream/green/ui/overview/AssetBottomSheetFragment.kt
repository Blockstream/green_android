package com.blockstream.green.ui.overview

import android.os.Bundle
import android.view.View
import com.blockstream.green.R
import com.blockstream.green.databinding.AssetDetailsBottomSheetBinding
import com.blockstream.green.ui.WalletBottomSheetDialogFragment
import com.blockstream.green.ui.items.OverlineTextListItem
import com.blockstream.green.ui.looks.AssetLook
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.ui.utils.StringHolder
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy


@AndroidEntryPoint
class AssetBottomSheetFragment : WalletBottomSheetDialogFragment<AssetDetailsBottomSheetBinding, AbstractWalletViewModel>(
    layout = R.layout.asset_details_bottom_sheet
) {

    companion object {
        private const val ASSET_ID = "ASSET_ID"

        fun newInstance(message: String): AssetBottomSheetFragment =
            AssetBottomSheetFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putString(ASSET_ID, message)
                }
            }
    }

    private val disposables = CompositeDisposable()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = viewModel.session
        val assetId = arguments?.getString(ASSET_ID) ?: session.policyAsset
        val asset = session.getAsset(assetId)

        val look = AssetLook(assetId, 0, session)

        val isPolicyAsset = session.policyAsset == assetId

        val list = mutableListOf<GenericItem>()

        list += OverlineTextListItem(StringHolder(R.string.id_name), StringHolder(if(isPolicyAsset) look.name else asset?.name ?: getString(R.string.id_no_registered_name_for_this)))

        val balanceListItem = OverlineTextListItem(StringHolder(R.string.id_total_balance), StringHolder("-"))
        val blockHeightListItem = OverlineTextListItem(StringHolder(R.string.id_block_height), StringHolder("-"))

        if(isPolicyAsset){
            list += blockHeightListItem
            list += balanceListItem
        }else{
            list += OverlineTextListItem(StringHolder(R.string.id_asset_id), StringHolder(assetId))
            list += balanceListItem
            list += OverlineTextListItem(StringHolder(R.string.id_precision), StringHolder((asset?.precision ?: 0).toString()))
            list += OverlineTextListItem(StringHolder(R.string.id_ticker), StringHolder(asset?.ticker ?: getString(R.string.id_no_registered_ticker_for_this)))
            list += OverlineTextListItem(StringHolder(R.string.id_issuer), StringHolder(asset?.entity?.domain ?: getString(R.string.id_unknown)))
        }

        val itemAdapter = FastItemAdapter<GenericItem>()
        itemAdapter.add(list)

        binding.recycler.apply {
            adapter = FastAdapter.with(itemAdapter)
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        session.getBalancesObservable()
            .subscribeBy(
                onNext = {
                    look.amount = it[assetId] ?: 0
                    balanceListItem.text = StringHolder(look.balance(withUnit = false))
                    binding.recycler.adapter?.notifyItemChanged(list.indexOf(balanceListItem))
                },
                onError = { }
            ).addTo(disposables)

        session.getBlockObservable().subscribeBy(
            onNext = {
                blockHeightListItem.text = StringHolder(it.height.toString())
            },
            onError = { }
        ).addTo(disposables)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}