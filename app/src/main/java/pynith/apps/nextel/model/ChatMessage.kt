package pynith.apps.nextel.model

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val date: String
)