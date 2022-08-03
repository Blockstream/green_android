package com.blockstream.green.ui.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.blockstream.gdk.BTC_POLICY_ASSET
import com.blockstream.gdk.data.Account
import com.blockstream.green.R
import com.blockstream.green.databinding.AssetDetailsBottomSheetBinding
import com.blockstream.green.gdk.isPolicyAsset
import com.blockstream.green.gdk.networkForAsset
import com.blockstream.green.ui.items.OverlineTextListItem
import com.blockstream.green.looks.AssetLook
import com.blockstream.green.ui.wallet.AbstractWalletViewModel
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.GenericItem
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.ui.utils.StringHolder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach


@AndroidEntryPoint
class AssetDetailsBottomSheetFragment: WalletBottomSheetDialogFragment<AssetDetailsBottomSheetBinding, AbstractWalletViewModel>() {
    override val screenName = "AssetDetails"
    override val segmentation: HashMap<String, Any>? = null

    val assetId by lazy { requireArguments().getString(ASSET_ID) ?: BTC_POLICY_ASSET }
    val asset by lazy { session.getAsset(assetId) }

    override val network by lazy { assetId.networkForAsset(session) }
    override val accountOrNull by lazy { requireArguments().getParcelable<Account?>(ACCOUNT) }

    override fun inflate(layoutInflater: LayoutInflater) = AssetDetailsBottomSheetBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val look = AssetLook(assetId, 0, session)

        val isPolicyAsset = assetId.isPolicyAsset(session)

        val list = mutableListOf<GenericItem>()

        list += OverlineTextListItem(StringHolder(R.string.id_name), StringHolder(if(isPolicyAsset) look.name else asset?.name ?: getString(R.string.id_no_registered_name_for_this)))

        val balanceListItem = OverlineTextListItem(StringHolder(if(accountOrNull == null) R.string.id_total_balance else R.string.id_account_balance), StringHolder("-"))
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

        val fastAdapter = FastAdapter.with(itemAdapter)
        binding.recycler.apply {
            adapter = fastAdapter
        }

        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        (if (accountOrNull == null) {
            session.walletAssetsFlow.map {
                it.mapValues { it.value }
            }
        } else {
            session.accountAssetsFlow(account)
        }).onEach {
            look.amount = it[assetId] ?: 0
            balanceListItem.text = StringHolder(session.starsOrNull ?: look.balance(withUnit = true))
            binding.recycler.adapter?.notifyItemChanged(list.indexOf(balanceListItem))
            fastAdapter.notifyAdapterDataSetChanged()
        }.launchIn(lifecycleScope)

        session
            .blockFlow(network)
            .onEach {
                blockHeightListItem.text = StringHolder(it.height.toString())
                fastAdapter.notifyAdapterDataSetChanged()
            }.launchIn(lifecycleScope)
    }

    companion object {
        private const val ASSET_ID = "ASSET_ID"
        private const val ACCOUNT = "ACCOUNT"

        fun show(assetId: String, account: Account? = null, fragmentManager: FragmentManager) {
            show(AssetDetailsBottomSheetFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putString(ASSET_ID, assetId)
                    account?.let { bundle.putParcelable(ACCOUNT, it) }
                }
            }, fragmentManager)
        }
    }
}