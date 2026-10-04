package pynith.apps.nextel.model

import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pynith.apps.nextel.R

class SingleViewHolder(view: View, viewType: Int) : RecyclerView.ViewHolder(view) {

    private val images = listOf(
        R.drawable.ad_img1, R.drawable.ad_img2, R.drawable.ad_img1,
        R.drawable.ad_img2, R.drawable.ad_card2
    )
    private  val position = viewType;

    fun bind(click: () -> Unit) {
        val title = itemView.findViewById<TextView>(R.id.txtTitle)
        val image = itemView.findViewById<ImageView>(R.id.image)
        val forImage = itemView.findViewById<LinearLayout>(R.id.forImage)
        image.setImageResource(images[position])

        // ? Fade + slide animation
        val animation = AnimationUtils.loadAnimation(forImage.context, R.anim.item_fade_slide)
        forImage.startAnimation(animation)

        // ? Click animation (scale)
        forImage.setOnClickListener {
            forImage.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    forImage.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .duration = 100
                }
                .start()

            click()
        }

    }


}