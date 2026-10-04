package pynith.apps.nextel.views.us

import android.content.Context
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import pynith.apps.nextel.R
import pynith.apps.nextel.views.BaseActivity

class ActivityFAQs: BaseActivity() {

    private lateinit var mContext: Context


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faqs)

        mContext = this;

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val fab = findViewById<FloatingActionButton>(R.id.closeBtn)

        // Set Toolbar as ActionBar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        fab.setOnClickListener {
            finish()
        }

    }



}