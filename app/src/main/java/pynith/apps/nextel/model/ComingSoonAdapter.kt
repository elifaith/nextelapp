package pynith.apps.nextel.model

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import pynith.apps.nextel.R

class ComingSoonAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_SINGLE = 0
        const val TYPE_SLIDER = 1
        const val TYPE_TWO = 2
    }

    var lop = 0

    override fun getItemCount() = 4

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> TYPE_SINGLE
            1 -> TYPE_SLIDER
            2 -> TYPE_TWO
            else -> TYPE_SINGLE
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_SINGLE -> {
                lop += 1
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.row_single, parent, false)
                SingleViewHolder(view,lop)
            }
            TYPE_SLIDER -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.row_slider, parent, false)
                SliderViewHolder(view)
            }
            TYPE_TWO -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.row_two, parent, false)
                TwoViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.row_grid, parent, false)
                GridViewHolder(view)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val click = {
            Toast.makeText(
                context,
                "Coming soon, keep anticipating",
                Toast.LENGTH_SHORT
            ).show()
        }

        when (holder) {
            is SingleViewHolder -> holder.bind(click)
            is SliderViewHolder -> holder.bind(click)
            is TwoViewHolder -> holder.bind(click)
            is GridViewHolder -> holder.bind(click)
        }
    }
}