package com.blockstream.green.ui.items

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import app.rive.runtime.kotlin.core.PlayableInstance
import com.blockstream.green.R
import com.blockstream.green.databinding.ListItemAppReviewBinding
import com.blockstream.green.utils.RiveListener
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

        var lastStateName = ""
        var handled = false

        binding.rive.registerListener(object : RiveListener() {
            override fun notifyPause(animation: PlayableInstance) {
                val rate = when(lastStateName){
                    "1_star" -> 1
                    "2_stars" -> 2
                    "3_stars" -> 3
                    "4_stars" -> 4
                    "5_stars" -> 5
                    else -> 0
                }

                if(rate > 0 && !handled){
                    handled = true
                    ContextCompat.getMainExecutor(binding.root.context).execute {
                        action.invoke(rate)
                    }
                }

            }
            override fun notifyStateChanged(stateMachineName: String, stateName: String) {
                lastStateName = stateName
            }
        })
    }

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): ListItemAppReviewBinding {
        return ListItemAppReviewBinding.inflate(inflater, parent, false)
    }
}
