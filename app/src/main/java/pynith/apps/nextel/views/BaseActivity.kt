package pynith.apps.nextel.views

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import pynith.apps.nextel.R
import pynith.apps.nextel.helper.AppConfig

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppConfig.load(this)

        when (AppConfig.themeName) {
            "Dark" -> setTheme(R.style.Theme_MyApp_Dark)
            "Light" -> setTheme(R.style.Theme_MyApp_Light)
            "Blue" -> setTheme(R.style.Theme_MyApp_Blue)
        }

        super.onCreate(savedInstanceState)
    }

    override fun attachBaseContext(newBase: Context) {
        AppConfig.load(newBase)

        val config = Configuration(newBase.resources.configuration)
        config.fontScale = AppConfig.fontScale

        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onResume() {
        super.onResume()

        /*val root = findViewById<ViewGroup>(android.R.id.content)
        val typeface = ResourcesCompat.getFont(this, AppConfig.getFontRes())

        typeface?.let {
            applyFontToView(root, it)
        }*/
    }

    fun applyFontToView(view: View, typeface: Typeface) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyFontToView(view.getChildAt(i), typeface)
            }
        } else if (view is TextView) {
            view.typeface = typeface
        }
    }

}