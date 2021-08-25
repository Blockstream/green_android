package com.blockstream.green.views

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// Fixes a nasty RecyclerView bug
// https://github.com/mikepenz/FastAdapter/issues/757
// https://stackoverflow.com/questions/30220771/recyclerview-inconsistency-detected-invalid-item-position/33985508#33985508
class NpaLinearLayoutManager @JvmOverloads constructor(
    context: Context,
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false) : LinearLayoutManager(context,orientation,reverseLayout) {
    override fun supportsPredictiveItemAnimations(): Boolean {
        return false
    }
}