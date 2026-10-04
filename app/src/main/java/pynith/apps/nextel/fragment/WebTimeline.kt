package pynith.apps.nextel.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.os.*
import android.view.*
import android.webkit.*
import android.widget.*
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import pynith.apps.template.SlidingRootNav
import pynith.apps.nextel.databinding.WebTimelineBinding
import pynith.apps.nextel.helper.SessionService
import pynith.apps.nextel.helper.LogoutCoordinator
import pynith.apps.nextel.views.main.HomeActivity
import pynith.apps.nextel.model.CONData
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class WebTimelines : Fragment() {

    private var _binding: WebTimelineBinding? = null
    private val binding get() = _binding!!

    private lateinit var mContext: Context
    internal var URL = CONData.appURL
    internal var loadingURL = URL;

    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null

    private lateinit var session: SessionService

    private val fileChooserLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->

            var results: Array<Uri>? = null

            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data

                if (data == null) {
                    mCameraPhotoPath?.let { results = arrayOf(it.toUri()) }
                } else {
                    data.dataString?.let {
                        results = arrayOf(it.toUri())
                    }
                }
            }

            mFilePathCallback?.onReceiveValue(results)
            mFilePathCallback = null
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = WebTimelineBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mContext = requireContext()

        val loadThisUrl = activity?.intent?.getStringExtra(HomeActivity.EXTRA_WEB_URL) ?: URL

        session = SessionService(mContext)


        setupWebView(binding.webview) { showErrorPage() }

        loadUrl(loadThisUrl)

        binding.imageView.progress = 50  // 50%
        binding.imageView.max = 100

        // To animate progress
        binding.imageView.progress = 0
        val handler = Handler(Looper.getMainLooper())
        var progress = 0
        val runnable = object : Runnable {
            override fun run() {
                if (progress <= 100) {
                    binding.imageView.progress = progress
                    progress += 1
                    handler.postDelayed(this, 50) // Update every 50ms
                }
            }
        }
        handler.post(runnable)

        binding.btnTryAgain.setOnClickListener {
            binding.webview.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            binding.layoutSplash.visibility = View.VISIBLE
            binding.layoutNoInternet.visibility = View.GONE
            loadUrl(URL)
        }

        Handler().postDelayed(java.lang.Runnable {
            //getUserData()
        }, 10000);
    }

    private fun loadUrl(url: String) {
        binding.layoutSplash.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
        binding.layoutNoInternet.visibility = View.GONE
        binding.webview.loadUrl(url)
    }

    private fun showErrorPage() {
        binding.webview.visibility = View.GONE
        binding.layoutNoInternet.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    // ✅ FIXED: Fragment-safe WebView setup
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView, errorCallback: () -> Unit) {

        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.settings.javaScriptEnabled = true
        webView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT

        webView.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        webView.settings.databaseEnabled = true

        // Security: Use COMPATIBILITY_MODE instead of ALWAYS_ALLOW to prevent loading insecure resources on secure pages
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.settings.builtInZoomControls = false

        webView.settings.domStorageEnabled = true
        // Security: Disable file access if not explicitly needed from the webview
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false

        webView.addJavascriptInterface(
            WebAppInterface(requireContext(),requireActivity()),
            "Android"
        )

        webView.settings.setSupportMultipleWindows(false)
        webView.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressBar.progress = newProgress
                binding.progressBar.visibility = if (newProgress == 100) View.GONE else View.VISIBLE
            }

            // ✅ FIXED for Fragment
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {

                mFilePathCallback?.onReceiveValue(null)
                mFilePathCallback = filePathCallback

                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                }

                fileChooserLauncher.launch(intent)
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {

            val allowedHosts = listOf(CONData.URLWeb, CONData.URLApp)

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
                binding.layoutSplash.visibility = View.GONE
                binding.webview.visibility = View.VISIBLE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    errorCallback()
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {

                val url = request?.url ?: return false
                val urlString = url.toString()
                val host = url.host ?: ""

                loadingURL = urlString


                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, url))
                    true
                } catch (e: ActivityNotFoundException) {
                    false
                }
            }
        }
    }

    // ✅ FILE CREATION (unchanged logic)
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    class WebAppInterface(
        private val context: Context,
        private val activity: Activity
    ) {

        @JavascriptInterface
        fun handleCanvasImage(base64: String) {
            activity.runOnUiThread {
                saveCanvasImage(base64)
            }
        }

        @JavascriptInterface
        fun logout() {
            activity.runOnUiThread {
                LogoutCoordinator.logoutAfterWebSession(context)
            }
        }

        @JavascriptInterface
        fun showMenu() {
            activity.runOnUiThread {
                //dialog.show()
            }
        }

        @JavascriptInterface
        fun handleCanvasShare(base64: String) {
            activity.runOnUiThread {
                shareCanvasImage(base64)
            }
        }

        private fun saveCanvasImage(base64: String) {
            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)

                val file = File(
                    context.getExternalFilesDir(null),
                    "canvas_${System.currentTimeMillis()}.png"
                )

                FileOutputStream(file).use {
                    it.write(bytes)
                }

                Toast.makeText(context, "Image saved", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Download failed", Toast.LENGTH_LONG).show()
            }
        }

        private fun shareCanvasImage(base64Image: String) {
            try {
                val base64Data = base64Image.substringAfter(",")
                val bytes = Base64.decode(base64Data, Base64.DEFAULT)

                val file = File(
                    context.cacheDir,
                    "canvas_${System.currentTimeMillis()}.png"
                )

                FileOutputStream(file).use {
                    it.write(bytes)
                }

                shareImage(file)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Sharing failed", Toast.LENGTH_LONG).show()
            }
        }

        private fun shareImage(file: File) {
            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(
                Intent.createChooser(intent, "Share Canvas Image")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

    }

    companion object {
        fun newInstance(slidingRootNav: SlidingRootNav): WebTimelines{
            val args = Bundle()
            val fragment = WebTimelines()
            fragment.arguments = args
            return fragment
        }
    }

}