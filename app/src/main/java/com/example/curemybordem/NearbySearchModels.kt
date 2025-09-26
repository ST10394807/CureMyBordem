package com.example.curemybordem

// -----------------------------
// Models for Google Places API
// -----------------------------

data class NearbySearchRequest(
    val includedTypes: List<String>,
    val maxResultCount: Int,
    val locationRestriction: LocationRestriction
)

data class LocationRestriction(
    val circle: Circle
)

data class Circle(
    val center: LatLng,
    val radius: Double
)

data class LatLng(
    val latitude: Double,
    val longitude: Double
)

// -----------------------------
// Response models
// -----------------------------

data class NearbySearchResponse(
    val places: List<Place>?
)

data class Place(
    val displayName: DisplayName?,
    val formattedAddress: String?,
    val types: List<String>?,
    val location: LatLng? // added to support “View on Map” + distance
)

data class DisplayName(
    val text: String?
)
