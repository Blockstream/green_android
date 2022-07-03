package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAppReviewBinding
import com.mikepenz.fastadapter.binding.AbstractBindingItem


data class AppReviewListItem constructor(val action: (rate: Int) -> Unit) :
    AbstractBindingItem<ListItemAppReviewBinding>() {
    override val type: Int
        get() = R.id.fastadapter_app_review_item_id

    init {
        identifier = "AppReviewListItem".hashCode().toLong()
    }

    override fun bindView(binding: ListItemAppReviewBinding, payloads: List<Any>) {
        binding.closeButton.setOnClickListener {
            action.invoke(0)
        }

        listOf(
            binding.star1,
            binding.star2,
            binding.star3,
            binding.star4,
            binding.star5
        ).forEach {
            it.setOnClickListener {
                binding.rate = when (it.id) {
                    R.id.star_1 -> 1
                    R.id.star_2 -> 2
                    R.id.star_3 -> 3
                    R.id.star_4 -> 4
                    else -> 5
                }

                val rate = binding.rate ?: 0

                action.invoke(rate)
            }
        }

    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemAppReviewBinding {
        return ListItemAppReviewBinding.inflate(inflater, parent, false)
    }
}
