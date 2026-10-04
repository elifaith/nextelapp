package pynith.apps.nextel.views.us

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import pynith.apps.nextel.R
import pynith.apps.nextel.helper.ApiError
import pynith.apps.nextel.helper.ApiResult
import pynith.apps.nextel.helper.NextelApi
import pynith.apps.nextel.helper.SessionService
import pynith.apps.nextel.views.BaseActivity
import pynith.apps.nextel.views.auth.LoginActivity

/** Native Android ticket list, creation form, and conversation view. */
class SupportActivity : BaseActivity() {
    private val api by lazy { NextelApi(this) }
    private lateinit var accessToken: String
    private lateinit var toolbar: Toolbar
    private lateinit var homeSection: View
    private lateinit var conversationSection: View
    private lateinit var accountName: TextView
    private lateinit var accountEmail: TextView
    private lateinit var accountInitials: TextView
    private lateinit var ticketsContainer: LinearLayout
    private lateinit var messagesContainer: LinearLayout
    private lateinit var subjectInput: TextInputEditText
    private lateinit var messageInput: TextInputEditText
    private lateinit var replyInput: TextInputEditText
    private lateinit var categorySpinner: android.widget.Spinner
    private lateinit var formError: TextView
    private lateinit var replyError: TextView
    private lateinit var emptyState: TextView
    private lateinit var progress: View
    private lateinit var openButton: View
    private lateinit var replyButton: View
    private lateinit var conversationReference: TextView
    private lateinit var conversationTitle: TextView
    private lateinit var conversationStatus: TextView
    private var selectedTicketId: String? = null
    private var retriedWithCurrentSessionToken = false

