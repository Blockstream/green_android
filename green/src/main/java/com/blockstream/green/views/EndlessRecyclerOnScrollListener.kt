package com.blockstream.green.views

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


abstract class EndlessRecyclerOnScrollListener : RecyclerView.OnScrollListener() {
    private var enabled = true

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if (enabled) {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            if(layoutManager.findLastVisibleItemPosition() == (layoutManager.itemCount - 1)){
                onLoadMore()
            }
        }
    }

    fun enable(): EndlessRecyclerOnScrollListener {
        enabled = true
        return this
    }

    fun disable(): EndlessRecyclerOnScrollListener {
        enabled = false
        return this
    }

    abstract fun onLoadMore()
}