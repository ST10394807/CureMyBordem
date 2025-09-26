package com.example.curemybordem

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface PlacesApiServiceNew {
    @Headers(
        "Content-Type: application/json",
        "X-Goog-FieldMask: places.displayName,places.formattedAddress,places.types"
    )
    @POST("v1/places:searchNearby")
    fun searchNearby(
        @Body request: NearbySearchRequest
    ): Call<NearbySearchResponse>
}
