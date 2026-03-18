package com.apollo.socially.ui.profile

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridSpacingDecoration(private val spanCount: Int, private val spacingPx: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount
        outRect.left = if (column == 0) 0 else spacingPx
        outRect.right = if (column == spanCount - 1) 0 else spacingPx
        outRect.top = if (position < spanCount) 0 else spacingPx
        outRect.bottom = 0
    }
}
