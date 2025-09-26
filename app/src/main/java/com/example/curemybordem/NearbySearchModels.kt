package com.example.curemybordem

// Request models
data class NearbySearchRequest(
    val includedTypes: List<String>? = null,
    val maxResultCount: Int = 10,
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

// Response models
data class NearbySearchResponse(
    val places: List<Place>? = null
)

data class Place(
    val displayName: DisplayName? = null,
    val formattedAddress: String? = null,
    val types: List<String>? = null // 👈 added types field
)

data class DisplayName(
    val text: String? = null,
    val languageCode: String? = null
)
