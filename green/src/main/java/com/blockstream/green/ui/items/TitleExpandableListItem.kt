package com.blockstream.green.ui.items

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.blockstream.green.R
import com.mikepenz.fastadapter.ClickListener
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.IClickable
import com.mikepenz.fastadapter.ISubItem
import com.mikepenz.fastadapter.expandable.items.AbstractExpandableItem
import com.mikepenz.fastadapter.ui.utils.StringHolder

open class TitleExpandableListItem(val title: StringHolder) : AbstractExpandableItem<TitleExpandableListItem.ViewHolder>(),
    IClickable<TitleExpandableListItem>, ISubItem<TitleExpandableListItem.ViewHolder> {

    private var mOnClickListener: ClickListener<TitleExpandableListItem>? = null

    //we define a clickListener in here so we can directly animate
    /**
     * we overwrite the item specific click listener so we can automatically animate within the item
     *
     * @return
     */
    @Suppress("SetterBackingFieldAssignment")
    override var onItemClickListener: ClickListener<TitleExpandableListItem>? = { v: View?, adapter: IAdapter<TitleExpandableListItem>, item: TitleExpandableListItem, position: Int ->
        if (item.subItems.isNotEmpty()) {
            v?.findViewById<View>(R.id.arrow)?.let {
                if (!item.isExpanded) {
                    ViewCompat.animate(it).rotation(90f).start()
                } else {
                    ViewCompat.animate(it).rotation(0f).start()
                }
            }
        }
        mOnClickListener?.invoke(v, adapter, item, position) ?: true
    }
        set(onClickListener) {
            this.mOnClickListener = onClickListener // on purpose
        }

    override var onPreItemClickListener: ClickListener<TitleExpandableListItem>?
        get() = null
        set(_) {}

    //this might not be true for your application
    override var isSelectable: Boolean
        get() = subItems.isEmpty()
        set(value) {
            super.isSelectable = value
        }

    override val type: Int
        get() = R.id.fastadapter_title_expandable_item_id

    override val layoutRes: Int
        get() = R.layout.list_item_title_expandable


    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)

        //set the text for the name
        StringHolder.applyTo(title, holder.title)

        if (subItems.isEmpty()) {
            holder.arrow.visibility = View.GONE
        } else {
            holder.arrow.visibility = View.VISIBLE
        }

        if (isExpanded) {
            holder.arrow.rotation = 90f
        } else {
            holder.arrow.rotation = 0f
        }
    }

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        holder.title.text = null
        //make sure all animations are stopped
        holder.arrow.clearAnimation()
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    /**
     * our ViewHolder
     */
    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var title: TextView = view.findViewById(R.id.title)
        var arrow: ImageView = view.findViewById(R.id.arrow)
    }
}