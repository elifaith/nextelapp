package pynith.apps.nextel.views.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import org.json.JSONObject
import pynith.apps.nextel.R
import pynith.apps.nextel.databinding.ActivityResetBinding
import pynith.apps.nextel.helper.ApiResult
import pynith.apps.nextel.helper.NextelApi
import pynith.apps.nextel.helper.SessionService
import pynith.apps.nextel.views.BaseActivity

/** Native password reset using the API's email + six-digit code contract. */
class ResetActivity : BaseActivity() {
    private lateinit var bind: ActivityResetBinding
    private val api by lazy { NextelApi(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityResetBinding.inflate(layoutInflater)
        setContentView(bind.root)

        bind.backButton.setOnClickListener { finish() }
        bind.tvLogUser.setOnClickListener { finish() }
        bind.tvCreate.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
        bind.welcomeSubtitle.text = "Request a 6-digit reset code using your email address."
        bind.username.hint = "Email address"
        bind.username.inputType = android.text.InputType.TYPE_CLASS_TEXT or
            android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        bind.otp.hint = "6-digit reset code"
        bind.password.hint = "New password"

        bind.sendOtpBtn.setOnClickListener { requestResetCode() }
        bind.continueBtn.setOnClickListener { resetPassword() }
    }

    private fun requestResetCode() {
        val email = bind.username.text?.toString()?.trim().orEmpty()
        bind.username.error = null
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            bind.username.error = "Enter a valid email address"
            return
        }

        setCodeRequestLoading(true)
        api.post("password/forgot", JSONObject().put("email", email)) { result ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                setCodeRequestLoading(false)
                when (result) {
                    is ApiResult.Success -> toast(result.message)
                    is ApiResult.Failure -> toast(result.error.displayMessage())
                }
            }
        }
    }

    private fun resetPassword() {
        val email = bind.username.text?.toString()?.trim().orEmpty()
        val code = bind.otp.text?.toString()?.trim().orEmpty()
        val password = bind.password.text?.toString().orEmpty()
        bind.username.error = null
        bind.otp.error = null
        bind.passwordLayout.error = null

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            bind.username.error = "Enter a valid email address"
            return
        }
        if (!Regex("^\\d{6}$").matches(code)) {
            bind.otp.error = "Enter the 6-digit reset code"
            return
        }
        if (password.length < 8) {
            bind.passwordLayout.error = "Password must contain at least 8 characters"
            return
        }

        setPasswordResetLoading(true)
        val payload = JSONObject()
            .put("email", email)
            .put("code", code)
            .put("password", password)

        api.post("password/reset", payload) { result ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                setPasswordResetLoading(false)
                when (result) {
                    is ApiResult.Success -> {
                        toast(result.message)
                        // The reset endpoint revokes existing tokens; don't let LoginActivity
                        // silently resume a cached session instead of showing the sign-in form.
                        SessionService(this).clearSession()
                        startActivity(Intent(this, LoginActivity::class.java).apply {
                            putExtra(LoginActivity.EXTRA_PREFILL_LOGIN, email)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        })
                        finish()
                    }

                    is ApiResult.Failure -> {
                        result.error.firstFieldError("email")?.let { bind.username.error = it }
                        result.error.firstFieldError("code")?.let { bind.otp.error = it }
                        result.error.firstFieldError("password")?.let { bind.passwordLayout.error = it }
                        toast(result.error.displayMessage())
                    }
                }
            }
        }
    }

    private fun setCodeRequestLoading(loading: Boolean) {
        bind.otpProgress.visibility = if (loading) View.VISIBLE else View.GONE
        bind.sendOtpBtn.isEnabled = !loading
        bind.continueBtn.isEnabled = !loading
    }

    private fun setPasswordResetLoading(loading: Boolean) {
        bind.progress.visibility = if (loading) View.VISIBLE else View.GONE
        bind.continueBtn.isEnabled = !loading
        bind.sendOtpBtn.isEnabled = !loading
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
