package com.example.curemybordem

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlacesAdapter : RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder>() {

    private val placesList = mutableListOf<Place>()

    // Update dataset and refresh RecyclerView
    fun updatePlaces(newPlaces: List<Place>) {
        placesList.clear()
        placesList.addAll(newPlaces)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(placesList[position])
    }

    override fun getItemCount(): Int = placesList.size

    class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val placeName: TextView = itemView.findViewById(R.id.placeName)
        private val placeAddress: TextView = itemView.findViewById(R.id.placeAddress)
        private val placeTypes: TextView = itemView.findViewById(R.id.placeTypes)

        fun bind(place: Place) {
            placeName.text = place.displayName?.text ?: "Unnamed"
            placeAddress.text = place.formattedAddress ?: "No address"
            placeTypes.text = place.types?.joinToString(", ") ?: "No types"
        }
    }
}
