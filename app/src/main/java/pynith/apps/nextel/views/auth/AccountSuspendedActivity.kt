package pynith.apps.nextel.views.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import pynith.apps.nextel.R
import pynith.apps.nextel.views.BaseActivity
import pynith.apps.nextel.views.us.SupportActivity

class AccountSuspendedActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_suspended)

        val message = intent.getStringExtra(EXTRA_MESSAGE)
            ?.takeIf(String::isNotBlank)
            ?: "This account has been suspended. Please contact support."
        findViewById<TextView>(R.id.suspendedMessage).text = message

        findViewById<View>(R.id.returnToLoginButton).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            finish()
        }
        findViewById<View>(R.id.contactSupportButton).setOnClickListener {
            startActivity(
                SupportActivity.createIntent(this, intent.getStringExtra(EXTRA_SUPPORT_TOKEN))
            )
        }
    }

    companion object {
        const val EXTRA_MESSAGE = "pynith.apps.nextel.auth.SUSPENDED_MESSAGE"
        const val EXTRA_SUPPORT_TOKEN = "pynith.apps.nextel.auth.SUPPORT_TOKEN"
    }
}
