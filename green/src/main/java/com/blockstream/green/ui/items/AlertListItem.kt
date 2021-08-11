package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAlertBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class AlertListItem(alertType: AlertType, val listener : View.OnClickListener) : AbstractBindingItem<ListItemAlertBinding>() {
    override val type: Int
        get() = R.id.fastadapter_alert_item_id

    init {
        identifier = alertType.ordinal.toLong()
    }

    override fun bindView(binding: ListItemAlertBinding, payloads: List<Any>) {
        val res = binding.root.resources
//        binding.alertView.title = res.getString(R.string.id_important)
//        binding.alertView.message = res.getString(R.string.id_you_ll_need_to_set_up_a_recovery_phrase)
//        binding.alertView.primaryButton(res.getString(R.string.id_set_up_recovery_phrase), listener)
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemAlertBinding {
        return ListItemAlertBinding.inflate(inflater, parent, false)
    }
}

enum class AlertType{
    SETUP_RECOVERY
}