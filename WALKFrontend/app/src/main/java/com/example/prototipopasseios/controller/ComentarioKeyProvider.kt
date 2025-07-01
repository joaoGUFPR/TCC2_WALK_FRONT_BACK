package com.example.prototipopasseios.controller

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.widget.RecyclerView

class ComentarioKeyProvider(private val adapter: ComentarioPasseioAdapter) :
    ItemKeyProvider<Long>(SCOPE_CACHED) {
    override fun getKey(position: Int): Long? = adapter.getItemId(position)

    override fun getPosition(key: Long): Int {
        for (i in 0 until adapter.itemCount) {
            if (adapter.getItemId(i) == key) return i
        }
        return RecyclerView.NO_POSITION
    }
}

class ComentarioDetailsLookup(private val recyclerView: RecyclerView) :
    ItemDetailsLookup<Long>() {
    override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
        val view = recyclerView.findChildViewUnder(e.x, e.y)
        return if (view != null) {
            val holder = recyclerView.getChildViewHolder(view) as ComentarioPasseioAdapter.ComentarioPasseioViewHolder
            holder.getItemDetails()
        } else {
            null
        }
    }
}
