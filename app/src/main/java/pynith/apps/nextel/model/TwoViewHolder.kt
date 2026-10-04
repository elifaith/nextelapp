package pynith.apps.nextel.model

import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import pynith.apps.nextel.R

class TwoViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun bind(click: () -> Unit) {

        val first = itemView.findViewById<View>(0)
        val second = itemView.findViewById<View>(1)

        val forImage = itemView.findViewById<LinearLayout>(R.id.forImage)
        val forImage2 = itemView.findViewById<View>(R.id.forImage2)

        val image = itemView.findViewById<ImageView>(R.id.image)
        image.setImageResource(R.drawable.ad_long1)

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


        val animation2 = AnimationUtils.loadAnimation(forImage2.context, R.anim.item_fade_slide)
        forImage2.startAnimation(animation2)
        forImage2.setOnClickListener {
            forImage2.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    forImage2.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .duration = 100
                }
                .start()

            click()
        }


        first?.setOnClickListener { click() }
        second?.setOnClickListener { click() }
    }
}
