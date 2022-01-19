package com.blockstream.green.ui.items

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.blockstream.green.R
import com.blockstream.green.gdk.getNetworkIcon
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.expandable.items.AbstractExpandableItem


class NetworkSmallListItem constructor(val network: String, val networkName: String) : AbstractExpandableItem<NetworkSmallListItem.ViewHolder>(){
    override val type: Int
        get() = R.id.fastadapter_network_small_item_id

    /** defines the layout which will be used for this item in the list */
    override val layoutRes: Int
        get() = R.layout.list_item_network_small

    init {
        identifier = network.hashCode().toLong()
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(val view: View) : FastAdapter.ViewHolder<NetworkSmallListItem>(view) {

        var title: TextView = view.findViewById(R.id.title)
        var icon: ImageView = view.findViewById(R.id.icon)
        var networkIcon: ImageView = view.findViewById(R.id.network)

        override fun bindView(item: NetworkSmallListItem, payloads: List<Any>) {
            title.text = item.networkName
            icon.setImageResource(R.drawable.ic_multisig)
            networkIcon.setImageResource(item.network.getNetworkIcon())
        }

        override fun unbindView(item: NetworkSmallListItem) {
            
        }
    }
}
