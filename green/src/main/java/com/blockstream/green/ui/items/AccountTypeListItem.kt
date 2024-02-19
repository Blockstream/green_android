package com.blockstream.green.ui.items

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isInvisible
import com.blockstream.common.looks.AccountTypeLook
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAccountTypeBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem

data class AccountTypeListItem constructor(
    val accountTypeLook: AccountTypeLook
) :
    AbstractBindingItem<ListItemAccountTypeBinding>() {
    override val type: Int
        get() = R.id.fastadapter_account_type_item_id

    init {
        identifier = accountTypeLook.accountType.ordinal.toLong()
    }

    override fun bindView(binding: ListItemAccountTypeBinding, payloads: List<Any>) {
        binding.accountTypeLook = accountTypeLook
        isEnabled = accountTypeLook.canBeAdded

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (accountTypeLook.canBeAdded) {
                binding.wrap.setRenderEffect(null)
                binding.beta.setRenderEffect(null)
                binding.wrap.alpha = 1f
                binding.beta.alpha = 1f
            } else {
                binding.wrap.alpha = 0.15f
                binding.beta.alpha = 0.15f
                RenderEffect.createBlurEffect(6f, 6f, Shader.TileMode.MIRROR).also {
                    binding.wrap.setRenderEffect(it)
                    binding.beta.setRenderEffect(it)
                }
            }
        } else {
            binding.wrap.isInvisible = !accountTypeLook.canBeAdded
        }
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemAccountTypeBinding {
        return ListItemAccountTypeBinding.inflate(inflater, parent, false)
    }
}