    private val categoryKeys = listOf("general", "account", "payments", "technical")
    private val categoryLabels = listOf("General enquiry", "Account access", "Payments", "Technical issue")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_support)

        accessToken = (intent.getStringExtra(EXTRA_ACCESS_TOKEN)
            ?: intent.getStringExtra(EXTRA_SUPPORT_TOKEN))
            ?.takeIf(String::isNotBlank)
            ?: SessionService(this).getToken().orEmpty()

        if (accessToken.isBlank()) {
            Toast.makeText(this, "Sign in to open or view support tickets.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            finish()
            return
        }

        bindViews()
        setupCategories()
        toolbar.setNavigationIcon(R.drawable.ic_support_back)
        toolbar.setNavigationOnClickListener { navigateBack() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = navigateBack()
        })
        findViewById<View>(R.id.openSupportTicketButton).setOnClickListener { openTicket() }
        findViewById<View>(R.id.sendSupportReplyButton).setOnClickListener { sendReply() }
        findViewById<View>(R.id.supportBackToTicketsButton).setOnClickListener { showTicketList() }

        loadTickets()
    }

    private fun bindViews() {
        toolbar = findViewById(R.id.supportToolbar)
        homeSection = findViewById(R.id.supportHomeSection)
        conversationSection = findViewById(R.id.supportConversationSection)
        accountName = findViewById(R.id.supportUserName)
        accountEmail = findViewById(R.id.supportUserEmail)
        accountInitials = findViewById(R.id.supportUserInitials)
        ticketsContainer = findViewById(R.id.supportTicketsContainer)
        messagesContainer = findViewById(R.id.supportMessagesContainer)
        subjectInput = findViewById(R.id.supportSubject)
        messageInput = findViewById(R.id.supportMessage)
        replyInput = findViewById(R.id.supportReply)
        categorySpinner = findViewById(R.id.supportCategory)
        formError = findViewById(R.id.supportFormError)
        replyError = findViewById(R.id.supportReplyError)
        emptyState = findViewById(R.id.supportEmptyState)
        progress = findViewById(R.id.supportProgress)
        openButton = findViewById(R.id.openSupportTicketButton)
        replyButton = findViewById(R.id.sendSupportReplyButton)
        conversationReference = findViewById(R.id.supportConversationReference)
        conversationTitle = findViewById(R.id.supportConversationTitle)
        conversationStatus = findViewById(R.id.supportConversationStatus)
    }

    private fun setupCategories() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryLabels).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        categorySpinner.adapter = adapter
    }

    private fun loadTickets() {
        progress.visibility = View.VISIBLE
        api.get("support-tickets", accessToken) { result ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                progress.visibility = View.GONE
                when (result) {
                    is ApiResult.Success -> {
                        val profile = result.data.optJSONObject("user") ?: JSONObject()
                        showAccount(profile.optString("name"), profile.optString("email"))
                        renderTickets(result.data.optJSONArray("tickets") ?: JSONArray())
                    }
                    is ApiResult.Failure -> handleFailure(result.error, formError) { loadTickets() }
                }
            }
        }
    }

    private fun showAccount(name: String, email: String) {
        accountName.text = name.ifBlank { "Registered Nextel user" }
        accountEmail.text = email
        accountInitials.text = name.trim()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { "N" }
    }

    private fun renderTickets(tickets: JSONArray) {
        ticketsContainer.removeAllViews()
        emptyState.visibility = if (tickets.length() == 0) View.VISIBLE else View.GONE
        for (index in 0 until tickets.length()) {
            val ticket = tickets.optJSONObject(index) ?: continue
            ticketsContainer.addView(ticketCard(ticket))
        }
    }

    private fun ticketCard(ticket: JSONObject): View {
        val card = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(1).toFloat()
            setCardBackgroundColor(color(R.color.brand_surface))
            strokeWidth = dp(1)
            strokeColor = color(R.color.brand_border)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(10) }
            isClickable = true
            isFocusable = true
            setOnClickListener { loadConversation(ticket.optString("id")) }
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
        }
        val titleAndStatus = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = ticket.optString("subject")
            setTextColor(color(R.color.brand_text))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val status = TextView(this).apply {
            text = ticket.optString("status_label", ticket.optString("status"))
            setTextColor(color(R.color.brand_primary))
            textSize = 10f
            setPadding(dp(9), dp(5), dp(9), dp(5))
            background = ContextCompat.getDrawable(this@SupportActivity, R.drawable.bg_support_status)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        titleAndStatus.addView(title)
        titleAndStatus.addView(status)
        content.addView(titleAndStatus)

        val reference = TextView(this).apply {
            text = "${ticket.optString("reference")} · ${ticket.optString("category").replaceFirstChar { it.uppercase() }}"
            setTextColor(color(R.color.brand_muted))
            textSize = 11f
            setPadding(0, dp(5), 0, 0)
        }
        content.addView(reference)

        val latestMessage = ticket.optJSONObject("latest_message")?.optString("body").orEmpty()
        if (latestMessage.isNotBlank()) {
            content.addView(TextView(this).apply {
                text = latestMessage
                setTextColor(color(R.color.brand_text))
                textSize = 12f
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(9), 0, 0)
            })
        }

        card.addView(content)
        return card
    }

    private fun openTicket() {
        clearError(formError)
        val subject = subjectInput.text?.toString()?.trim().orEmpty()
        val body = messageInput.text?.toString()?.trim().orEmpty()
        if (subject.length < 4) {
            showError(formError, "Please enter a subject of at least 4 characters.")
            subjectInput.requestFocus()
            return
        }
        if (body.length < 5) {
            showError(formError, "Please describe your issue in at least 5 characters.")
            messageInput.requestFocus()
            return
        }

        setLoading(openButton, true)
        val selectedCategory = categoryKeys.getOrElse(categorySpinner.selectedItemPosition) { "general" }
        val payload = JSONObject()
            .put("subject", subject)
            .put("category", selectedCategory)
            .put("message", body)

        api.post("support-tickets", payload, accessToken) { result ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                setLoading(openButton, false)
                when (result) {
                    is ApiResult.Success -> {
                        val profile = result.data.optJSONObject("user") ?: JSONObject()
                        if (profile.has("name") || profile.has("email")) {
                            showAccount(profile.optString("name"), profile.optString("email"))
                        }
                        subjectInput.setText("")
                        messageInput.setText("")
                        categorySpinner.setSelection(0)
                        val ticketId = result.data.optJSONObject("ticket")?.optString("id").orEmpty()
                        if (ticketId.isBlank()) {
                            showError(formError, "Your ticket was created, but we couldn't open its conversation.")
                            loadTickets()
                        } else {
                            Toast.makeText(this, "Support ticket opened.", Toast.LENGTH_SHORT).show()
                            loadConversation(ticketId)
                        }
                    }
                    is ApiResult.Failure -> handleFailure(result.error, formError) { openTicket() }
                }
            }
        }
    }

    private fun loadConversation(ticketId: String) {
        if (ticketId.isBlank()) return
        progress.visibility = View.VISIBLE
        api.get("support-tickets/$ticketId", accessToken) { result ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                progress.visibility = View.GONE
                when (result) {
                    is ApiResult.Success -> {
                        val ticket = result.data.optJSONObject("ticket") ?: JSONObject()
                        selectedTicketId = ticket.optString("id", ticketId)
                        conversationReference.text = ticket.optString("reference")
                        conversationTitle.text = ticket.optString("subject")
                        conversationStatus.text = ticket.optString("status_label", ticket.optString("status"))
                        renderMessages(result.data.optJSONArray("messages") ?: JSONArray())
                        showConversation()
                    }
                    is ApiResult.Failure -> Toast.makeText(this, result.error.displayMessage(), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun renderMessages(messages: JSONArray) {
        messagesContainer.removeAllViews()
        for (index in 0 until messages.length()) {
            val message = messages.optJSONObject(index) ?: continue
            val fromSupport = message.optString("sender_type") == "admin"
            val wrapper = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = if (fromSupport) Gravity.START else Gravity.END
                setPadding(0, dp(3), 0, dp(8))
            }
            val bubble = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(13), dp(10), dp(13), dp(10))
                background = ContextCompat.getDrawable(
                    this@SupportActivity,
                    if (fromSupport) R.drawable.bg_support_card else R.drawable.bg_support_status,
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }
            bubble.addView(TextView(this).apply {
                text = if (fromSupport) message.optString("sender_name").ifBlank { "Nextel Support" } else "You"
                setTextColor(color(R.color.brand_primary))
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
            })
            bubble.addView(TextView(this).apply {
                text = message.optString("body")
                setTextColor(color(R.color.brand_text))
                textSize = 14f
                setPadding(0, dp(5), 0, 0)
            })
            bubble.addView(TextView(this).apply {
                text = message.optString("created_at").replace('T', ' ').take(16)
                setTextColor(color(R.color.brand_muted))
                textSize = 10f
                setPadding(0, dp(5), 0, 0)
            })
            wrapper.addView(bubble, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginStart = if (fromSupport) 0 else dp(36)
                marginEnd = if (fromSupport) dp(36) else 0
            })
            messagesContainer.addView(wrapper)
        }
    }

    private fun sendReply() {
        clearError(replyError)
        val ticketId = selectedTicketId.orEmpty()
        val body = replyInput.text?.toString()?.trim().orEmpty()
        if (ticketId.isBlank()) return
        if (body.isBlank()) {
            showError(replyError, "Write a reply before sending.")
            return
        }

        setLoading(replyButton, true)
        api.post("support-tickets/$ticketId/messages", JSONObject().put("message", body), accessToken) { result ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                setLoading(replyButton, false)
                when (result) {
                    is ApiResult.Success -> {
                        replyInput.setText("")
                        loadConversation(ticketId)
                    }
                    is ApiResult.Failure -> handleFailure(result.error, replyError) { sendReply() }
                }
            }
        }
    }

    private fun showTicketList() {
        selectedTicketId = null
        conversationSection.visibility = View.GONE
        homeSection.visibility = View.VISIBLE
        toolbar.title = "Help & Support"
        loadTickets()
    }

    private fun showConversation() {
        homeSection.visibility = View.GONE
        conversationSection.visibility = View.VISIBLE
        toolbar.title = "Support ticket"
    }

    private fun navigateBack() {
        if (conversationSection.visibility == View.VISIBLE) showTicketList() else finish()
    }

    private fun handleFailure(error: ApiError, target: TextView, retry: () -> Unit) {
        Log.d("WEBVIEWELIAS", "Result: $error")
        if (error.statusCode == 401 && !retriedWithCurrentSessionToken) {
            val currentSessionToken = SessionService(this).getToken()
            if (!currentSessionToken.isNullOrBlank() && currentSessionToken != accessToken) {
                retriedWithCurrentSessionToken = true
                accessToken = currentSessionToken
                clearError(target)
                retry()
                return
            }
        }

        val fieldMessage = when (target.id) {
            R.id.supportFormError -> error.firstFieldError("subject")
                ?: error.firstFieldError("category")
                ?: error.firstFieldError("message")
            R.id.supportReplyError -> error.firstFieldError("message")
            else -> null
        }
        showError(target, fieldMessage ?: error.displayMessage())
        if (error.statusCode == 401) {
            Toast.makeText(this, "Your secure support session expired. Please sign in again.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            finish()
        }
    }

    private fun showError(view: TextView, message: String) {
        view.text = message
        view.visibility = View.VISIBLE
    }

    private fun clearError(view: TextView) {
        view.text = ""
        view.visibility = View.GONE
    }

    private fun setLoading(button: View, loading: Boolean) {
        button.isEnabled = !loading
        button.alpha = if (loading) 0.65f else 1f
        progress.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun color(resource: Int): Int = ContextCompat.getColor(this, resource)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_ACCESS_TOKEN = "pynith.apps.nextel.support.ACCESS_TOKEN"
        const val EXTRA_SUPPORT_TOKEN = "pynith.apps.nextel.support.SUPPORT_TOKEN" // Legacy extra key.

        fun createIntent(context: Context, accessToken: String?): Intent =
            Intent(context, SupportActivity::class.java).apply {
                accessToken?.takeIf(String::isNotBlank)?.let {
                    putExtra(EXTRA_ACCESS_TOKEN, it)
                }
            }
    }
}
