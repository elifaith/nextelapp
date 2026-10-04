package pynith.apps.nextel.helper

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import pynith.apps.nextel.App
import pynith.apps.nextel.BuildConfig
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Checks the upgrade endpoint on WorkManager's background executor. */
class UpdateCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val checker = UpdateChecker(applicationContext)
        if (runAttemptCount == 0 && !checker.beginCheckIfDue()) {
            if (inputData.getBoolean(KEY_IS_PERIODIC, false)) {
                UpdateCheckScheduler.scheduleCatchUp(
                    applicationContext,
                    checker.millisUntilNextCheck()
                )
            }
            return Result.success()
        }

        return try {
            val outcome = withContext(Dispatchers.IO) { fetchUpdate() }
            when (outcome) {
                is FetchOutcome.UpToDate -> checker.clearUpdateInfo()
                is FetchOutcome.Available -> checker.saveUpdateInfo(outcome.info)
                // A missing feed must not burn the daily budget on retries; keep the last
                // known state and let the next scheduled window try again.
                is FetchOutcome.Unavailable ->
                    Log.w(TAG, "Upgrade endpoint is not available (HTTP ${outcome.code}).")
            }
            (applicationContext as? App)?.onUpdateCheckFinished()
            Result.success()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, "Update check failed; WorkManager will retry.", error)
            Result.retry()
        }
    }

    private fun fetchUpdate(): FetchOutcome {
        val baseUrl = "https://fakelife.online/api/v1"
        val url = baseUrl.toHttpUrlOrNull()
            ?.newBuilder()
            ?.addPathSegments("app-upgrade")
            ?.addQueryParameter("platform", "android")
            ?.addQueryParameter("current_version", BuildConfig.VERSION_NAME)
            ?.addQueryParameter("current_build", BuildConfig.VERSION_CODE.toString())
            ?.build()
            ?: throw IOException("Configured API base URL is not a valid HTTP URL.")

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .get()
            .build()

        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                if (response.code in NOT_RETRIED_CODES) return FetchOutcome.Unavailable(response.code)
                throw IOException("Upgrade endpoint returned HTTP ${response.code}.")
            }

            val body = response.body?.string()
                ?: throw IOException("Upgrade endpoint returned an empty response.")
            val data = JSONObject(body).optJSONObject("data")
                ?: throw IOException("Upgrade endpoint response did not contain data.")
            if (!data.optBoolean("update_available", false)) {
                return FetchOutcome.UpToDate
            }

            val latest = data.optJSONObject("latest")
                ?: throw IOException("Upgrade endpoint did not include latest release details.")

            FetchOutcome.Available(
                AppUpdateInfo(
                    // The installed build is a local fact, not the server's opinion.
                    currentBuild = BuildConfig.VERSION_CODE,
                    updateRequired = data.optBoolean("update_required", false),
                    versionName = latest.optString("version_name").orEmpty(),
                    buildNumber = latest.optInt("build_number", 0),
                    minimumSupportedBuild = latest.optInt("minimum_supported_build", 0),
                    forceUpdate = latest.optBoolean("force_update", false),
                    title = latest.optString("title").takeIf(String::isNotBlank)
                        ?: DEFAULT_TITLE,
                    releaseNotes = latest.optString("release_notes").orEmpty(),
                    serverDownloadUrl = latest.nullableString("server_download_url"),
                    playStoreUrl = latest.nullableString("play_store_url"),
                    publishedAt = latest.nullableString("published_at")
                )
            )
        }
    }

    private sealed class FetchOutcome {
        data object UpToDate : FetchOutcome()
        data class Available(val info: AppUpdateInfo) : FetchOutcome()
        data class Unavailable(val code: Int) : FetchOutcome()
    }

    companion object {
        const val KEY_IS_PERIODIC = "is_periodic_update_check"
        private const val TAG = "UpdateCheckWorker"
        private const val DEFAULT_TITLE = "A new update is available"

        /** HTTP statuses that signal a contract mismatch rather than a transient outage. */
        private val NOT_RETRIED_CODES = setOf(404, 405)

        private val httpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}