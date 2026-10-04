package pynith.apps.nextel.games

import android.os.Bundle
import android.util.TypedValue
import android.widget.*
import androidx.core.view.setPadding
import pynith.apps.nextel.R
import pynith.apps.nextel.games.widget.FeatureMiddleCardView
import pynith.apps.nextel.games.widget.FeaturedCardSliverView
import pynith.apps.nextel.games.widget.FeaturedCardView
import pynith.apps.nextel.games.widget.SmallFeatureCardView
import pynith.apps.nextel.views.BaseActivity

class AppGameActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(R.attr.colorPrimaryDark)
        }

        // TOP BAR
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16)
        }

        val menuBtn = ImageButton(this).apply {
            setImageResource(R.drawable.left_arrow)
            setBackgroundColor(0x00000000)
            setBackgroundResource(
                TypedValue().let {
                    theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, it, true)
                    it.resourceId
                }
            )
            setPadding(7)
            setOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        val title = TextView(this).apply {
            setText(R.string.games)
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(64, 8, 0, 0)
        }

        topBar.addView(menuBtn)
        topBar.addView(title)

        // CONTENT CONTAINER
        val scrollView = ScrollView(this).apply {
            setPadding(18,0,18,0)
            setBackgroundResource(R.drawable.bg_game)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0,38,0,0)
        }

        content.addView(
            FeaturedCardSliverView(
                context = this,
                imageRes = R.drawable.ludo,
                title = "Featured Ludo Game",
                subtitle = "This is a feature coming soon",
                onClick = {
                    showToast("Vip awesome Ludo game coming soon.")
                }
            )
        )

        // HORIZONTAL LIST
        val horizontalScroll = HorizontalScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        row.addView(
            SmallFeatureCardView(this, "Classic dice", R.drawable.ads_card2, "NEW") {
                showToast("Amazing classic dice game coming soon.")
            }
        )

        row.addView(
            SmallFeatureCardView(this, "The Wheel", R.drawable.ads_card3, "HOT") {
                showToast("Thrilling wheel of fortune game coming soon.")
            }
        )

        row.addView(
            SmallFeatureCardView(this, "Ne Zha Croco", R.drawable.ads_card4, "") {
                showToast("New Awesome Nezha Croco game coming soon.")
            }
        )

        horizontalScroll.addView(row)
        content.addView(horizontalScroll)


        // HORIZONTAL LIST
        val hScroll = HorizontalScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            setPadding(0,38,0,0)
        }
        val roll = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        roll.addView(

            FeatureMiddleCardView(this, "Ludo Games", R.drawable.ludo) {
                showToast("Our awesome Ludo game coming soon.")
            }
        )

        roll.addView(
            FeatureMiddleCardView(this, "Hangman Words", R.drawable.hangman) {
                showToast("Starters Hangman Game Coming Soon.")
            }
        )

        hScroll.addView(roll)
        content.addView(hScroll)


        // FEATURED CARD
        content.addView(
            FeaturedCardView(
                context = this,
                imageRes = R.drawable.hangman,
                title = "Featured Hangman",
                subtitle = "This is a feature coming soon",
                onClick = {
                    showToast("Hangman game advance words coming soon.")
                }
            )
        )

        scrollView.addView(content)

        root.addView(topBar)
        root.addView(scrollView)

        setContentView(root)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

}