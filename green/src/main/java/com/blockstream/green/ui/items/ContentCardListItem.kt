package com.blockstream.green.ui.items

import android.view.View
import com.blockstream.green.R
import com.blockstream.green.utils.StringHolder
import com.blockstream.green.views.GreenContentCardView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.expandable.items.AbstractExpandableItem

class ContentCardListItem constructor(
    val key: Any? = null,
    val title: StringHolder,
    val caption: StringHolder? = null,
) : AbstractExpandableItem<ContentCardListItem.ViewHolder>() {
    override val type: Int
        get() = R.id.fastadapter_content_card_item_id

    /** defines the layout which will be used for this item in the list */
    override val layoutRes: Int
        get() = R.layout.list_item_content_card

    init {
        identifier = title.hashCode().toLong()
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : FastAdapter.ViewHolder<ContentCardListItem>(view) {
        var card: GreenContentCardView = view.findViewById(R.id.card)

        override fun bindView(item: ContentCardListItem, payloads: List<Any>) {
            card.setTitle(item.title.getText(itemView.context))
            card.setCaption(item.caption?.getText(itemView.context))
        }

        override fun unbindView(item: ContentCardListItem) {

        }
    }
}