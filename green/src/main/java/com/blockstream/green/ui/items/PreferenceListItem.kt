package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemPreferenceBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.ui.utils.StringHolder

data class PreferenceListItem(
    var title: StringHolder = StringHolder(null),
    var subtitle: StringHolder = StringHolder(null),
    val withSwitch : Boolean = false,
    var withButton: Boolean = false,
    val withRadio : Boolean = false,
) : AbstractBindingItem<ListItemPreferenceBinding>() {
    var switchChecked: Boolean = false
    var radioChecked: Boolean = false
    var buttonText : String? = null

    override val type: Int
        get() = R.id.fastadapter_preference_item_id

    init {
        identifier = hashCode().toLong()
    }

    override fun bindView(binding: ListItemPreferenceBinding, payloads: List<Any>) {
        title.applyToOrHide(binding.title)
        subtitle.applyToOrHide(binding.subtitle)

        binding.switchMaterial.isVisible = withSwitch
        binding.switchMaterial.isChecked = switchChecked
        binding.radionMaterial.isVisible = withRadio
        binding.radionMaterial.isChecked = radioChecked
        binding.button.isVisible = withButton
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemPreferenceBinding {
        return ListItemPreferenceBinding.inflate(inflater, parent, false)
    }
}