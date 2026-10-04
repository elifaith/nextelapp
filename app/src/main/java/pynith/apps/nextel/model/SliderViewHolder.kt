package pynith.apps.nextel.model

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import pynith.apps.nextel.R

class SliderViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val handler = Handler(Looper.getMainLooper())
    private var scrollPosition = 0

    fun bind(click: () -> Unit) {
        val recycler = itemView.findViewById<RecyclerView>(R.id.recyclerSlider)

        val layoutManager = LinearLayoutManager(
            itemView.context,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        recycler.layoutManager = layoutManager
        recycler.adapter = SimpleCardAdapter(click)


        //autoScroll(recycler, layoutManager)

        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(recycler)

    }

    private fun autoScroll(
        recycler: RecyclerView,
        layoutManager: LinearLayoutManager
    ) {
        handler.postDelayed(object : Runnable {
            override fun run() {
                val itemCount = recycler.adapter?.itemCount ?: 0

                if (itemCount == 0) return

                scrollPosition = (scrollPosition + 1) % itemCount
                recycler.smoothScrollToPosition(scrollPosition)

                handler.postDelayed(this, 3000)
            }
        }, 3000)
    }


}