package pynith.apps.nextel.helper

import android.content.Context
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.content.res.ResourcesCompat
import pynith.apps.nextel.R
import pynith.apps.nextel.model.CONData

object AppConfig {

    var notificationSound = true
    var notificationVibration = false
    var fontScale = 1.0f
    var themeName = "Light"
    var fontFamily = "Patrick"
    var dailyReminder = false

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(CONData.APP_PREFS_EXT, Context.MODE_PRIVATE)

        dailyReminder = prefs.getBoolean("daily_reminder", dailyReminder)
        notificationSound = prefs.getBoolean("sound", notificationSound)
        notificationVibration = prefs.getBoolean("vibration", notificationVibration)
        fontScale = prefs.getFloat("fontScale", fontScale)
        themeName = prefs.getString("theme", themeName)!!
        fontFamily = prefs.getString("fontFamily", fontFamily)!!
    }

    fun setNotificationPrefs(context: Context, sound: Boolean? = null, vibration: Boolean? = null) {
        val prefs = context.getSharedPreferences(CONData.APP_PREFS_EXT, Context.MODE_PRIVATE)

        sound?.let {
            notificationSound = it
            prefs.edit { putBoolean("sound", it) }
        }

        vibration?.let {
            notificationVibration = it
            prefs.edit { putBoolean("vibration", it) }
        }
    }

    fun setDailyReminder(context: Context, value: Boolean) {

        val prefs = context.getSharedPreferences(CONData.APP_PREFS_EXT, Context.MODE_PRIVATE)
        dailyReminder = value;

        prefs.edit { putBoolean("daily_reminder", value) }

        if (value) {
            NotificationService.scheduleDailyReminders(context)
        } else {
            NotificationService.cancelAll(context)
        }
    }

    fun isDailyReminderEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(CONData.APP_PREFS_EXT, Context.MODE_PRIVATE)
        return prefs.getBoolean("daily_reminder", false)
    }

    fun setFontSize(context: Context, size: Float) {
        fontScale = size / 16f
        context.getSharedPreferences(CONData.APP_PREFS_EXT, Context.MODE_PRIVATE)
            .edit { putFloat("fontScale", fontScale) }
    }

    fun setTheme(context: Context, theme: String) {
        themeName = theme
        context.getSharedPreferences(CONData.APP_PREFS_EXT, Context.MODE_PRIVATE)
            .edit { putString("theme", theme) }
    }

    fun setFontFamily(context: Context, font: String) {
        fontFamily = font
        context.getSharedPreferences(CONData.APP_PREFS_EXT, Context.MODE_PRIVATE)
            .edit { putString("fontFamily", font) }
    }

    fun getFontRes(): Int {
        return when (fontFamily) {
            "Poppins" -> R.font.poppins
            "Roboto" -> R.font.roboto
            "Nunito" -> R.font.nunito
            "Patrick" -> R.font.patrick
            "Montserrat" -> R.font.montserrat
            else -> R.font.poppins
        }
    }

}