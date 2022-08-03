package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.blockstream.gdk.data.AccountAsset
import com.blockstream.green.R
import com.blockstream.green.data.EnrichedAsset
import com.blockstream.green.databinding.ListItemAssetAccountsBinding
import com.blockstream.green.databinding.SelectAccountLayoutBinding
import com.blockstream.green.extensions.bind
import com.blockstream.green.extensions.context
import com.blockstream.green.gdk.AssetPair
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.getAssetNameOrNull
import com.blockstream.green.gdk.isPolicyAsset
import com.blockstream.green.ui.bottomsheets.ChooseAssetAccountListener
import com.blockstream.green.utils.toPixels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import mu.KLogging


data class AssetAccountsListItem constructor(
    val session: GdkSession,
    val assetPair: AssetPair,
    val enrichedAsset: EnrichedAsset,
    val listener: ChooseAssetAccountListener,
) : AbstractExpandableBindingItem<ListItemAssetAccountsBinding>() {
    override val type: Int
        get() = R.id.fastadapter_asset_accounts_item_id

    init {
        identifier = (assetPair.first.ifBlank { "AssetAccountsListItem" }).hashCode().toLong() + enrichedAsset.hashCode().toLong()
    }

    override fun createScope(): CoroutineScope = session.createScope(dispatcher = Dispatchers.Main)

    override fun bindView(binding: ListItemAssetAccountsBinding, payloads: List<Any>) {
        binding.card.strokeColor = ContextCompat.getColor(
            binding.context(),
            if (isSelected) R.color.brand_green else android.R.color.transparent
        )
        binding.card.strokeWidth = binding.context().toPixels(2)

        binding.asset.bind(
            scope = scope,
            assetId = assetPair.first,
            session = session,
        )

        if(enrichedAsset.isAnyLiquidAsset) {
            // Change asset name
            binding.asset.name = context(binding).getString(R.string.id_receive_any_liquid_asset)
            binding.asset.icon.setImageDrawable(ContextCompat.getDrawable(context(binding), R.drawable.ic_unknown))
        }

        val accountExists =
            isSelected && session.accounts.find { it.isLiquid && it.isAmp == enrichedAsset.isAmp } != null

        binding.messageTextView.text = context(binding).getString(
            when {
                enrichedAsset.isAnyLiquidAsset -> {
                    R.string.id_you_need_a_liquid_account_in
                }
                accountExists && enrichedAsset.isAmp -> {
                    R.string.id_s_is_an_amp_asset_you_can
                }
                accountExists && !enrichedAsset.isAmp -> {
                    R.string.id_s_is_a_liquid_asset_you_can
                }
                !accountExists && enrichedAsset.isAmp -> {
                    R.string.id_s_is_an_amp_asset_you_need_an
                }
                else -> {
                    R.string.id_s_is_a_liquid_asset_you_need_a
                }
            },
            enrichedAsset.assetId.getAssetNameOrNull(session) ?: enrichedAsset.assetId.slice(
                0 until enrichedAsset.assetId.length.coerceAtMost(5)
            )
        )

        if (isSelected) {
            val layoutInflater = LayoutInflater.from(context(binding))

            val entries = (if (enrichedAsset.assetId.isPolicyAsset(session)) {
                session.accounts.filter { it.network.policyAsset == enrichedAsset.assetId }
            } else {
                session.accounts.filter { it.isLiquid && it.isAmp == enrichedAsset.isAmp }
            })

            // Cache views
            val tag = entries.joinToString { it.id }

            if (binding.accounts.tag != tag) {
                binding.accounts.removeAllViews()
                entries.forEach { account ->
                    SelectAccountLayoutBinding.inflate(layoutInflater)
                        .also { accountBinding ->
                            accountBinding.account = account

                            accountBinding.root.setOnClickListener { _ ->
                                listener.accountAssetClicked(AccountAsset(account = account, assetId = assetPair.first))
                            }

                            binding.accounts.addView(accountBinding.root)
                        }
                }

                binding.accounts.tag = tag
            }
        }

        binding.createNewAccount.root.setOnClickListener {
            listener.createNewAccountClicked(assetId = assetPair.first)
        }

        // Set last to avoid ui glitches
        binding.isSelected = isSelected
        binding.isPolicy = assetPair.first.isPolicyAsset(session) && !enrichedAsset.isAnyLiquidAsset
        binding.isWatchOnly = session.isWatchOnly

        // Disable animation
        binding.root.isClickable = false
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemAssetAccountsBinding {
        return ListItemAssetAccountsBinding.inflate(inflater, parent, false).also {
            it.asset
            it.createNewAccount.root.isVisible = false
        }
    }

    companion object : KLogging()
}
