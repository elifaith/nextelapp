package pynith.apps.nextel.views.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.button.MaterialButton
import pynith.apps.nextel.R
import pynith.apps.nextel.helper.AppUpdateInfo
import pynith.apps.nextel.helper.UpdateChecker
import pynith.apps.nextel.views.BaseActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone



/** Native release-notes page shown after a background update check finds a newer build. */
class UpdateActivity : BaseActivity() {
    private var opening = false
    private var mandatory = false

    private lateinit var updateButton: MaterialButton
    private lateinit var laterButton: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorContainer: View
    private lateinit var errorText: TextView

    private lateinit var updateInfo: AppUpdateInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val suppliedInfo = AppUpdateInfo.fromJson(intent.getStringExtra(EXTRA_UPDATE_INFO))
        updateInfo = suppliedInfo ?: UpdateChecker(this).getSavedUpdateInfo() ?: run {
            finish()
            return
        }

        mandatory = intent.getBooleanExtra(EXTRA_MANDATORY, false)
                || updateInfo.forceUpdate == true
                || updateInfo.updateRequired == true

        setContentView(R.layout.activity_update)

        setupViews()
        populateContent()
        setupBackHandling()
    }

    private fun setupViews() {
        updateButton = findViewById(R.id.updateButton)
        laterButton = findViewById(R.id.laterButton)
        progressBar = findViewById(R.id.updateProgress)
        errorContainer = findViewById(R.id.errorContainer)
        errorText = findViewById(R.id.errorText)

        updateButton.setOnClickListener {
            openUpdate()
        }

        laterButton.setOnClickListener {
            finish()
        }

        laterButton.visibility = if (mandatory) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun populateContent() {
        val info = updateInfo

        val title = findViewById<TextView>(R.id.titleText)
        val subtitle = findViewById<TextView>(R.id.subtitleText)
        val actionRequired = findViewById<TextView>(R.id.actionRequired)

        val currentVersion = findViewById<TextView>(R.id.currentVersion)
        val latestVersion = findViewById<TextView>(R.id.latestVersion)
        val releaseDate = findViewById<TextView>(R.id.releaseDate)

        val requiredNotice = findViewById<View>(R.id.requiredNotice)

        title.text = if (mandatory) {
            "Update required"
        } else {
            "A new version is here"
        }

        if(mandatory){
            title.visibility = View.GONE
        }

        subtitle.text = if (mandatory) {
            "Please update Nextel Connect to continue using the app."
        } else {
            "We’ve made improvements to make your experience better."
        }

        actionRequired.visibility = if (mandatory) {
            View.VISIBLE
        } else {
            View.GONE
        }

        currentVersion.text = getInstalledVersion()
        latestVersion.text = info.versionName
            .takeIf { it.isNotBlank() }
            ?: "New version"

        val date = formatDate(info.publishedAt)

        releaseDate.text = if (date.isNotEmpty()) {
            "Released $date"
        } else {
            ""
        }

        releaseDate.visibility = if (date.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        requiredNotice.visibility = if (mandatory) {
            View.VISIBLE
        } else {
            View.GONE
        }

        populateChangelog(info.releaseNotes)
    }

    private fun populateChangelog(rawNotes: String?) {
        val notes = ChangelogFormatter.lines(rawNotes)

        val section = findViewById<View>(R.id.whatsNewSection)
        val notesContainer = findViewById<LinearLayout>(R.id.notesContainer)

        if (notes.isEmpty()) {
            section.visibility = View.GONE
            return
        }

        section.visibility = View.VISIBLE
        notesContainer.removeAllViews()

        notes.forEach { note ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, dp(14))
            }

            val bullet = View(this).apply {
                setBackgroundResource(R.drawable.left_arrow)
            }

            row.addView(
                bullet,
                LinearLayout.LayoutParams(
                    dp(7),
                    dp(7)
                ).apply {
                    topMargin = dp(6)
                    marginEnd = dp(12)
                }
            )

            val text = TextView(this).apply {
                this.text = note
                setTextColor(Color.parseColor("#33443C"))
                textSize = 14f
                setLineSpacing(0f, 1.5f)
            }

            row.addView(
                text,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            notesContainer.addView(row)
        }
    }

    private fun openUpdate() {
        val info = updateInfo ?: run {
            showError("No secure update link is available.")
            return
        }

        val url = info.serverDownloadUrl
            ?: info.playStoreUrl

        if (url.isNullOrBlank()) {
            showError("No secure update link is available.")
            return
        }

        setOpening(true)

        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            ).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }

            startActivity(intent)

            // If Android accepted the intent, the browser/store is opening.
            // We don't need to keep the loading state forever.
            setOpening(false)

        } catch (_: ActivityNotFoundException) {
            setOpening(false)

            showError(
                "Could not open the update page. " +
                        "Install from the store or enable installs from this source."
            )
        } catch (_: Exception) {
            setOpening(false)

            showError(
                "Could not open the update page. Please try again."
            )
        }
    }

    private fun setOpening(value: Boolean) {
        opening = value

        updateButton.isEnabled = !value
        progressBar.visibility = if (value) {
            View.VISIBLE
        } else {
            View.GONE
        }

        updateButton.text = if (value) {
            ""
        } else {
            if (mandatory) {
                "Update now"
            } else {
                "Get the update"
            }
        }

        updateButton.icon = if (!value) {
            ContextCompat.getDrawable(
                this,
                if (mandatory) {
                    R.drawable.phone_vibrate
                } else {
                    R.drawable.user_profile
                }
            )
        } else {
            null
        }
    }

    private fun showError(message: String) {
        errorContainer.visibility = View.VISIBLE
        errorText.text = message
    }

    private fun setupBackHandling() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!mandatory) {
                        finish()
                    }
                }
            }
        )
    }

    private fun getInstalledVersion(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                0
            )

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
        } catch (_: Exception) {
            "Current version"
        }
    }

    private fun formatDate(value: String?): String {
        if (value.isNullOrBlank()) {
            return ""
        }

        return try {
            val parser = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.US
            )

            parser.timeZone = TimeZone.getTimeZone("UTC")

            val date = parser.parse(value) ?: return ""

            SimpleDateFormat(
                "d MMM yyyy",
                Locale.US
            ).format(date)

        } catch (_: Exception) {
            ""
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }


    companion object {
        const val EXTRA_MANDATORY = "mandatory"

        private const val BRAND_COLOR = "#198754"
        private const val BACKGROUND_COLOR = "#F5F8F6"
        private const val TEXT_COLOR = "#17231E"
        private const val MUTED_COLOR = "#68776F"
        private const val NEW_VERSION_COLOR = "#198754"
        const val EXTRA_UPDATE_INFO = "pynith.apps.nextel.extra.UPDATE_INFO"
    }
}

/**
 * Equivalent of the Flutter ChangelogFormatter.
 */
object ChangelogFormatter {

    fun lines(raw: String?): List<String> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }

        return raw
            .split(Regex("\\r\\n|\\n|\\r"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { stripBullet(it) }
            .filter { it.isNotEmpty() }
    }

    private fun stripBullet(line: String): String {
        return line.replaceFirst(
            Regex("^([-*•]|\\d+[.)])\\s+"),
            ""
        )
    }
}
