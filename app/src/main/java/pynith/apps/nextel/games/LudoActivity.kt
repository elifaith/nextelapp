package pynith.apps.nextel.games

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import pynith.apps.nextel.views.BaseActivity

class LudoActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create layout and view via code
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val textView = TextView(this).apply { text = "Hello, this UI was created programmatically!" }

        layout.addView(textView)
        setContentView(layout)
    }
}
