package pynith.apps.nextel.views.coupon

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import pynith.apps.nextel.R
import pynith.apps.nextel.helper.ApiResult
import pynith.apps.nextel.helper.NextelApi
import pynith.apps.nextel.model.AgentData
import pynith.apps.nextel.model.BatchData
import pynith.apps.nextel.model.CouponData
import pynith.apps.nextel.views.BaseActivity
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Native coupon lookup: form first, then validity and details below. */
class CouponSearchActivity : BaseActivity() {
    private val api by lazy { NextelApi(this) }

    private lateinit var codeLayout: TextInputLayout
    private lateinit var codeInput: TextInputEditText
    private lateinit var verifyButton: MaterialButton
    private lateinit var errorCard: View
    private lateinit var errorMessage: TextView
    private lateinit var results: View
    private lateinit var validityCard: View
    private lateinit var validityIcon: TextView
    private lateinit var validityTitle: TextView
    private lateinit var validityMessage: TextView
    private lateinit var validityCode: TextView
    private lateinit var basicCard: LinearLayout
    private lateinit var productCard: View
    private lateinit var productTitle: TextView
    private lateinit var productRows: LinearLayout
    private lateinit var batchCard: View
    private lateinit var batchRows: LinearLayout
    private lateinit var agentCard: View
    private lateinit var agentRows: LinearLayout
    private lateinit var redeemerCard: View
    private lateinit var redeemerRows: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coupon_search)

        findViewById<MaterialToolbar>(R.id.coupon_toolbar).setNavigationOnClickListener { finish() }

        codeLayout = findViewById(R.id.coupon_code_layout)
        codeInput = findViewById(R.id.coupon_code)
        verifyButton = findViewById(R.id.coupon_verify_button)
        errorCard = findViewById(R.id.coupon_error_card)
        errorMessage = findViewById(R.id.coupon_error_message)
        results = findViewById(R.id.coupon_results)
        validityCard = findViewById(R.id.coupon_validity_card)
        validityIcon = findViewById(R.id.coupon_validity_icon)
        validityTitle = findViewById(R.id.coupon_validity_title)
        validityMessage = findViewById(R.id.coupon_validity_message)
        validityCode = findViewById(R.id.coupon_validity_code)
        basicCard = findViewById(R.id.coupon_basic_card)
        productCard = findViewById(R.id.coupon_product_card)
        productTitle = findViewById(R.id.coupon_product_title)
        productRows = findViewById(R.id.coupon_product_rows)
        batchCard = findViewById(R.id.coupon_batch_card)
        batchRows = findViewById(R.id.coupon_batch_rows)
        agentCard = findViewById(R.id.coupon_agent_card)
        agentRows = findViewById(R.id.coupon_agent_rows)
        redeemerCard = findViewById(R.id.coupon_redeemer_card)
        redeemerRows = findViewById(R.id.coupon_redeemer_rows)

        codeInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                verifyCoupon()
                true
            } else {
                false
            }
        }
        verifyButton.setOnClickListener { verifyCoupon() }
    }

    private fun verifyCoupon() {
        codeLayout.error = null
        val code = codeInput.text?.toString()?.trim().orEmpty()
        if (code.isEmpty()) {
            codeLayout.error = getString(R.string.coupon_code_required)
            return
        }
        if (code.length < 3) {
            codeLayout.error = getString(R.string.coupon_code_too_short)
            return
        }

        setLoading(true)
        errorCard.visibility = View.GONE
        results.visibility = View.GONE

        val encoded = URLEncoder.encode(code, StandardCharsets.UTF_8.toString())
        api.get("coupons/verify?code=$encoded") { result ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                setLoading(false)

                when (result) {
                    is ApiResult.Success -> {
                        bindCoupon(CouponData.fromJson(result.data))
                    }
                    is ApiResult.Failure -> {
                        errorMessage.text = result.error.firstFieldError("code")
                            ?: result.error.displayMessage().ifBlank {
                                getString(R.string.coupon_not_found_fallback)
                            }
                        errorCard.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun bindCoupon(coupon: CouponData) {
        val valid = coupon.isValid
        val accent = ContextCompat.getColor(
            this,
            if (valid) R.color.brand_primary else R.color.brand_danger
        )

        validityCard.setBackgroundResource(
            if (valid) R.drawable.coupon_valid_card_bg else R.drawable.coupon_used_card_bg
        )
        validityIcon.text = if (valid) "✓" else "✕"
        validityIcon.setTextColor(accent)
        validityTitle.text = getString(
            if (valid) R.string.coupon_valid_title else R.string.coupon_used_title
        )
        validityTitle.setTextColor(accent)
        validityMessage.text = coupon.verification?.message
            ?: getString(if (valid) R.string.coupon_valid_fallback else R.string.coupon_used_fallback)
        validityCode.text = coupon.code.orEmpty()

        basicCard.removeAllViews()
        addRow(basicCard, getString(R.string.coupon_label_code), display(coupon.code), valueBold = true)
        addRow(basicCard, getString(R.string.coupon_label_type), display(coupon.typeName ?: coupon.type))
        addRow(
            basicCard,
            getString(R.string.coupon_label_status),
            display(coupon.status),
            valueColor = accent
        )
        addRow(basicCard, getString(R.string.coupon_label_price), display(coupon.price))
        addRow(basicCard, getString(R.string.coupon_label_region), formatRegion(coupon.region))
        addRow(
            basicCard,
            getString(R.string.coupon_label_special),
            getString(if (coupon.isSpecial) R.string.coupon_yes else R.string.coupon_no)
        )
        if (!coupon.usedAt.isNullOrBlank()) {
            addRow(basicCard, getString(R.string.coupon_label_used_at), formatDate(coupon.usedAt))
        }
        if (!coupon.displayName.isNullOrBlank()) {
            addRow(basicCard, getString(R.string.coupon_label_description), coupon.displayName)
        }

        bindProduct(coupon)
        bindBatch(coupon.batch)
        bindPerson(agentCard, agentRows, coupon.agent)
        bindPerson(redeemerCard, redeemerRows, coupon.redeemer)

        errorCard.visibility = View.GONE
        results.visibility = View.VISIBLE
    }

    private fun bindProduct(coupon: CouponData) {
        val product = coupon.product
        if (product == null) {
            productCard.visibility = View.GONE
            return
        }

        productTitle.text = when {
            coupon.packageData != null -> getString(R.string.coupon_product_package)
            coupon.additionalMinutePlan != null -> getString(R.string.coupon_product_minutes)
            coupon.cloudStoragePlan != null -> getString(R.string.coupon_product_cloud)
            else -> getString(R.string.coupon_product_generic)
        }

        productRows.removeAllViews()
        addRow(productRows, getString(R.string.coupon_label_name), display(product.name), valueBold = true)
        addProductRow(productRows, getString(R.string.coupon_label_price), product.price)
        addProductRow(productRows, getString(R.string.coupon_label_description), product.description)
        addProductRow(productRows, getString(R.string.coupon_label_duration), product.duration)
        addProductRow(productRows, getString(R.string.coupon_label_minutes), product.minutes)
        addProductRow(productRows, getString(R.string.coupon_label_storage), product.storage)
        addProductRow(productRows, getString(R.string.coupon_label_storage_size), product.storageSize)
        productCard.visibility = View.VISIBLE
    }

    private fun bindBatch(batch: BatchData?) {
        if (batch == null) {
            batchCard.visibility = View.GONE
            return
        }

        batchRows.removeAllViews()
        addRow(batchRows, getString(R.string.coupon_label_reference), display(batch.reference), valueBold = true)
        addRow(batchRows, getString(R.string.coupon_label_name), display(batch.displayName))
        addRow(batchRows, getString(R.string.coupon_label_type), display(batch.typeName ?: batch.type))
        addRow(batchRows, getString(R.string.coupon_label_region), formatRegion(batch.region))
        if (!batch.createdAt.isNullOrBlank()) {
            addRow(batchRows, getString(R.string.coupon_label_created), formatDate(batch.createdAt))
        }
        batchCard.visibility = View.VISIBLE
    }

    private fun bindPerson(card: View, rows: LinearLayout, person: AgentData?) {
        if (person == null || (person.name.isNullOrBlank() && person.username.isNullOrBlank())) {
            card.visibility = View.GONE
            return
        }

        rows.removeAllViews()
        if (!person.name.isNullOrBlank()) {
            addRow(rows, getString(R.string.coupon_label_name), person.name)
        }
        if (!person.username.isNullOrBlank()) {
            addRow(rows, getString(R.string.coupon_label_username), getString(R.string.coupon_username_format, person.username))
        }
        card.visibility = View.VISIBLE
    }

    private fun addProductRow(container: LinearLayout, label: String, value: String?) {
        if (value.isNullOrBlank()) return
        addRow(container, label, value)
    }

    private fun addRow(
        container: LinearLayout,
        label: String,
        value: String,
        valueBold: Boolean = false,
        valueColor: Int? = null
    ) {
        val row = layoutInflater.inflate(R.layout.item_coupon_detail_row, container, false)
        row.findViewById<TextView>(R.id.coupon_row_label).text = label
        val valueView = row.findViewById<TextView>(R.id.coupon_row_value)
        valueView.text = value
        if (valueBold) {
            valueView.setTypeface(valueView.typeface, android.graphics.Typeface.BOLD)
        }
        if (valueColor != null) {
            valueView.setTextColor(valueColor)
        }
        container.addView(row)
    }

    private fun setLoading(loading: Boolean) {
        verifyButton.isEnabled = !loading
        verifyButton.text = getString(if (loading) R.string.coupon_checking else R.string.coupon_verify)
        codeInput.isEnabled = !loading
    }

    private fun display(value: String?): String =
        value?.takeIf { it.isNotBlank() } ?: getString(R.string.coupon_dash)

    private fun formatRegion(region: String?): String {
        return when (region?.lowercase(Locale.US)) {
            "nigeria" -> getString(R.string.coupon_region_nigeria)
            "foreign" -> getString(R.string.coupon_region_foreign)
            null, "" -> getString(R.string.coupon_dash)
            else -> region.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
        }
    }

    private fun formatDate(value: String?): String {
        if (value.isNullOrBlank()) return getString(R.string.coupon_dash)
        return try {
            val normalized = value.replace("Z", "+0000").replace(Regex("\\.\\d+"), "")
            val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(normalized) ?: return value
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(parsed)
        } catch (_: Exception) {
            value
        }
    }
}
