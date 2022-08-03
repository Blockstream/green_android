package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.gdk.data.AccountAsset
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAccountAssetBinding
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.extensions.bind
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import kotlinx.coroutines.CoroutineScope


data class AccountAssetListItem constructor(
    val scope: CoroutineScope,
    val accountAsset: AccountAsset,
    val session: GdkSession,
    val showBalance: Boolean = true,
) : AbstractBindingItem<ListItemAccountAssetBinding>() {
    override val type: Int
        get() = R.id.fastadapter_account_asset_item_id

    init {
        identifier = accountAsset.hashCode().toLong()
    }

    override fun bindView(binding: ListItemAccountAssetBinding, payloads: List<Any>) {
        binding.accountAsset.bind(scope, accountAsset, session, showBalance)
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemAccountAssetBinding {
        return ListItemAccountAssetBinding.inflate(inflater, parent, false)
    }
}
