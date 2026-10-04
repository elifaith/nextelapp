package pynith.apps.nextel.helper

import android.app.Activity
import android.content.Context
import android.content.Intent
import org.json.JSONObject
import pynith.apps.nextel.BuildConfig
import pynith.apps.nextel.views.update.UpdateActivity

/** Reads a JSON member that may be absent or explicitly null, trimming blank strings to null. */
internal fun JSONObject.nullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).trim().takeIf(String::isNotEmpty)
}

/** The update details returned by the `/app-upgrade` endpoint for this Android install. */
data class AppUpdateInfo(
    val currentBuild: Int,
    val updateRequired: Boolean,
    val versionName: String,
    val buildNumber: Int,
    val minimumSupportedBuild: Int,
    val forceUpdate: Boolean,
    val title: String,
    val releaseNotes: String,
    val serverDownloadUrl: String?,
    val playStoreUrl: String?,
    val publishedAt: String?
) {
    /** A below-minimum install or server-required/forced update cannot be skipped. */
    val isMandatory: Boolean
        get() = updateRequired || forceUpdate || currentBuild < minimumSupportedBuild

    fun toJson(): JSONObject = JSONObject()
        .put("current_build", currentBuild)
        .put("update_required", updateRequired)
        .put("version_name", versionName)
        .put("build_number", buildNumber)
        .put("minimum_supported_build", minimumSupportedBuild)
        .put("force_update", forceUpdate)
        .put("title", title)
        .put("release_notes", releaseNotes)
        .put("server_download_url", serverDownloadUrl ?: JSONObject.NULL)
        .put("play_store_url", playStoreUrl ?: JSONObject.NULL)
        .put("published_at", publishedAt ?: JSONObject.NULL)

    companion object {
        fun fromJson(rawJson: String?): AppUpdateInfo? {
            if (rawJson.isNullOrBlank()) return null

            return try {
                val json = JSONObject(rawJson)
                AppUpdateInfo(
                    currentBuild = json.optInt("current_build", BuildConfig.VERSION_CODE),
                    updateRequired = json.optBoolean("update_required", false),
                    versionName = json.optString("version_name").orEmpty(),
                    buildNumber = json.optInt("build_number", 0),
                    minimumSupportedBuild = json.optInt("minimum_supported_build", 0),
                    forceUpdate = json.optBoolean("force_update", false),
                    title = json.optString("title").takeIf(String::isNotBlank)
                        ?: "A new update is available",
                    releaseNotes = json.optString("release_notes").orEmpty(),
                    serverDownloadUrl = json.nullableString("server_download_url"),
                    playStoreUrl = json.nullableString("play_store_url"),
                    publishedAt = json.nullableString("published_at")
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

/** Persists update state and presents the native update page without blocking app startup. */
class UpdateChecker(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun shouldCheckNow(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return millisUntilNextCheck(nowMillis) == 0L
    }

    fun millisUntilNextCheck(nowMillis: Long = System.currentTimeMillis()): Long {
        val lastCheck = preferences.getLong(KEY_LAST_CHECK_STARTED, 0L)
        if (lastCheck <= 0L) return 0L
        return (lastCheck + CHECK_INTERVAL_MILLIS - nowMillis).coerceAtLeast(0L)
    }

    /** Atomically prevents launch-time and periodic work from querying twice on the same day. */
    fun beginCheckIfDue(nowMillis: Long = System.currentTimeMillis()): Boolean =
        synchronized(checkLock) {
            if (!shouldCheckNow(nowMillis)) return@synchronized false
            preferences.edit().putLong(KEY_LAST_CHECK_STARTED, nowMillis).commit()
        }


    fun saveUpdateInfo(info: AppUpdateInfo) {
        preferences.edit()
            .putString(KEY_UPDATE_INFO, info.toJson().toString())
            .commit()
    }

    fun clearUpdateInfo() {
        preferences.edit()
            .remove(KEY_UPDATE_INFO)
            .remove(KEY_LAST_PROMPTED_BUILD)
            .remove(KEY_LAST_PROMPTED_AT)
            .commit()
    }

    fun getSavedUpdateInfo(): AppUpdateInfo? = AppUpdateInfo.fromJson(
        preferences.getString(KEY_UPDATE_INFO, null)
    )

    /**
     * Opens the update page only from a foreground Activity. Optional updates are prompted at
     * most once a day per build; required updates are shown every time the app returns to the UI.
     */
    fun openUpdatePageIfNeeded(
        activity: Activity,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (activity.isFinishing || activity.isDestroyed || activity is UpdateActivity) return false
        val info = getSavedUpdateInfo() ?: return false

        val packageInfo = activity.packageManager.getPackageInfo(
            activity.packageName,
            0
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val currentVersion = packageInfo.longVersionCode
            if(info.buildNumber <= currentVersion ){
                clearUpdateInfo()
                return false
            }
        } else {
            @Suppress("DEPRECATION")
            val currentVersion = packageInfo.versionCode
            if(info.buildNumber <= currentVersion ){
                clearUpdateInfo()
                return false
            }
        }


        if (!info.isMandatory) {
            val lastBuild = preferences.getInt(KEY_LAST_PROMPTED_BUILD, Int.MIN_VALUE)
            val lastPromptedAt = preferences.getLong(KEY_LAST_PROMPTED_AT, 0L)
            if (lastBuild == info.buildNumber &&
                lastPromptedAt > 0L &&
                nowMillis - lastPromptedAt < CHECK_INTERVAL_MILLIS
            ) {
                return false
            }
        }

        return try {
            activity.startActivity(
                Intent(activity, UpdateActivity::class.java)
                    .putExtra(UpdateActivity.EXTRA_UPDATE_INFO, info.toJson().toString())
            )
            if (!info.isMandatory) {
                preferences.edit()
                    .putInt(KEY_LAST_PROMPTED_BUILD, info.buildNumber)
                    .putLong(KEY_LAST_PROMPTED_AT, nowMillis)
                    .apply()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "app_upgrade_checker"
        private const val KEY_LAST_CHECK_STARTED = "last_check_started_at"
        private const val KEY_UPDATE_INFO = "update_info"
        private const val KEY_LAST_PROMPTED_BUILD = "last_prompted_build"
        private const val KEY_LAST_PROMPTED_AT = "last_prompted_at"
        private const val CHECK_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L
        private val checkLock = Any()
    }
}