package com.example.curemybordem

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
import kotlin.math.*

import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var apiService: PlacesApiServiceNew
    private lateinit var placesAdapter: PlacesAdapter
    private val apiKey = BuildConfig.MAPS_API_KEY
    private val tag = "AppDebug"

    // Track user location for distance calc
    private var currentLat: Double? = null
    private var currentLng: Double? = null

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
        val latInput = findViewById<EditText>(R.id.latInput)
        val lngInput = findViewById<EditText>(R.id.lngInput)

        // Toggle advanced section
        val toggleAdvanced = findViewById<TextView>(R.id.toggleAdvanced)
        val advancedLayout = findViewById<View>(R.id.advancedLayout)

        toggleAdvanced.setOnClickListener {
            if (advancedLayout.visibility == View.GONE) {
                advancedLayout.visibility = View.VISIBLE
                toggleAdvanced.text = "Hide advanced options"
            } else {
                advancedLayout.visibility = View.GONE
                toggleAdvanced.text = "Advanced: Enter custom latitude/longitude"
            }
        }

        searchButton.setOnClickListener {
            Log.d(tag, "Search button clicked")

            val distanceKm = distanceInput.text.toString().toDoubleOrNull()
            if (distanceKm == null) {
                Toast.makeText(this, "Enter distance in km", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val radiusMeters = distanceKm * 1000

            // If user entered custom lat/lng, use it
            val latText = latInput.text.toString()
            val lngText = lngInput.text.toString()
            if (latText.isNotBlank() && lngText.isNotBlank()) {
                val lat = latText.toDoubleOrNull()
                val lng = lngText.toDoubleOrNull()
                if (lat != null && lng != null) {
                    Log.i(tag, "Using custom lat/lng: $lat,$lng")
                    currentLat = lat
                    currentLng = lng
                    makeNearbySearchRequest(
                        NearbySearchRequest(
                            includedTypes = listOf("restaurant"),
                            maxResultCount = 10,
                            locationRestriction = LocationRestriction(
                                Circle(LatLng(lat, lng), radiusMeters)
                            )
                        )
                    )
                    return@setOnClickListener
                }
            }

            // Otherwise, use current GPS location
            checkPermissionsAndFetchLocation(radiusMeters)
        }

        val btnSetLocation = findViewById<Button>(R.id.btnSetLocation)
        val btnRevertLocation = findViewById<Button>(R.id.btnRevertLocation)

        btnSetLocation.setOnClickListener {
            val latText = latInput.text.toString()
            val lngText = lngInput.text.toString()
            val lat = latText.toDoubleOrNull()
            val lng = lngText.toDoubleOrNull()

            if (lat != null && lng != null) {
                currentLat = lat
                currentLng = lng
                Toast.makeText(this, "Custom location set: $lat, $lng", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Enter valid latitude & longitude", Toast.LENGTH_SHORT).show()
            }
        }

        btnRevertLocation.setOnClickListener {
            latInput.text.clear()
            lngInput.text.clear()
            currentLat = null
            currentLng = null
            Toast.makeText(this, "Reverted to GPS location", Toast.LENGTH_SHORT).show()
        }

    }

    /** Setup RecyclerView */
    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.placesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        placesAdapter = PlacesAdapter(emptyList(), null, null)
        recyclerView.adapter = placesAdapter
        Log.d(tag, "RecyclerView initialized")
    }

    /** Configure Retrofit */
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
                        "places.displayName,places.formattedAddress,places.types,places.location"
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
                currentLat = location.latitude
                currentLng = location.longitude
                Log.i(tag, "Got location: lat=$currentLat, lng=$currentLng, radius=$radius")

                val request = NearbySearchRequest(
                    includedTypes = listOf("restaurant"),
                    maxResultCount = 10,
                    locationRestriction = LocationRestriction(
                        Circle(LatLng(currentLat!!, currentLng!!), radius)
                    )
                )
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
                        // ✅ Sort places by distance if we have a reference point
                        val sortedPlaces = if (currentLat != null && currentLng != null) {
                            places.sortedBy { place ->
                                place.location?.let {
                                    haversine(currentLat!!, currentLng!!, it.latitude, it.longitude)
                                } ?: Double.MAX_VALUE
                            }
                        } else {
                            places // no sorting if no location available
                        }

                        placesAdapter = PlacesAdapter(sortedPlaces, currentLat, currentLng)
                        findViewById<RecyclerView>(R.id.placesRecyclerView).adapter = placesAdapter

                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(tag, "API error: code=${response.code()}, body=$errorBody")
                }
            }

            override fun onFailure(call: Call<NearbySearchResponse>, t: Throwable) {
                Log.e(tag, "Network failure: ${t.message}", t)
            }
        })
    }
}

private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) *
            cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    return R * 2 * atan2(sqrt(a), sqrt(1 - a))
}
