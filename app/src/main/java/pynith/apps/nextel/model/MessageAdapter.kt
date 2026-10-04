package pynith.apps.nextel.model

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pynith.apps.nextel.R

class MessageAdapter(private val list: MutableList<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_USER = 1
        const val TYPE_SUPPORT = 2
        fun formatTime(date: String): String {
            return try {
                val input = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", java.util.Locale.getDefault())
                val output = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                val parsed = input.parse(date)
                output.format(parsed!!)
            } catch (e: Exception) {
                date
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (list[position].isUser) TYPE_USER else TYPE_SUPPORT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == TYPE_USER) {
            val view = inflater.inflate(R.layout.item_chat_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_chat_support, parent, false)
            SupportViewHolder(view)
        }
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]

        if (holder is UserViewHolder) {
            holder.bind(item)
        } else if (holder is SupportViewHolder) {
            holder.bind(item)
        }
    }

    // ? USER VIEW (RIGHT SIDE)
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtMessage: TextView = itemView.findViewById(R.id.txtMessage)
        private val txtTime: TextView? = itemView.findViewById(R.id.txtTime)

        fun bind(item: ChatMessage) {
            txtMessage.text = item.message
            txtTime?.text = formatTime(item.date)
        }
    }

    // ? SUPPORT VIEW (LEFT SIDE)
    class SupportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtMessage: TextView = itemView.findViewById(R.id.txtMessage)
        private val txtTime: TextView? = itemView.findViewById(R.id.txtTime)

        fun bind(item: ChatMessage) {
            txtMessage.text = item.message
            txtTime?.text = formatTime(item.date)
        }
    }

}