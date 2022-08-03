package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.gdk.data.Account
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAccountBinding
import com.blockstream.green.gdk.GdkSession
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.gdk.policyAsset
import com.blockstream.green.utils.toAmountLook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KLogging


data class AccountListItem constructor(
    val account: Account,
    val session: GdkSession,
) : AbstractBindingItem<ListItemAccountBinding>() {
    override val type: Int
        get() = R.id.fastadapter_account_item_id

    init {
        identifier = "AccountListItem".hashCode() + account.id.hashCode().toLong()
    }

    override fun createScope(): CoroutineScope = session.createScope(Dispatchers.Main)

    override fun bindView(binding: ListItemAccountBinding, payloads: List<Any>) {
        binding.account = account
        binding.showBalance = true

        val policyAsset = session.accountAssets(account).policyAsset()

        binding.primaryValue = ""
        binding.secondaryValue = ""
        scope.launch {
            binding.primaryValue = withContext(context = Dispatchers.IO) { policyAsset.toAmountLook(session, withUnit = true, withGrouping = true, withMinimumDigits = false) }
            binding.secondaryValue = withContext(context = Dispatchers.IO) { policyAsset.toAmountLook(session, withUnit = true, isFiat = true, withGrouping = true, withMinimumDigits = false) }
        }

        binding.icon.setImageResource(account.network.getNetworkIcon())
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemAccountBinding {
        return ListItemAccountBinding.inflate(inflater, parent, false)
    }

    companion object: KLogging()
}