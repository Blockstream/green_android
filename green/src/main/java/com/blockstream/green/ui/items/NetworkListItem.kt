package com.blockstream.green.ui.items

import android.view.View
import com.blockstream.green.R
import com.blockstream.green.gdk.getNetworkIcon
import com.blockstream.green.views.GreenContentCardView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.expandable.items.AbstractExpandableItem


class NetworkListItem constructor(val network: String, val networkName: String, val caption : String) : AbstractExpandableItem<NetworkListItem.ViewHolder>(){
    override val type: Int
        get() = R.id.fastadapter_network_item_id

    /** defines the layout which will be used for this item in the list */
    override val layoutRes: Int
        get() = R.layout.list_item_content_card

    init {
        identifier = network.hashCode().toLong()
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<NetworkListItem>(view) {
        var card: GreenContentCardView = view.findViewById(R.id.card)

        override fun bindView(item: NetworkListItem, payloads: List<Any>) {
            card.setTitle(item.networkName)
            card.setCaption(item.caption)
            card.setIcon(item.network.getNetworkIcon())
        }

        override fun unbindView(item: NetworkListItem) {
            
        }
    }
}
