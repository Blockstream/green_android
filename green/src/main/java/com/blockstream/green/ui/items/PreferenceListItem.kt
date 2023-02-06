package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemPreferenceBinding
import com.blockstream.green.utils.StringHolder
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class PreferenceListItem constructor(
    var title: StringHolder = StringHolder(null),
    var subtitle: StringHolder = StringHolder(null),
    val withSwitch : Boolean = false,
    var withButton: StringHolder = StringHolder(null),
    val withRadio : Boolean = false,
    val titleColor: Int = 0,
    val subtitleColor: Int = 0,
    val isInnerMenu: Boolean = false,
    val iconRes: Int = 0
) : AbstractBindingItem<ListItemPreferenceBinding>() {
    // Decided to add those flags here to avoid animating the whole cell, on the down side,
    // dev should notify the adapter for ui update
    var switchChecked: Boolean = false
    var radioChecked: Boolean = false
    var buttonText : String? = null

    override val type: Int
        get() = R.id.fastadapter_preference_item_id

    init {
        identifier = title.hashCode().toLong()
    }

    override fun bindView(binding: ListItemPreferenceBinding, payloads: List<Any>) {
        title.applyToOrHide(binding.title)
        subtitle.applyToOrHide(binding.subtitle)

        binding.switchMaterial.isVisible = withSwitch
        binding.switchMaterial.isChecked = switchChecked
        binding.radionMaterial.isVisible = withRadio
        binding.radionMaterial.isChecked = radioChecked
        withButton.applyToOrHide(binding.button)

        binding.title.setTextColor(ContextCompat.getColor(binding.root.context, if(titleColor > 0) titleColor else  R.color.color_on_surface_emphasis_high))
        binding.subtitle.setTextColor(ContextCompat.getColor(binding.root.context, if(subtitleColor > 0) subtitleColor else  R.color.color_on_surface_emphasis_medium))

        when {
            isInnerMenu -> {
                binding.icon.isVisible = true
                binding.icon.setImageResource(R.drawable.ic_arrow_right_60)
            }
            iconRes > 0 -> {
                binding.icon.isVisible = true
                binding.icon.setImageResource(iconRes)
            }
            else -> {
                binding.icon.isVisible = false
            }
        }
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemPreferenceBinding {
        return ListItemPreferenceBinding.inflate(inflater, parent, false)
    }
}