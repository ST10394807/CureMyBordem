package com.example.curemybordem

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var apiService: PlacesApiServiceNew
    private lateinit var placesAdapter: PlacesAdapter
    private val apiKey = BuildConfig.MAPS_API_KEY
    private val tag = "AppDebug"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(tag, "MainActivity started")

        if (apiKey.isBlank()) {
            Log.e(tag, "API key is missing! Check local.properties and BuildConfig.")
        }

        setupRetrofit()
        setupRecyclerView()

        val searchButton = findViewById<Button>(R.id.searchButton)
        val distanceInput = findViewById<EditText>(R.id.distanceInput)

        searchButton.setOnClickListener {
            Log.d(tag, "Search button clicked")
            val distanceKm = distanceInput.text.toString().toDoubleOrNull()
            if (distanceKm == null) {
                Toast.makeText(this, "Enter distance in km", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val radiusMeters = distanceKm * 1000
            checkPermissionsAndFetchLocation(radiusMeters)
        }
    }

    /** Setup RecyclerView with adapter */
    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.placesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        placesAdapter = PlacesAdapter()
        recyclerView.adapter = placesAdapter
        Log.d(tag, "RecyclerView initialized")
    }

    /** Configure Retrofit client */
    private fun setupRetrofit() {
        val logging = HttpLoggingInterceptor { msg -> Log.d("$tag-HTTP", msg) }
        logging.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val original = chain.request()
                val newRequest = original.newBuilder()
                    .addHeader("X-Goog-Api-Key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader(
                        "X-Goog-FieldMask",
                        "places.displayName,places.formattedAddress,places.types"
                    )
                    .build()
                Log.d(tag, "Outgoing request: ${newRequest.method} ${newRequest.url}")
                chain.proceed(newRequest)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://places.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        apiService = retrofit.create(PlacesApiServiceNew::class.java)
        Log.d(tag, "Retrofit initialized with baseUrl=https://places.googleapis.com/v1/")
    }

    /** Check location permissions */
    private fun checkPermissionsAndFetchLocation(radius: Double) {
        Log.d(tag, "Checking location permissions...")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            fetchUserLocation(radius)
        }
    }

    /** Fetch user location */
    private fun fetchUserLocation(radius: Double) {
        Log.d(tag, "fetchUserLocation() called")
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude
                Log.i(tag, "Got location: lat=$lat, lng=$lng, radius=$radius")

                val request = NearbySearchRequest(
                    includedTypes = listOf("restaurant"),
                    maxResultCount = 10,
                    locationRestriction = LocationRestriction(
                        Circle(LatLng(lat, lng), radius)
                    )
                )

                Log.d(tag, "Prepared NearbySearchRequest: $request")
                makeNearbySearchRequest(request)
            } else {
                Log.e(tag, "Location is null")
            }
        }.addOnFailureListener { e ->
            Log.e(tag, "Failed to get location: ${e.message}", e)
        }
    }

    /** Make Places API request */
    private fun makeNearbySearchRequest(request: NearbySearchRequest) {
        Log.d(tag, "Sending NearbySearchRequest...")

        apiService.searchNearby(request).enqueue(object : Callback<NearbySearchResponse> {
            override fun onResponse(
                call: Call<NearbySearchResponse>,
                response: Response<NearbySearchResponse>
            ) {
                Log.d(tag, "HTTP response received. Success=${response.isSuccessful}")

                if (response.isSuccessful) {
                    val places = response.body()?.places
                    if (places.isNullOrEmpty()) {
                        Log.w(tag, "No places found.")
                        Toast.makeText(this@MainActivity, "No places found.", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.i(tag, "Places found: ${places.size}")
                        placesAdapter.updatePlaces(places) // 🔥 show results in RecyclerView
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(
                        tag,
                        "API error: code=${response.code()}, message=${response.message()}, body=$errorBody"
                    )
                }
            }

            override fun onFailure(call: Call<NearbySearchResponse>, t: Throwable) {
                Log.e(tag, "Network failure: ${t.message}", t)
            }
        })
    }
}
