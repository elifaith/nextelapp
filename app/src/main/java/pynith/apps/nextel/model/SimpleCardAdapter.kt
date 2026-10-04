package pynith.apps.nextel.model

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pynith.apps.nextel.R

class SimpleCardAdapter(
    private val onClick: () -> Unit
) : RecyclerView.Adapter<SimpleCardAdapter.CardViewHolder>() {

    private val items = listOf(
        "Feature March", "Newbie Task", "Voucher Claim",
        "Trading Task", "Team Leader"
    )
    private val images = listOf(
        R.drawable.ad_card1, R.drawable.ad_card2, R.drawable.ad_card3,
        R.drawable.ad_card4, R.drawable.ad_card5,
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card, parent, false)
        return CardViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(items[position], images[position], position, onClick)
    }

    class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(title: String, image:Int, position:Int, click: () -> Unit) {
            val txt = itemView.findViewById<TextView>(R.id.txtTitle)
            val img = itemView.findViewById<ImageView>(R.id.img)
            val badge = itemView.findViewById<TextView>(R.id.badge)
            txt.text = title
            img.setImageResource(image)

            when (position % 3) {
                0 -> {
                    badge.text = "NEW"
                    badge.setBackgroundColor(Color.RED)
                }
                1 -> {
                    badge.text = "HOT"
                    badge.setBackgroundColor(Color.BLUE)
                }
                else -> {
                    badge.text = "SOON"
                    badge.setBackgroundColor(Color.DKGRAY)
                }
            }

            // ? Fade + slide animation
            val animation = AnimationUtils.loadAnimation(itemView.context, R.anim.item_fade_slide)
            itemView.startAnimation(animation)

            // ? Click animation (scale)
            itemView.setOnClickListener {
                itemView.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction {
                        itemView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .duration = 100
                    }
                    .start()

                click()
            }
        }
    }

}