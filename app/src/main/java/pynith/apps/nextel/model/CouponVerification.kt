package pynith.apps.nextel.model

import com.google.gson.annotations.SerializedName
import org.json.JSONObject

data class CouponVerification(
    val success: Boolean,
    val message: String,
    val data: CouponData?
)

data class CouponData(
    val id: String?,
    val code: String?,
    val type: String?,
    val typeName: String?,
    val price: String?,
    val region: String?,
    val isSpecial: Boolean,
    val isValid: Boolean,
    val status: String?,
    val usedAt: String?,
    val displayName: String?,
    val packageData: ProductData?,
    val additionalMinutePlan: ProductData?,
    val cloudStoragePlan: ProductData?,
    val batch: BatchData?,
    val agent: AgentData?,
    val redeemer: AgentData?,
    val verification: VerificationData?,
    val createdAt: String?,
    val updatedAt: String?
) {
    val product: ProductData?
        get() = packageData ?: additionalMinutePlan ?: cloudStoragePlan

    companion object {
        fun fromJson(json: JSONObject): CouponData = CouponData(
            id = json.stringish("id"),
            code = json.stringish("code"),
            type = json.stringish("type"),
            typeName = json.stringish("type_name"),
            price = json.stringish("price"),
            region = json.stringish("region"),
            isSpecial = json.optBoolean("is_special", false),
            isValid = json.optBoolean("is_valid", false),
            status = json.stringish("status"),
            usedAt = json.stringish("used_at"),
            displayName = json.stringish("display_name"),
            packageData = json.optJSONObject("package")?.let(ProductData::fromJson),
            additionalMinutePlan = json.optJSONObject("additional_minute_plan")
                ?.let(ProductData::fromJson),
            cloudStoragePlan = json.optJSONObject("cloud_storage_plan")
                ?.let(ProductData::fromJson),
            batch = json.optJSONObject("batch")?.let(BatchData::fromJson),
            agent = json.optJSONObject("agent")?.let(AgentData::fromJson),
            redeemer = json.optJSONObject("redeemer")?.let(AgentData::fromJson),
            verification = json.optJSONObject("verification")?.let(VerificationData::fromJson),
            createdAt = json.stringish("created_at"),
            updatedAt = json.stringish("updated_at")
        )
    }
}

data class ProductData(
    val id: String?,
    val name: String?,
    val price: String?,
    val description: String?,
    val duration: String?,
    val minutes: String?,
    val storage: String?,
    val storageSize: String?
) {
    companion object {
        fun fromJson(json: JSONObject): ProductData = ProductData(
            id = json.stringish("id"),
            name = json.stringish("name"),
            price = json.stringish("price"),
            description = json.stringish("description"),
            duration = json.stringish("duration"),
            minutes = json.stringish("minutes"),
            storage = json.stringish("storage"),
            storageSize = json.stringish("storage_size")
        )
    }
}

data class BatchData(
    val id: String?,
    val reference: String?,
    val type: String?,
    val typeName: String?,
    val region: String?,
    val displayName: String?,
    val createdAt: String?
) {
    companion object {
        fun fromJson(json: JSONObject): BatchData = BatchData(
            id = json.stringish("id"),
            reference = json.stringish("reference"),
            type = json.stringish("type"),
            typeName = json.stringish("type_name"),
            region = json.stringish("region"),
            displayName = json.stringish("display_name"),
            createdAt = json.stringish("created_at")
        )
    }
}

data class AgentData(
    val id: String?,
    val name: String?,
    val username: String?
) {
    companion object {
        fun fromJson(json: JSONObject): AgentData = AgentData(
            id = json.stringish("id"),
            name = json.stringish("name"),
            username = json.stringish("username")
        )
    }
}

data class VerificationData(
    val isValid: Boolean,
    val status: String?,
    val message: String?
) {
    companion object {
        fun fromJson(json: JSONObject): VerificationData = VerificationData(
            isValid = json.optBoolean("is_valid", false),
            status = json.stringish("status"),
            message = json.stringish("message")
        )
    }
}

internal fun JSONObject.stringish(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return when (val value = opt(key)) {
        null, JSONObject.NULL -> null
        else -> value.toString().takeIf { it.isNotBlank() && it != "null" }
    }
}
