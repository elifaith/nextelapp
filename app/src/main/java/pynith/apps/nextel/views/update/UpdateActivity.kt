package pynith.apps.nextel.views.update

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pynith.apps.nextel.R
import pynith.apps.nextel.helper.ApkDownloader
import pynith.apps.nextel.helper.AppUpdateInfo
import pynith.apps.nextel.helper.UpdateChecker
import pynith.apps.nextel.views.BaseActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


/** Native release-notes page shown after a background update check finds a newer build. */
class UpdateActivity : BaseActivity() {
    private var opening = false
    private var downloading = false
    private var mandatory = false

    private lateinit var updateButton: MaterialButton
    private lateinit var laterButton: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorContainer: View
    private lateinit var errorText: TextView

    private lateinit var downloadContainer: View
    private lateinit var downloadStatusText: TextView
    private lateinit var downloadProgressBar: ProgressBar
    private lateinit var cancelDownloadButton: TextView

    private lateinit var updateInfo: AppUpdateInfo

    private var downloader: ApkDownloader? = null
    private var downloadedFile: File? = null

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

        downloadContainer = findViewById(R.id.downloadProgressContainer)
        downloadStatusText = findViewById(R.id.downloadStatusText)
        downloadProgressBar = findViewById(R.id.downloadProgressBar)
        cancelDownloadButton = findViewById(R.id.cancelDownload)

        updateButton.setOnClickListener {
            openUpdate()
        }

        laterButton.setOnClickListener {
            finish()
        }

        cancelDownloadButton.setOnClickListener {
            cancelDownload()
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

    // ---------------------------------------------------------------------
    // Update action: in-app APK download when the server hosts the file,
    // otherwise fall back to opening the store / browser page.
    // ---------------------------------------------------------------------

    private fun openUpdate() {
        if (downloading) return

        if (downloadedFile != null) {
            promptInstall()
            return
        }

        val serverUrl = updateInfo.serverDownloadUrl
        if (!serverUrl.isNullOrBlank() && isHttpUrl(serverUrl)) {
            beginDownload()
        } else {
            openExternalUpdate()
        }
    }

    private fun isHttpUrl(url: String): Boolean {
        val scheme = Uri.parse(url).scheme
        return scheme == "http" || scheme == "https"
    }

    /** Legacy path: no APK link from the server, so open the store/browser page. */
    private fun openExternalUpdate() {
        val url = updateInfo.serverDownloadUrl
            ?: updateInfo.playStoreUrl

        if (url.isNullOrBlank()) {
            showError("No secure update link is available.")
            return
        }

        opening = true
        renderActionState()

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
            opening = false
            renderActionState()

        } catch (_: ActivityNotFoundException) {
            opening = false
            renderActionState()

            showError(
                "Could not open the update page. " +
                        "Install from the store or enable installs from this source."
            )
        } catch (_: Exception) {
            opening = false
            renderActionState()

            showError(
                "Could not open the update page. Please try again."
            )
        }
    }

    // ---------------------------------------------------------------------
    // In-app APK download
    // ---------------------------------------------------------------------

