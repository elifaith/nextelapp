package pynith.apps.nextel.views.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import org.json.JSONObject
import pynith.apps.nextel.R
import pynith.apps.nextel.helper.ApiResult
import pynith.apps.nextel.helper.LogoutCoordinator
import pynith.apps.nextel.helper.NextelApi
import pynith.apps.nextel.helper.SessionService
import pynith.apps.nextel.helper.WebSessionHandoff
import pynith.apps.nextel.views.BaseActivity

class EmailVerificationActivity : BaseActivity() {
    private val api by lazy { NextelApi(this) }
    private lateinit var session: SessionService
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)
        session = SessionService(this)
        email = intent.getStringExtra(EXTRA_EMAIL).orEmpty()

        findViewById<android.widget.TextView>(R.id.verificationMessage).text = if (email.isBlank()) {
            "Enter the 6-digit code sent to your email address."
        } else {
            "Enter the 6-digit code sent to $email."
        }

        findViewById<View>(R.id.verifyButton).setOnClickListener { verifyCode() }
        findViewById<View>(R.id.resendButton).setOnClickListener { resendCode() }
        findViewById<View>(R.id.useAnotherAccountButton).setOnClickListener {
            LogoutCoordinator.requestLogout(this)
        }
    }

    private fun verifyCode() {
        val codeInput = findViewById<android.widget.EditText>(R.id.verificationCode)
        codeInput.error = null
        val code = codeInput.text?.toString()?.trim().orEmpty()
        if (!Regex("^\\d{6}$").matches(code)) {
            codeInput.error = "Enter the 6-digit code"
            return
        }

        val token = session.getToken()
        if (token.isNullOrBlank()) {
            returnToLogin()
            return
        }

        setLoading(true)
        api.post("email/verify", JSONObject().put("code", code), token) { result ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                setLoading(false)
                when (result) {
                    is ApiResult.Success -> continueToLivewire()
                    is ApiResult.Failure -> handleFailure(result.error.displayMessage(), result.error.statusCode)
                }
            }
        }
    }

    private fun resendCode() {
        val token = session.getToken()
        if (token.isNullOrBlank()) {
            returnToLogin()
            return
        }

        setLoading(true)
        api.post("email/resend", JSONObject(), token) { result ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                setLoading(false)
                when (result) {
                    is ApiResult.Success -> toast(result.message)
                    is ApiResult.Failure -> handleFailure(result.error.displayMessage(), result.error.statusCode)
                }
            }
        }
    }

    private fun continueToLivewire() {
        setLoading(true)
        WebSessionHandoff.openLivewire(this) { message ->
            if (!isFinishing && !isDestroyed) {
                setLoading(false)
                toast(message)
            }
        }
    }

    private fun handleFailure(message: String, statusCode: Int) {
        if (statusCode == 401) {
            session.clearSession()
            returnToLogin()
        } else {
            toast(message)
        }
    }

    private fun setLoading(loading: Boolean) {
        findViewById<View>(R.id.verificationProgress).visibility = if (loading) View.VISIBLE else View.GONE
        findViewById<View>(R.id.verifyButton).isEnabled = !loading
        findViewById<View>(R.id.resendButton).isEnabled = !loading
    }

    private fun returnToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    @Deprecated("Use the native logout flow rather than returning to login with an unverified token.")
    override fun onBackPressed() {
        LogoutCoordinator.requestLogout(this)
    }

    companion object {
        const val EXTRA_EMAIL = "pynith.apps.nextel.auth.VERIFICATION_EMAIL"
    }
}
