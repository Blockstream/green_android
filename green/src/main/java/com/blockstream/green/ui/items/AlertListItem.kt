package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockstream.gdk.data.TwoFactorReset
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAlertBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

class AlertListItem constructor(private val alertType: AlertType, val action : (isClose: Boolean) -> Unit) : AbstractBindingItem<ListItemAlertBinding>() {
    override val type: Int
        get() = R.id.fastadapter_alert_item_id

    init {
        identifier = javaClass.hashCode().toLong() + alertType.hashCode().toLong()
    }

    override fun bindView(binding: ListItemAlertBinding, payloads: List<Any>) {
        val res = binding.root.resources

        when(alertType){
            is AlertType.SystemMessage -> {
                binding.alertView.title = res.getString(R.string.id_system_message)
                binding.alertView.message = alertType.message
                binding.alertView.setMaxLines(3)
                binding.alertView.closeButton {
                    action.invoke(true)
                }
            }
            is AlertType.Dispute2FA -> {
                binding.alertView.title = res.getString(R.string.id_2fa_dispute_in_progress)
                binding.alertView.message = res.getString(R.string.id_warning_wallet_locked_by)
                binding.alertView.setMaxLines(0)
                binding.alertView.closeButton(null)
            }
            is AlertType.Reset2FA -> {
                binding.alertView.title = res.getString(R.string.id_2fa_reset_in_progress)
                binding.alertView.message = res.getString(R.string.id_your_wallet_is_locked_for_a, alertType.twoFactorReset.daysRemaining)
                binding.alertView.setMaxLines(0)
                binding.alertView.closeButton(null)
            }
        }
        binding.alertView.primaryButton(res.getString(R.string.id_learn_more)){
            action.invoke(false)
        }
    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemAlertBinding {
        return ListItemAlertBinding.inflate(inflater, parent, false)
    }
}

sealed class AlertType{
    class SystemMessage(val message: String) : AlertType()
    abstract class Abstract2FA(val twoFactorReset: TwoFactorReset) : AlertType()
    class Dispute2FA(twoFactorReset: TwoFactorReset) : Abstract2FA(twoFactorReset)
    class Reset2FA(twoFactorReset: TwoFactorReset): Abstract2FA(twoFactorReset)
}