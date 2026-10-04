package pynith.apps.nextel.views.main

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import pynith.apps.nextel.R
import pynith.apps.nextel.fragment.SettingsFragment
import pynith.apps.nextel.views.BaseActivity

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.container, SettingsFragment())
            .commit()

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        /*toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }*/
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}