    /**
     * Android 9 and below save the APK into the public Downloads folder, which
     * needs the read/write storage permission, so it is requested first.
     * Android 10+ use app-scoped storage where no permission is required
     * (and the manifest only declares the storage permission up to API 28).
     */
    private fun beginDownload() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
            return
        }
        startDownload()
    }

    private fun downloadDestination(): File {
        val safeVersion = updateInfo.versionName.replace(Regex("[^A-Za-z0-9._-]"), "-")
        val fileName = "nextel-${if (safeVersion.isBlank()) "update" else safeVersion}.apk"
        val directory = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // Public Downloads folder: requires the storage permission asked for above.
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        } else {
            // App-scoped storage: no permission needed on Android 10+.
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        } ?: filesDir

        return File(directory, fileName)
    }

    private fun startDownload() {
        val url = updateInfo.serverDownloadUrl ?: return

        hideError()
        downloadedFile = null
        downloading = true
        downloadProgressBar.progress = 0
        downloadProgressBar.isIndeterminate = true
        downloadStatusText.text = "Starting download…"
        renderActionState()

        downloader = ApkDownloader.start(
            url = url,
            destination = downloadDestination(),
            onProgress = { bytesRead, totalBytes -> renderDownloadProgress(bytesRead, totalBytes) },
            onComplete = { file -> onDownloaded(file) },
            onError = { message -> onDownloadFailed(message) }
        )
    }

    private fun renderDownloadProgress(bytesRead: Long, totalBytes: Long) {
        if (isFinishing || isDestroyed) return

        if (totalBytes > 0) {
            val percent = ((bytesRead * 100) / totalBytes).toInt()
            downloadProgressBar.isIndeterminate = false
            downloadProgressBar.max = 100
            downloadProgressBar.progress = percent
            downloadStatusText.text =
                "Downloading update… $percent% (${mb(bytesRead)} / ${mb(totalBytes)} MB)"
        } else {
            downloadStatusText.text = "Downloading update… ${mb(bytesRead)} MB"
        }
    }

    private fun onDownloaded(file: File) {
        if (isFinishing || isDestroyed) return

        downloading = false
        downloader = null
        downloadedFile = file
        renderActionState()
        promptInstall()
    }

    private fun onDownloadFailed(message: String) {
        if (isFinishing || isDestroyed) return

        downloading = false
        downloader = null
        renderActionState()
        showError(message)
    }

    private fun cancelDownload() {
        downloader?.cancel()
        downloader = null
        downloading = false
        renderActionState()
    }

    private fun mb(bytes: Long): String =
        String.format(Locale.US, "%.1f", bytes / (1024f * 1024f))

    // ---------------------------------------------------------------------
    // Install
    // ---------------------------------------------------------------------

    /** Prompts the user to install the downloaded APK, asking Android 8+ for the "install unknown apps" permission first. */
    private fun promptInstall() {
        val file = downloadedFile ?: run {
            showError("The update file is missing. Please download it again.")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !packageManager.canRequestPackageInstalls()
        ) {
            requestInstallPermission()
            return
        }

        launchInstall(file)
    }

    private fun requestInstallPermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Allow updates from Nextel")
            .setMessage(
                "To install the downloaded update, Android needs your permission " +
                        "for Nextel to install apps from this source. Enable " +
                        "\"Allow from this source\" on the next screen and the " +
                        "update will continue."
            )
            .setCancelable(false)
            .setPositiveButton("Continue") { _, _ ->
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:$packageName")
                    )
                    startActivityForResult(intent, REQUEST_INSTALL_PERMISSION)
                } catch (_: ActivityNotFoundException) {
                    showError("The install-permission screen is not available on this device.")
                }
            }
            .setNegativeButton("Not now") { _, _ ->
                showError("Install permission is needed to update the app. Tap the button to try again.")
            }
            .show()
    }

    private fun launchInstall(file: File) {
        try {
            val apkUri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showError("No app installer is available on this device.")
        } catch (_: Exception) {
            showError("The update could not be opened for installation.")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_INSTALL_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                packageManager.canRequestPackageInstalls()
            ) {
                downloadedFile?.let { launchInstall(it) }
            } else {
                showError("Install permission was not granted. Tap the button to try again.")
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                startDownload()
            } else {
                showError("Storage permission is required to save the update file. Tap the button to try again.")
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // ---------------------------------------------------------------------
    // Button / progress state
    // ---------------------------------------------------------------------

    private fun renderActionState() {
        val busy = opening || downloading

        updateButton.isEnabled = !busy

        progressBar.visibility = if (opening) {
            View.VISIBLE
        } else {
            View.GONE
        }

        updateButton.text = when {
            downloading -> "Downloading…"
            downloadedFile != null -> "Install update"
            mandatory -> "Update now"
            else -> "Get the update"
        }

        updateButton.icon = if (!busy) {
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

        downloadContainer.visibility = if (downloading) {
            View.VISIBLE
        } else {
            View.GONE
        }

        laterButton.isEnabled = !downloading
    }

    private fun showError(message: String) {
        errorContainer.visibility = View.VISIBLE
        errorText.text = message
    }

    private fun hideError() {
        errorContainer.visibility = View.GONE
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

    override fun onDestroy() {
        downloader?.cancel()
        downloader = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MANDATORY = "mandatory"

        private const val REQUEST_STORAGE_PERMISSION = 4001
        private const val REQUEST_INSTALL_PERMISSION = 4002

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
