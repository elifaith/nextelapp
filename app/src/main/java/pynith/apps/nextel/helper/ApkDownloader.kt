package pynith.apps.nextel.helper

import android.os.Handler
import android.os.Looper
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Streams an update APK to disk with progress reporting, and can be cancelled
 * at any time. The download writes to a ".part" file first and only swaps in
 * the final name once the body is complete, so a partial download can never
 * be handed to the package installer.
 */
class ApkDownloader private constructor(
    private val call: Call
) {
    var canceled: Boolean = false
        private set

    fun cancel() {
        canceled = true
        call.cancel()
    }

    companion object {
        /** Minimum time between progress callbacks posted to the UI thread. */
        private const val PROGRESS_INTERVAL_MILLIS = 100L

        private val mainHandler = Handler(Looper.getMainLooper())

        /**
         * Starts a cancellable download of [url] into [destination].
         * All callbacks are invoked on the main thread.
         */
        fun start(
            url: String,
            destination: File,
            onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> },
            onComplete: (File) -> Unit,
            onError: (message: String) -> Unit
        ): ApkDownloader {
            val request = Request.Builder().url(url).get().build()
            val call = httpClient.newCall(request)
            val downloader = ApkDownloader(call)

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, error: IOException) {
                    cleanupTemp(destination)
                    if (downloader.canceled) return
                    post { onError(error.localizedMessage ?: "The download could not be started.") }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val body = it.body
                        if (!it.isSuccessful || body == null) {
                            cleanupTemp(destination)
                            val message = if (it.isSuccessful) {
                                "The server returned an empty response."
                            } else {
                                "The server returned HTTP ${it.code} while downloading the update."
                            }
                            post { onError(message) }
                            return
                        }

                        val total = body.contentLength()
                        val temp = File(destination.absolutePath + ".part")
                        try {
                            destination.parentFile?.mkdirs()

                            var downloaded = 0L
                            var lastPosted = 0L
                            body.byteStream().use { input ->
                                temp.outputStream().use { output ->
                                    val buffer = ByteArray(BUFFER_SIZE)
                                    var read = input.read(buffer)
                                    while (read != -1) {
                                        output.write(buffer, 0, read)
                                        downloaded += read
                                        val now = System.currentTimeMillis()
                                        if (now - lastPosted >= PROGRESS_INTERVAL_MILLIS) {
                                            lastPosted = now
                                            post { onProgress(downloaded, total) }
                                        }
                                        read = input.read(buffer)
                                    }
                                    output.flush()
                                }
                            }

                            if (downloader.canceled) {
                                temp.delete()
                                return
                            }
                            if (downloaded <= 0L) {
                                throw IOException("The downloaded file is empty.")
                            }
                            if (total > 0 && downloaded != total) {
                                throw IOException("The download ended before it was complete.")
                            }
                            if (!temp.renameTo(destination)) {
                                temp.copyTo(destination, overwrite = true)
                                temp.delete()
                            }

                            post { onProgress(downloaded, if (total > 0) total else downloaded) }
                            post { onComplete(destination) }
                        } catch (error: Exception) {
                            temp.delete()
                            if (downloader.canceled) return
                            post {
                                onError(error.localizedMessage ?: "The download failed. Please try again.")
                            }
                        }
                    }
                }
            })

            return downloader
        }

        private fun post(action: () -> Unit) = mainHandler.post(action)

        private fun cleanupTemp(destination: File) {
            File(destination.absolutePath + ".part").delete()
        }

        /**
         * Downloads can be large, so there is no whole-call timeout (read
         * timeout still guards a stalled connection). Cookies come from the
         * shared WebView cookie store via [WebCookieJar], so the app_gate
         * cookie is sent automatically when the update is hosted on the
         * gated web domain.
         */
        private val httpClient: OkHttpClient = OkHttpClient.Builder()
            .cookieJar(WebCookieJar)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()

        private const val BUFFER_SIZE = 8 * 1024
    }
}
