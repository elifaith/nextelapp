package pynith.apps.nextel.games.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import pynith.apps.nextel.R

class FeaturedCardView(
    context: Context,
    imageRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) : FrameLayout(context) {

    init {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.card_feature_view, this, true)

        val image = view.findViewById<ImageView>(R.id.image)
        val titleView = view.findViewById<TextView>(R.id.titleView)
        val subtitleView = view.findViewById<TextView>(R.id.subtitleView)

        image.setImageResource(imageRes)
        titleView.text = title
        subtitleView.text = subtitle

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
    }
}