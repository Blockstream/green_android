package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.data.Country
import com.blockstream.green.databinding.ListItemCountryBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class CountryListItem(val country : Country) : AbstractBindingItem<ListItemCountryBinding>() {
    override val type: Int
        get() = R.id.fastadapter_country_item_id

    init{
        identifier = country.hashCode().toLong()
    }

    override fun bindView(binding: ListItemCountryBinding, payloads: List<Any>) {
        binding.country.text = country.name
        binding.countryCode.text = country.dialCodeString
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemCountryBinding {
        return ListItemCountryBinding.inflate(inflater, parent, false)
    }
}