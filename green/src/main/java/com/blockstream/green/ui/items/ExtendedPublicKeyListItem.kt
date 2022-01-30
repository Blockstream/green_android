package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemExtendedPublicKeyBinding
import com.blockstream.green.utils.StringHolder
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class ExtendedPublicKeyListItem constructor(
    val name: StringHolder = StringHolder(),
    val extendedPublicKey: StringHolder = StringHolder(),
) : AbstractBindingItem<ListItemExtendedPublicKeyBinding>() {
    override val type: Int
        get() = R.id.fastadapter_extended_public_key_item_id

    init {
        identifier = hashCode().toLong()
    }

    override fun bindView(binding: ListItemExtendedPublicKeyBinding, payloads: List<Any>) {
        name.applyTo(binding.name)
        extendedPublicKey.applyTo(binding.extendedPublicKey)
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemExtendedPublicKeyBinding =
        ListItemExtendedPublicKeyBinding.inflate(inflater, parent, false)
}