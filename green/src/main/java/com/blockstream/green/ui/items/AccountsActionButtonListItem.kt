package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemButtonActionBinding
import com.blockstream.green.utils.StringHolder
import com.mikepenz.fastadapter.binding.AbstractBindingItem


data class AccountsActionButtonListItem constructor(
    var archivedAccounts: Int,
    val showAddNewAccount : Boolean = true,
    private val extraPadding: Boolean = false
) : AbstractBindingItem<ListItemButtonActionBinding>() {
    override val type: Int
        get() = R.id.fastadapter_accounts_action_button_item_id

    init {
        identifier = hashCode().toLong()
    }

    override fun bindView(binding: ListItemButtonActionBinding, payloads: List<Any>) {

//        val padding =
//            binding.root.resources.getDimension(if (extraPadding) R.dimen.dp32 else R.dimen.dp16)
//                .toInt()
//        binding.root.updatePadding(left = padding, right = padding)

        StringHolder(
            if (archivedAccounts > 0) "${binding.root.resources.getString(R.string.id_view_archived_accounts)} ($archivedAccounts)" else binding.root.resources.getString(
                R.string.id_no_archived_accounts
            )
        ).applyTo(binding.buttonArchivedAccounts)
        binding.buttonArchivedAccounts.setTextColor(
            ContextCompat.getColor(
                binding.root.context,
                if (archivedAccounts > 0) R.color.color_on_surface_emphasis_low else R.color.color_on_surface_emphasis_very_low
            )
        )
        binding.buttonArchivedAccounts.isEnabled = archivedAccounts > 0
        binding.buttonArchivedAccounts.isVisible = archivedAccounts != -1
        binding.buttonAddNewAccount.isVisible = showAddNewAccount
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemButtonActionBinding {
        return ListItemButtonActionBinding.inflate(inflater, parent, false)
    }
}