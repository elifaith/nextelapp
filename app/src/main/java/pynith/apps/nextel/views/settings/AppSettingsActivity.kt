package pynith.apps.nextel.views.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor
import pynith.apps.nextel.R
import pynith.apps.nextel.views.us.SupportActivity
import pynith.apps.nextel.helper.AppConfig
import androidx.core.content.edit
import pynith.apps.nextel.App
import pynith.apps.nextel.model.CONData
import pynith.apps.nextel.views.BaseActivity
import pynith.apps.nextel.helper.SessionService

class AppSettingsActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var biometricManager: BiometricManager
    private lateinit var biometricSwitch: Switch
    private lateinit var executor: Executor
    private lateinit var session: SessionService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_app_settings)

        prefs = getSharedPreferences(CONData.APP_PREFS_EXT, MODE_PRIVATE)
        session = SessionService(this)
        biometricManager = BiometricManager.from(this)
        executor = ContextCompat.getMainExecutor(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        // Set Toolbar as ActionBar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        setupUI()
    }

    private fun setupUI() {

        biometricSwitch = findViewById(R.id.switchBiometric)
        val notificationSwitch = findViewById<Switch>(R.id.switchNotification)
        val soundSwitch = findViewById<Switch>(R.id.switchSound)
        val vibrationSwitch = findViewById<Switch>(R.id.switchVibration)
        val reminderSwitch = findViewById<Switch>(R.id.switchReminder)

        val fontSlider = findViewById<SeekBar>(R.id.seekFont)
        val txtFontSize = findViewById<TextView>(R.id.txtFontSize)

        val spinnerTheme = findViewById<Spinner>(R.id.spinnerTheme)
        val spinnerFont = findViewById<Spinner>(R.id.spinnerFont)

        /// BIOMETRIC
        val activeToken = session.getToken()
        val hasSessionToken = !activeToken.isNullOrBlank()
        var biometricEnabled = prefs.getBoolean(CONData.BIOMETRIC_ENABLED, false) && hasSessionToken
        if (!hasSessionToken) {
            prefs.edit().putBoolean(CONData.BIOMETRIC_ENABLED, false).apply()
        } else if (biometricEnabled && !session.shouldRememberSession()) {
            try {
                // Migrate an already-enabled setting to a persistent encrypted session.
                session.saveSession(activeToken!!)
            } catch (_: Exception) {
                prefs.edit().putBoolean(CONData.BIOMETRIC_ENABLED, false).apply()
                biometricEnabled = false
            }
        }
        biometricSwitch.isChecked = biometricEnabled
        biometricSwitch.setOnCheckedChangeListener { _, isChecked ->
            toggleBiometric(isChecked)
        }

        /// NOTIFICATION
        notificationSwitch.isChecked = prefs.getBoolean("notifications_enabled", true)
        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean("notifications_enabled", isChecked) }
        }

        /// REMINDER
        //reminderSwitch.isChecked = AppConfig.dailyReminder
        reminderSwitch.isChecked = AppConfig.isDailyReminderEnabled(this)
        reminderSwitch.setOnCheckedChangeListener { _, v ->
            AppConfig.setDailyReminder(this, value = v)
        }

        /// SOUND + VIBRATION
        soundSwitch.isChecked = AppConfig.notificationSound
        vibrationSwitch.isChecked = AppConfig.notificationVibration

        soundSwitch.setOnCheckedChangeListener { _, v ->
            AppConfig.setNotificationPrefs(this, sound = v)
        }

        vibrationSwitch.setOnCheckedChangeListener { _, v ->
            AppConfig.setNotificationPrefs(this, vibration = v)
        }

        /// FONT SIZE
        val currentSize = (AppConfig.fontScale * 19).toInt()
        txtFontSize.text = "Current: $currentSize"

        fontSlider.max = 40
        fontSlider.progress = currentSize

        fontSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                txtFontSize.text = "Current: $value"
                AppConfig.setFontSize(this@AppSettingsActivity, value.toFloat())
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                recreate()
            }
        })

        /// THEME
        val themes = arrayOf("Light", "Dark", "Blue")
        spinnerTheme.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themes)

        spinnerTheme.setSelection(themes.indexOf(AppConfig.themeName))


        spinnerTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                AppConfig.setTheme(this@AppSettingsActivity, themes[pos])
                //recreate()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        /// FONT FAMILY
        val fonts = arrayOf("Poppins","Roboto","Nunito","Patrick","Montserrat")

        spinnerFont.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fonts)
        spinnerFont.setSelection(fonts.indexOf(AppConfig.fontFamily))

        spinnerFont.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>, v: View?, pos: Int, id: Long) {
                AppConfig.setFontFamily(this@AppSettingsActivity, fonts[pos])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        /// DELETE
        findViewById<View>(R.id.btnDelete).setOnClickListener {
            confirmDelete()
        }

        /// LOGOUT
        findViewById<View>(R.id.btnLogout).setOnClickListener {
            App.logout(this)
        }

        /// SUPPORT
        findViewById<View>(R.id.btnSupport).setOnClickListener {
            startActivity(SupportActivity.createIntent(this, session.getToken()))
        }
    }

    private fun toggleBiometric(enable: Boolean) {
        if (!enable) {
            prefs.edit().putBoolean(CONData.BIOMETRIC_ENABLED, false).apply()
            return
        }

        val token = session.getToken()
        if (token.isNullOrBlank()) {
            prefs.edit().putBoolean(CONData.BIOMETRIC_ENABLED, false).apply()
            biometricSwitch.isChecked = false
            toast("Sign in again before enabling biometric login")
            return
        }

        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            != BiometricManager.BIOMETRIC_SUCCESS
        ) {
            biometricSwitch.isChecked = false
            toast("Strong biometrics are not available on this device")
            return
        }

        val prompt = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable biometric login")
            .setNegativeButtonText("Cancel")
            .build()

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    try {
                        // Biometric login must survive an app/process restart, even if the
                        // user originally signed in with Remember Me turned off.
                        session.saveSession(token)
                        prefs.edit().putBoolean(CONData.BIOMETRIC_ENABLED, true).apply()
                        biometricSwitch.isChecked = true
                        toast("Biometric enabled")
                    } catch (_: Exception) {
                        prefs.edit().putBoolean(CONData.BIOMETRIC_ENABLED, false).apply()
                        biometricSwitch.isChecked = false
                        toast("Could not securely save biometric login. Please try again.")
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    biometricSwitch.isChecked = false
                }
            })

        biometricPrompt.authenticate(prompt)
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Account deletion")
            .setMessage("Account deletion is not available from Android Settings. Please contact Nextel support for help.")
            .setPositiveButton("Contact support") { _, _ ->
                startActivity(SupportActivity.createIntent(this, session.getToken()))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}