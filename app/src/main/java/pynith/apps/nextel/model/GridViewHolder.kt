package pynith.apps.nextel.model

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pynith.apps.nextel.R

class GridViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun bind(click: () -> Unit) {
        val recycler = itemView.findViewById<RecyclerView>(R.id.recyclerGrid)

        recycler.layoutManager = GridLayoutManager(itemView.context, 2)
        recycler.adapter = SimpleCardAdapter(click)
    }
}