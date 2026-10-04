package pynith.apps.nextel.games.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import pynith.apps.nextel.R

class FeaturedCardSliverView(
    context: Context,
    imageRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) : FrameLayout(context) {

    init {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.card_feature_silver, this, true)

        val image = view.findViewById<ImageView>(R.id.image)
        val titleView = view.findViewById<TextView>(R.id.txtTitle)
        val subtitleView = view.findViewById<TextView>(R.id.subtitleView)

        image.setImageResource(imageRes)
        titleView.text = title
        subtitleView.text = subtitle

        alpha = 0f
        translationY = 100f

        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.scaleX = 0.96f
                    v.scaleY = 0.96f
                }
                MotionEvent.ACTION_UP -> {
                    v.scaleX = 1f
                    v.scaleY = 1f
                    performClick()
                }
                MotionEvent.ACTION_CANCEL -> {
                    v.scaleX = 1f
                    v.scaleY = 1f
                }
            }
            false
        }

        setOnClickListener { onClick() }

        // Animate like Flutter
        animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .start()
    }
}