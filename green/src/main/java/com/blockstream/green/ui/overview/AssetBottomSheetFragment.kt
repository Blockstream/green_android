package com.blockstream.green.ui.overview

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.blockstream.green.R
import com.blockstream.green.databinding.AssetDetailsBottomSheetBinding
import com.blockstream.green.gdk.SessionManager
import com.blockstream.gdk.params.Convert
import com.blockstream.green.ui.WalletBottomSheetDialogFragment
import com.blockstream.green.ui.items.OverlineTextListItem
import com.blockstream.green.utils.asset
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.ui.utils.StringHolder
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.kotlin.subscribeBy
import javax.inject.Inject


@AndroidEntryPoint
class AssetBottomSheetFragment : WalletBottomSheetDialogFragment<AssetDetailsBottomSheetBinding>(
    layout = R.layout.asset_details_bottom_sheet
) {

    @Inject
    lateinit var sessionManager : SessionManager

    private val args: AssetBottomSheetFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        val session = sessionManager.getWalletSession(args.wallet)
        val assetId = args.assetId
        val asset = session.getAsset(assetId)

        val balanceListItem = OverlineTextListItem(StringHolder(R.string.id_total_balance), StringHolder("-"))

        val list = mutableListOf<GenericItem>()

        list += OverlineTextListItem(StringHolder(R.string.id_name), StringHolder(asset?.name ?: getString(R.string.id_no_registered_name_for_this)))
        list += OverlineTextListItem(StringHolder(R.string.id_asset_id), StringHolder(assetId))
        list += balanceListItem
        list += OverlineTextListItem(StringHolder(R.string.id_precision), StringHolder((asset?.precision ?: 0).toString()))
        list += OverlineTextListItem(StringHolder(R.string.id_ticker), StringHolder(asset?.ticker ?: getString(R.string.id_no_registered_ticker_for_this)))
        list += OverlineTextListItem(StringHolder(R.string.id_issuer), StringHolder(asset?.entity?.domain ?: getString(R.string.id_unknown)))

        val itemAdapter = FastItemAdapter<GenericItem>()
        itemAdapter.add(list)

        binding.recycler.apply {
            adapter = FastAdapter.with(itemAdapter)
        }

        session.getBalancesObservable()
            .subscribeBy(
                onNext = {
                    it[assetId]?.let {
                        balanceListItem.text = StringHolder(session.convertAmount(Convert(it, asset)).asset(false))
                    }
                }
            )

//        session?.getBalancesObservable()?.subscribe {
//            balanceListItem.text = StringHolder(it.find { it.first == assetId }?.second?.format() ?: "-")
//            binding.recycler.adapter?.notifyDataSetChanged()
//        }?.addTo(disposables)

    }
}