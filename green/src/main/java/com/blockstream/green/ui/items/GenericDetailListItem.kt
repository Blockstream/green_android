package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewTreeLifecycleOwner
import com.blockstream.green.R
import com.blockstream.green.data.Countly
import com.blockstream.green.databinding.ListItemGenericDetailBinding
import com.blockstream.green.gdk.GreenSession
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.utils.copyToClipboard
import com.blockstream.green.utils.snackbar
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import mu.KLogging

class GenericDetailListItem constructor(
    val title: StringHolder = StringHolder(null),
    val content: StringHolder = StringHolder(null),
    val copyOnClick: Boolean = false,
    val liveContent: MutableLiveData<String>? = null,
    val buttonText: StringHolder = StringHolder(null),
    val enableButton: LiveData<Boolean>? = MutableLiveData(true),
    val countly: Countly? = null,
    val session: GreenSession? = null
) : AbstractBindingItem<ListItemGenericDetailBinding>() {
    override val type: Int
        get() = R.id.fastadapter_generic_detail_item_id

    init {
        identifier = title.hashCode().toLong() + content.hashCode().toLong()
    }

    override fun attachToWindow(binding: ListItemGenericDetailBinding) {
        super.attachToWindow(binding)

        if(binding.lifecycleOwner == null) {
            binding.lifecycleOwner = ViewTreeLifecycleOwner.get(binding.root)
        }
    }

    override fun detachFromWindow(binding: ListItemGenericDetailBinding) {
        super.detachFromWindow(binding)
        binding.lifecycleOwner = null
    }

    override fun bindView(binding: ListItemGenericDetailBinding, payloads: List<Any>) {
        val context = binding.root.context

        title.applyToOrHide(binding.title)
        content.applyTo(binding.content)
        buttonText.applyToOrHide(binding.button)

        binding.self = this

        binding.content.requestFocus()
        binding.content.setOnClickListener {
            if (copyOnClick) {
                copyToClipboard(
                    binding.title.text.toString(),
                    content = binding.content.text.toString(),
                    binding.root.context,
                    binding.content
                )
                binding.root.snackbar(R.string.id_copied_to_clipboard)
                session?.let { session -> countly?.shareTransaction(session = session) }
            }
        }

        if (copyOnClick) {
            binding.content.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                null,
                ContextCompat.getDrawable(context, R.drawable.ic_baseline_content_copy_18),
                null
            )
        } else {
            binding.content.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
        }
    }

    override fun unbindView(binding: ListItemGenericDetailBinding) {
        super.unbindView(binding)
        binding.self = null
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemGenericDetailBinding {
        return ListItemGenericDetailBinding.inflate(inflater, parent, false)
    }

    companion object: KLogging()
}