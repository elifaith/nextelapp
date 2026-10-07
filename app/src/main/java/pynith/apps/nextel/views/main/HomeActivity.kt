package pynith.apps.nextel.views.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction

import com.google.android.material.floatingactionbutton.FloatingActionButton
import pynith.apps.template.SlidingRootNav
import pynith.apps.nextel.R
import pynith.apps.nextel.fragment.BaseActivity
import pynith.apps.nextel.helper.SessionService
import pynith.apps.nextel.helper.LogoutCoordinator
import pynith.apps.nextel.helper.WebSessionHandoff
import pynith.apps.nextel.model.CONData
import pynith.apps.nextel.views.auth.LoginActivity
import pynith.apps.nextel.views.settings.AppSettingsActivity
import pynith.apps.nextel.views.coupon.CouponSearchActivity
import pynith.apps.nextel.views.us.SupportActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class HomeActivity : BaseActivity() {

    private lateinit var mContext: Context
    internal var mLoaded = false
    // set your custom url here
    internal var URL = CONData.appURL

    public lateinit var session: SessionService

    //for attach files
    private var mCameraPhotoPath: String? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    internal var doubleBackToExitPressedOnce = false


    private lateinit var btnTryAgain: Button
    private lateinit var mWebView: WebView
    private lateinit var prgs: ProgressBar
    private var viewSplash: View? = null
    lateinit var layoutSplash: RelativeLayout
    private lateinit var layoutWebview: RelativeLayout
    private lateinit var horizontal: ProgressBar
    private lateinit var layoutNoInternet: RelativeLayout


    companion object {
        internal var TAG = "---HomeActivity"
        const val INPUT_FILE_REQUEST_CODE = 1
        const val HOME_VIEW = 0
        const val NEWS_TIMELINE = 1
        const val DISCOVERY_VIEW = 2

        const val EXTRA_WEB_URL = "pynith.apps.nextel.extra.WEB_URL"
        const val EXTRA_WEB_PATH = "pynith.apps.nextel.extra.WEB_PATH"

        var notify: MenuItem? = null
        var createPost: MenuItem? = null

        fun newInstance(context: Context): Intent {
            return Intent(context, HomeActivity::class.java)
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.web_timeline
    }

    override fun initViews() = Unit

    override fun hasNavigation(): Boolean {
        return true
    }

    override fun initFunction() {
        // code go here
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val suppliedUrl = intent.getStringExtra(EXTRA_WEB_URL)
        val requestedPath = intent.getStringExtra(EXTRA_WEB_PATH)
        val loadThisUrl = when {
            suppliedUrl != null && isTrustedWebUrl(Uri.parse(suppliedUrl)) -> suppliedUrl
            requestedPath != null -> webUrlForPath(requestedPath)
            else -> URL
        }

        mContext = this
        mWebView = findViewById<View>(R.id.webview) as WebView
        prgs = findViewById<View>(R.id.progressBar) as ProgressBar
        btnTryAgain = findViewById<View>(R.id.btn_try_again) as Button
        viewSplash = findViewById<View>(R.id.view_splash) as View
        layoutWebview = findViewById<View>(R.id.layout_webview) as RelativeLayout
        layoutNoInternet = findViewById<View>(R.id.layout_no_internet) as RelativeLayout
        /** Layout of Splash screen View  */
        layoutSplash = findViewById<View>(R.id.layout_splash) as RelativeLayout
        horizontal = findViewById<View>(R.id.imageView) as ProgressBar
        session = SessionService(mContext)
        val fab = findViewById<FloatingActionButton>(R.id.fabMenu)


        fab.setOnClickListener {
            mWebView.evaluateJavascript(
                "javascript:settings()",
                null
            )
        }


        setupWebView(mWebView) { showErrorPage() }

        if (!session.isLoggedIn()) {
            openNativeLogin()
            return
        }
        currentWebUrl = loadThisUrl
        loadUrl(loadThisUrl)

        //imageView.load(R.mipmap.loading)
        horizontal.progress = 50  // 50%
        horizontal.max = 100

        // To animate progress
        horizontal.progress = 0
        val handler = Handler(Looper.getMainLooper())
        var progress = 0
        val runnable = object : Runnable {
            override fun run() {
                if (progress <= 100) {
                    horizontal.progress = progress
                    progress += 1
                    handler.postDelayed(this, 50) // Update every 50ms
                }
            }
        }
        handler.post(runnable)

        btnTryAgain.setOnClickListener {
            mWebView.visibility = View.GONE
            prgs.visibility = View.VISIBLE
            layoutSplash.visibility = View.VISIBLE
            layoutNoInternet.visibility = View.GONE
            loadUrl(currentWebUrl)
        }

    }

    override fun onBackPressed() {

        if (mWebView.canGoBack()) {
            mWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private var currentWebUrl: String = URL
    private var hasMainFrameError = false
    private var isHandlingWebLogout = false

    private fun loadUrl(url: String) {
        hasMainFrameError = false
        currentWebUrl = url
        layoutSplash.visibility = View.VISIBLE
        prgs.visibility = View.GONE
        layoutNoInternet.visibility = View.GONE
        mWebView.loadUrl(url)
    }

    fun showErrorPage() {
        hasMainFrameError = true
        prgs.visibility = View.GONE
        layoutSplash.visibility = View.GONE
        mWebView.visibility = View.GONE
        layoutNoInternet.visibility = View.VISIBLE
    }

    fun openLivewirePath(path: String) {
        loadUrl(webUrlForPath(path))
    }

    private fun webUrlForPath(path: String): String {
        val normalized = "/" + path.trimStart('/')
        return Uri.parse(CONData.WebPage).buildUpon().encodedPath(normalized).build().toString()
    }

    private fun isTrustedWebUrl(uri: Uri): Boolean {
        val trustedOrigin = Uri.parse(CONData.WebPage)
        val host = uri.host ?: return false
        val trustedHost = trustedOrigin.host ?: return false
        return uri.scheme in setOf("http", "https") &&
            uri.scheme.equals(trustedOrigin.scheme, ignoreCase = true) &&
            host.equals(trustedHost, ignoreCase = true) && uri.port == trustedOrigin.port
    }

    private fun beginNativeLogoutFromWeb() {
        if (isHandlingWebLogout) return
        isHandlingWebLogout = true
        LogoutCoordinator.logoutAfterWebSession(this)
    }

    private fun openNativeLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }

    private fun handleWebNavigation(uri: Uri): Boolean {
        if (isTrustedWebUrl(uri)) {
            if (uri.path == "/mini-app" && uri.getQueryParameter("logged_out") == "1") {
                beginNativeLogoutFromWeb()
                return true
            }
            if (uri.path == "/login" || uri.path == "/auth/login") {
                if (session.getToken().isNullOrBlank()) {
                    openNativeLogin()
                } else {
                    WebSessionHandoff.openLivewire(this) { message ->
                        if (!isFinishing && !isDestroyed) Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
                return true
            }
            currentWebUrl = uri.toString()
            return false
        }

        if (uri.scheme !in setOf("http", "https", "mailto", "tel")) return true
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: ActivityNotFoundException) {
            true
        }
    }


    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }

        var results: Array<Uri>? = null

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                // If there is not data, then we may have taken a photo
                mCameraPhotoPath?.let { results = arrayOf(it.toUri()) }
            } else {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(dataString.toUri())
                }
            }
        }

        mFilePathCallback!!.onReceiveValue(results)
        mFilePathCallback = null
        return
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack()
            return true
        }

        if (doubleBackToExitPressedOnce) {
            return super.onKeyDown(keyCode, event)
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show()

        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        return true
    }

    fun setupWebView(webView: WebView, errorCallback: () -> Unit) {

        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.settings.javaScriptEnabled = true
        webView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT

        // Block insecure subresources on secure web sessions.
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.settings.builtInZoomControls = false

        webView.settings.domStorageEnabled = true
        // Security: Disable file access if not explicitly needed from the webview
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)

        webView.addJavascriptInterface(
            WebAppInterface(this, slidingRootNav),
            "Android"
        )

        webView.settings.setSupportMultipleWindows(false)

        webView.webViewClient = object : WebViewClient() {

            //Over load all url page
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                return handleWebNavigation(uri)
            }

            // For older Android versions
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val uri = url?.let(Uri::parse) ?: return false
                return handleWebNavigation(uri)
            }

            //Page starts loading
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                val pageUri = url?.let(Uri::parse)
                if (pageUri != null && isTrustedWebUrl(pageUri) &&
                    pageUri.path == "/mini-app" && pageUri.getQueryParameter("logged_out") == "1"
                ) {
                    view?.stopLoading()
                    beginNativeLogoutFromWeb()
                    return
                }
                hasMainFrameError = false
                view?.apply {
                    requestLayout()
                    invalidate()
                }

                if (prgs.visibility == View.GONE) {
                    prgs.visibility = View.VISIBLE
                }
                super.onPageStarted(view, url, favicon)
            }

            // Handles page complete load
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (hasMainFrameError) return

                if (prgs.visibility == View.VISIBLE)
                    prgs.visibility = View.GONE

                mWebView.visibility = View.VISIBLE
                layoutSplash.visibility = View.GONE
                layoutNoInternet.visibility = View.GONE



            }

            override fun onLoadResource(view: WebView, url: String) {
                super.onLoadResource(view, url)
            }

            // Handles most loading errors
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    errorCallback()
                }
            }

            // Handles HTTP errors (404, 500)
            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                if (request?.isForMainFrame == true) {
                    errorCallback()
                }
            }

            // Handles SSL errors
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                // Security: Explicitly cancel on SSL error to prevent MITM attacks
                handler?.cancel()
                errorCallback()
            }
        }

        //file attach request
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                prgs.progress = newProgress
                prgs.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
            }

            override fun onShowFileChooser(
                webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
                if (mFilePathCallback != null) {
                    mFilePathCallback!!.onReceiveValue(null)
                }
                mFilePathCallback = filePathCallback

                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent!!.resolveActivity(this@HomeActivity.packageManager) != null) {
                    // Create the File where the photo should go
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                        // takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath) // This was redundant
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        Log.e(TAG, "Unable to create Image File", ex)
                    }

                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            this@HomeActivity,
                            this@HomeActivity.packageName + ".provider",
                            photoFile
                        )
                        mCameraPhotoPath = photoURI.toString()
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    } else {
                        takePictureIntent = null
                    }
                }

                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "image/*"

                val intentArray: Array<Intent?> = if (takePictureIntent != null) {
                    arrayOf(takePictureIntent)
                } else {
                    arrayOfNulls(0)
                }

                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)

                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)

                return true
            }
        }


    }

    class WebAppInterface(private val activity: Activity, private val slidingRootNav: SlidingRootNav) {
        @JavascriptInterface
        fun handleCanvasImage(base64: String) {
            activity.runOnUiThread {
                saveCanvasImage(activity, base64, "NovaPNL")
            }
        }

        @JavascriptInterface
        fun logout() {
            activity.runOnUiThread {
                LogoutCoordinator.logoutAfterWebSession(activity)
            }
        }

        @JavascriptInterface
        fun showMenu() {
            activity.runOnUiThread {
                slidingRootNav.openMenu()
            }
        }

        @JavascriptInterface
        fun showProfile() {
            activity.runOnUiThread {
                (activity as? HomeActivity)?.openLivewirePath("/dashboard/profile")
            }
        }

        @JavascriptInterface
        fun openAppSettings() {
            activity.runOnUiThread {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.startActivity(Intent(activity, AppSettingsActivity::class.java))
                }
            }
        }

        @JavascriptInterface
        fun openSupportTickets() {
            activity.runOnUiThread {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    val accessToken = (activity as? HomeActivity)?.session?.getToken()
                        ?: SessionService(activity).getToken()
                    activity.startActivity(SupportActivity.createIntent(activity, accessToken))
                }
            }
        }

        @JavascriptInterface
        fun openCouponSearch() {
            activity.runOnUiThread {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    activity.startActivity(Intent(activity, CouponSearchActivity::class.java))
                }
            }
        }

        @JavascriptInterface
        fun handleCanvasShare(base64: String) {
            activity.runOnUiThread {
                shareCanvasImage(activity, base64)
            }
        }

        fun saveCanvasImage(context: Context, base64: String, name: String) {
            try {
                val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                val filename = "${name}-${System.currentTimeMillis()}.png"

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // ✅ Android 10+
                    val resolver = context.contentResolver
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/NovaPNL")
                    }

                    val uri = resolver.insert(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )

                    uri?.let {
                        resolver.openOutputStream(it)?.use { stream ->
                            stream.write(bytes)
                        }
                    }

                    Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                } else {
                    // ⚠️ Android 6–9 (API 23–28)
                    if (hasStoragePermission(activity)) {
                        saveCanvasImageLess(activity, base64, name)
                    } else {
                        requestStoragePermission(activity)
                    }

                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Download failed", Toast.LENGTH_LONG).show()
            }
        }

        fun saveCanvasImageLess(context: Context, base64: String, name: String) {
            try {
                val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                val filename = "${name}-${System.currentTimeMillis()}.png"

                val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_PICTURES
                )

                val file = File(picturesDir, filename)
                FileOutputStream(file).use { it.write(bytes) }

                // 🔄 Make it visible in gallery
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("image/png"),
                    null
                )

                Toast.makeText(context, "Image saved to gallery", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Download failed", Toast.LENGTH_LONG).show()
            }
        }

        fun hasStoragePermission(context: Context): Boolean {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                true // no permission needed on Android 10+
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }

        fun requestStoragePermission(activity: Activity) {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    100
                )
            }
        }



        fun shareCanvasImage(context: Context, base64Image: String) {
            try {

                val base64Data = base64Image.substringAfter(",")
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)

                val file = File(
                    context.cacheDir,
                    "canvas_${System.currentTimeMillis()}.png"
                )

                val fos = FileOutputStream(file)
                fos.write(bytes)
                fos.flush()
                fos.close()

                shareImage(context, file)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Sharing failed", Toast.LENGTH_LONG).show()
            }
        }

        private fun shareImage(context: Context, file: File) {

            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/png"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            context.startActivity(
                Intent.createChooser(intent, "Share Canvas Image")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                // ✅ Permission granted → now save
                Toast.makeText(this, "Permission Now you can save", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadFragment(fg: Fragment, tag: String) {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fg)
        transaction.addToBackStack(tag)
        transaction.commit()
    }

}