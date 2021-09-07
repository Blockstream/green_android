package com.blockstream.green.views

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


abstract class EndlessRecyclerOnScrollListener constructor(val recyclerView: RecyclerView) : RecyclerView.OnScrollListener() {
    private var enabled = true

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        if (enabled) {
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            if(layoutManager.findLastVisibleItemPosition() == (layoutManager.itemCount - 1)){
                callOnLoadMore()
            }
        }
    }

    private fun checkIfVisible(){
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        if(layoutManager.findLastVisibleItemPosition() == (layoutManager.itemCount - 1)){
            callOnLoadMore()
        }
    }

    // Prevent the following error
    // RecyclerView: Cannot call this method in a scroll callback. Scroll callbacks mightbe run
    // during a measure & layout pass where you cannot change theRecyclerView data.
    // Any method call that might change the structureof the RecyclerView or the adapter
    // contents should be postponed tothe next frame.
    private fun callOnLoadMore(){
        recyclerView.post {
            onLoadMore()
        }
    }

    fun enable(): EndlessRecyclerOnScrollListener {
        enabled = true
        checkIfVisible()
        return this
    }

    fun disable(): EndlessRecyclerOnScrollListener {
        enabled = false
        return this
    }

    abstract fun onLoadMore()
}