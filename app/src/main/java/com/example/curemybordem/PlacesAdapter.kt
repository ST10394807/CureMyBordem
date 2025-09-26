package com.example.curemybordem

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.*

class PlacesAdapter(
    private var places: List<Place>,
    private val userLat: Double?,
    private val userLng: Double?
) : RecyclerView.Adapter<PlacesAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.placeName)
        val address: TextView = itemView.findViewById(R.id.placeAddress)
        val types: TextView = itemView.findViewById(R.id.placeTypes)
        val distance: TextView = itemView.findViewById(R.id.placeDistance)
        val mapButton: Button = itemView.findViewById(R.id.btnOpenMap)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = places[position]

        holder.name.text = place.displayName?.text ?: "Unknown"
        holder.address.text = place.formattedAddress ?: "No address"
        holder.types.text = place.types?.joinToString(", ") ?: "No types"

        // Show distance if possible
        if (userLat != null && userLng != null && place.location != null) {
            val d = haversine(userLat, userLng, place.location.latitude, place.location.longitude)
            holder.distance.text = String.format("%.1f km away", d)
            holder.distance.visibility = View.VISIBLE
        } else {
            holder.distance.visibility = View.GONE
        }

        // Map button
        if (place.location != null) {
            holder.mapButton.visibility = View.VISIBLE
            holder.mapButton.setOnClickListener {
                val uri = Uri.parse("geo:${place.location.latitude},${place.location.longitude}?q=${Uri.encode(place.displayName?.text)}")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                holder.itemView.context.startActivity(intent)
            }
        } else {
            holder.mapButton.visibility = View.GONE
        }
    }

    override fun getItemCount() = places.size

    fun updatePlaces(newPlaces: List<Place>) {
        places = newPlaces
        notifyDataSetChanged()
    }

    // Haversine formula for distance (km)
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
