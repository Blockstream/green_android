package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.blockstream.gdk.data.Network
import com.blockstream.gdk.data.TwoFactorReset
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAlertBinding
import com.blockstream.green.extensions.context
import com.blockstream.green.extensions.setDrawable
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class AlertListItem constructor(val alertType: AlertType) : AbstractBindingItem<ListItemAlertBinding>() {
    override val type: Int
        get() = R.id.fastadapter_alert_item_id

    init {
        identifier = "AlertListItem".hashCode() + alertType.hashCode().toLong()
    }

    var action: ((isClose: Boolean) -> Unit)? = null

    override fun bindView(binding: ListItemAlertBinding, payloads: List<Any>) {
        val res = binding.root.resources

        when(alertType){
            is AlertType.SystemMessage -> {
                binding.alertView.title = res.getString(R.string.id_system_message)
                binding.alertView.message = alertType.message
                binding.alertView.setMaxLines(3)
                binding.alertView.closeButton {
                    action?.invoke(true)
                }
                binding.alertView.primaryButton(res.getString(R.string.id_learn_more)){
                    action?.invoke(false)
                }
            }
            is AlertType.Dispute2FA -> {
                binding.alertView.title = res.getString(R.string.id_2fa_dispute_in_progress)
                binding.alertView.message = res.getString(R.string.id_warning_wallet_locked_by)
                binding.alertView.setMaxLines(0)
                binding.alertView.closeButton(null)
                binding.alertView.primaryButton(res.getString(R.string.id_learn_more)){
                    action?.invoke(false)
                }
            }
            is AlertType.Reset2FA -> {
                binding.alertView.title = res.getString(R.string.id_2fa_reset_in_progress)
                binding.alertView.message = res.getString(R.string.id_your_wallet_is_locked_for_a, alertType.twoFactorReset.daysRemaining)
                binding.alertView.setMaxLines(0)
                binding.alertView.closeButton(null)
                binding.alertView.primaryButton(res.getString(R.string.id_learn_more)){
                    action?.invoke(false)
                }
            }
            is AlertType.TestnetWarning -> {
                binding.alertView.title = res.getString(R.string.id_warning)
                binding.alertView.message = res.getString(R.string.id_this_wallet_operates_on_a_test)
                binding.alertView.setMaxLines(0)
                binding.alertView.closeButton(null)
                binding.alertView.primaryButton("", null)
                binding.alertView.setIconVisibility(true)
            }
            is AlertType.EphemeralBip39 -> {
                binding.alertView.title = res.getString(R.string.id_passphrase_protected)
                binding.alertView.message = res.getString(R.string.id_this_wallet_is_based_on_your)
                binding.alertView.setMaxLines(0)
                binding.alertView.closeButton(null)
                binding.alertView.primaryButton("", null)

                ContextCompat.getDrawable(
                    binding.root.context,
                    R.drawable.ic_bip39_passphrase_24
                )?.also {
                    it.setTint(
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.white
                        )
                    )
                }?.let {
                    binding.alertView.binding.titleTextView.setDrawable(
                        drawableLeft = it,
                        padding = 6
                    )
                }
            }
            is AlertType.Banner -> {
                binding.banner = alertType.banner

                if(alertType.banner.link.isNullOrBlank()){
                    binding.alertView.primaryButton("", null)
                }else{
                    binding.alertView.primaryButton(binding.context().getString(R.string.id_learn_more), null)
                }

                if(alertType.banner.dismissable == true) {
                    binding.alertView.closeButton {
                        action?.invoke(true)
                    }
                }
            }
            is AlertType.FailedNetworkLogin -> {
                binding.alertView.title = res.getString(R.string.id_warning)
                binding.alertView.message = "Some accounts can not be logged in due to network issues. Please try again later."
                binding.alertView.setMaxLines(0)
                binding.alertView.closeButton(null)
                binding.alertView.primaryButton(res.getString(R.string.id_try_again)){
                    action?.invoke(false)
                }
            }
            is AlertType.AppReview -> {

            }
        }

    }

    override fun createBinding(inflater: LayoutInflater, parent: ViewGroup?): ListItemAlertBinding {
        return ListItemAlertBinding.inflate(inflater, parent, false)
    }
}

sealed class AlertType{
    data class SystemMessage(val network: Network, val message: String) : AlertType()
    data class Dispute2FA(val network: Network, val twoFactorReset: TwoFactorReset) : AlertType()
    data class Reset2FA(val network: Network, val twoFactorReset: TwoFactorReset): AlertType()
    object TestnetWarning : AlertType()
    object EphemeralBip39 : AlertType()
    object AppReview : AlertType()
    data class Banner(val banner: com.blockstream.green.data.Banner) : AlertType()
    object FailedNetworkLogin : AlertType()
}
