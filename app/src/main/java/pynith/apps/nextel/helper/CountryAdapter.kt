package pynith.apps.nextel.helper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pynith.apps.nextel.R
import pynith.apps.nextel.model.Country

class CountryAdapter(
    private var countries: List<Country>,
    private val onCountrySelected: (Country) -> Unit
) : RecyclerView.Adapter<CountryAdapter.CountryViewHolder>() {

    inner class CountryViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val flag: TextView =
            itemView.findViewById(R.id.flag)

        private val name: TextView =
            itemView.findViewById(R.id.countryName)

        private val dialCode: TextView =
            itemView.findViewById(R.id.dialCode)

        fun bind(country: Country) {

            flag.text = country.flag
            name.text = country.name
            dialCode.text = country.dialCode

            itemView.setOnClickListener {
                onCountrySelected(country)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CountryViewHolder {

        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_country,
                parent,
                false
            )

        return CountryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CountryViewHolder,
        position: Int
    ) {
        holder.bind(countries[position])
    }

    override fun getItemCount(): Int {
        return countries.size
    }

    fun updateList(newList: List<Country>) {
        countries = newList
        notifyDataSetChanged()
    }
}