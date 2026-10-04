package pynith.apps.nextel.views.us

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import pynith.apps.nextel.R
import pynith.apps.nextel.helper.SessionService
import pynith.apps.nextel.views.BaseActivity

class AboutActivity: BaseActivity() {

    private lateinit var mContext: Context


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        mContext = this;

        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        // Set Toolbar as ActionBar
        setSupportActionBar(toolbar)

        // Enable back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Handle back button click
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

    }

    private fun onLinkClick(url: String?) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        mContext.startActivity(intent)
    }


    fun onInstagramClick(view: View?) {
        onLinkClick(mContext.getString(R.string.settings_instagram_url))
    }

    fun onGitHubClick(view: View?) {
        startActivity(SupportActivity.createIntent(mContext, SessionService(mContext).getToken()))
    }


}