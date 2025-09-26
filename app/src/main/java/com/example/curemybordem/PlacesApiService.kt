package com.example.curemybordem

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// Google Places (NEW) Nearby Search
interface PlacesApiService {
    @GET("place/nearbysearch/json")
    fun getNearbyPlaces(
        @Query("location") location: String,
        @Query("radius") radius: Int,
        @Query("type") type: String = "point_of_interest",
        @Query("key") apiKey: String
    ): Call<PlacesResponse>
}

