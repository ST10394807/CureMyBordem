package com.example.curemybordem

data class PlacesResponse(
    val results: List<PlaceResult>?,
    val status: String?
)

data class PlaceResult(
    val name: String?,
    val vicinity: String?
)

