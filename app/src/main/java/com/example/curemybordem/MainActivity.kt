package com.example.curemybordem

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
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
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var apiService: PlacesApiServiceNew
    private lateinit var placesAdapter: PlacesAdapter
    private lateinit var progressBar: ProgressBar
    private val apiKey = BuildConfig.MAPS_API_KEY
    private val tag = "AppDebug"

    // Track user location for distance calc
    private var currentLat: Double? = null
    private var currentLng: Double? = null
    private var currentPlaces: List<Place> = emptyList()

    // Track selected type
    private var selectedType: String = "restaurant"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(tag, "MainActivity started")

        if (apiKey.isBlank()) {
            Log.e(tag, "API key is missing! Check local.properties and BuildConfig.")
        }

        setupRetrofit()
        setupRecyclerView()
        setupTypeDropdown()

        progressBar = findViewById(R.id.progressBar)

        val searchButton = findViewById<Button>(R.id.searchButton)
        val distanceInput = findViewById<EditText>(R.id.distanceInput)
        val latInput = findViewById<EditText>(R.id.latInput)
        val lngInput = findViewById<EditText>(R.id.lngInput)
        val toggleAdvanced = findViewById<TextView>(R.id.toggleAdvanced)
        val advancedLayout = findViewById<View>(R.id.advancedLayout)
        val btnSetLocation = findViewById<Button>(R.id.btnSetLocation)
        val btnRevertLocation = findViewById<Button>(R.id.btnRevertLocation)
        val btnShowAllTypes = findViewById<Button>(R.id.btnShowAllTypes)

        // Toggle advanced inputs
        toggleAdvanced.setOnClickListener {
            if (advancedLayout.visibility == View.GONE) {
                advancedLayout.visibility = View.VISIBLE
                toggleAdvanced.text = "Hide advanced options"
            } else {
                advancedLayout.visibility = View.GONE
                toggleAdvanced.text = "Advanced: Enter custom latitude/longitude"
            }
        }

        // Set custom location
        btnSetLocation.setOnClickListener {
            val lat = latInput.text.toString().toDoubleOrNull()
            val lng = lngInput.text.toString().toDoubleOrNull()
            if (lat != null && lng != null) {
                currentLat = lat
                currentLng = lng
                Toast.makeText(this, "Custom location set: $lat, $lng", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Enter valid latitude & longitude", Toast.LENGTH_SHORT).show()
            }
        }

        // Revert to GPS
        btnRevertLocation.setOnClickListener {
            latInput.text.clear()
            lngInput.text.clear()
            currentLat = null
            currentLng = null
            Toast.makeText(this, "Reverted to GPS location", Toast.LENGTH_SHORT).show()
        }

        // Search button
        searchButton.setOnClickListener {
            val distanceKm = distanceInput.text.toString().toDoubleOrNull()
            if (distanceKm == null) {
                Toast.makeText(this, "Enter distance in km", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val radiusMeters = distanceKm * 1000

            if (currentLat != null && currentLng != null) {
                makeNearbySearchRequest(
                    NearbySearchRequest(
                        includedTypes = listOf(selectedType),
                        maxResultCount = 10,
                        locationRestriction = LocationRestriction(
                            Circle(LatLng(currentLat!!, currentLng!!), radiusMeters)
                        )
                    )
                )
            } else {
                checkPermissionsAndFetchLocation(radiusMeters)
            }
        }

        // Show all place types
        btnShowAllTypes.setOnClickListener {
            showTypesPopup()
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

    /** Setup type dropdown */
    private fun setupTypeDropdown() {
        val typeInput = findViewById<AutoCompleteTextView>(R.id.typeAutoComplete)

        val placeTypes = listOf(
            // Food & Drink
            "restaurant", "cafe", "bar", "bakery", "supermarket", "meal_takeaway", "fast_food",
            "food", "grocery_or_supermarket",

            // Shopping
            "shopping_mall", "store", "clothing_store", "shoe_store", "book_store", "convenience_store",
            "electronics_store", "furniture_store", "jewelry_store", "department_store",

            // Health & Wellness
            "pharmacy", "hospital", "doctor", "dentist", "veterinary_care", "health",

            // Education
            "school", "university", "library",

            // Leisure & Entertainment
            "gym", "park", "museum", "art_gallery", "stadium", "zoo", "aquarium",
            "amusement_park", "movie_theater", "night_club", "tourist_attraction",

            // Travel & Transport
            "train_station", "bus_station", "subway_station", "airport", "taxi_stand", "transit_station",

            // Lodging
            "hotel", "lodging", "campground", "rv_park",

            // Services
            "atm", "bank", "post_office", "gas_station", "car_rental", "car_repair",
            "car_wash", "parking", "police", "fire_station"
        )


        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, placeTypes)
        typeInput.setAdapter(adapter)
        typeInput.setText(selectedType, false)

        typeInput.setOnItemClickListener { _, _, position, _ ->
            selectedType = placeTypes[position]
            Log.i(tag, "Selected type: $selectedType")
        }
    }

    /** Configure Retrofit */
    private fun setupRetrofit() {
        val logging = HttpLoggingInterceptor { msg -> Log.d("$tag-HTTP", msg) }
        logging.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader("X-Goog-Api-Key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader(
                        "X-Goog-FieldMask",
                        "places.displayName,places.formattedAddress,places.types,places.location"
                    )
                    .build()
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            fetchUserLocation(radius)
        }
    }

    /** Fetch user location */
    private fun fetchUserLocation(radius: Double) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLat = location.latitude
                currentLng = location.longitude

                val request = NearbySearchRequest(
                    includedTypes = listOf(selectedType),
                    maxResultCount = 10,
                    locationRestriction = LocationRestriction(
                        Circle(LatLng(currentLat!!, currentLng!!), radius)
                    )
                )
                makeNearbySearchRequest(request)
            } else {
                Log.e(tag, "Location is null")
            }
        }
    }

    /** Make Places API request */
    private fun makeNearbySearchRequest(request: NearbySearchRequest) {
        progressBar.visibility = View.VISIBLE // 👈 Show loading
        apiService.searchNearby(request).enqueue(object : Callback<NearbySearchResponse> {
            override fun onResponse(
                call: Call<NearbySearchResponse>,
                response: Response<NearbySearchResponse>
            ) {
                progressBar.visibility = View.GONE // 👈 Hide loading
                if (response.isSuccessful) {
                    val places = response.body()?.places ?: emptyList()
                    if (places.isEmpty()) {
                        Toast.makeText(this@MainActivity, "No places found.", Toast.LENGTH_SHORT).show()
                    } else {
                        currentPlaces = if (currentLat != null && currentLng != null) {
                            places.sortedBy { p ->
                                p.location?.let { haversine(currentLat!!, currentLng!!, it.latitude, it.longitude) }
                                    ?: Double.MAX_VALUE
                            }
                        } else places

                        placesAdapter = PlacesAdapter(currentPlaces, currentLat, currentLng)
                        findViewById<RecyclerView>(R.id.placesRecyclerView).adapter = placesAdapter
                    }
                } else {
                    Log.e(tag, "API error: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<NearbySearchResponse>, t: Throwable) {
                progressBar.visibility = View.GONE // 👈 Hide loading
                Log.e(tag, "Network failure: ${t.message}", t)
            }
        })
    }

    /** Show types in popup */
    private fun showTypesPopup() {
        val placeTypes = listOf(
            // Food & Drink
            "restaurant", "cafe", "bar", "bakery", "supermarket", "meal_takeaway", "fast_food",
            "food", "grocery_or_supermarket",

            // Shopping
            "shopping_mall", "store", "clothing_store", "shoe_store", "book_store", "convenience_store",
            "electronics_store", "furniture_store", "jewelry_store", "department_store",

            // Health & Wellness
            "pharmacy", "hospital", "doctor", "dentist", "veterinary_care", "health",

            // Education
            "school", "university", "library",

            // Leisure & Entertainment
            "gym", "park", "museum", "art_gallery", "stadium", "zoo", "aquarium",
            "amusement_park", "movie_theater", "night_club", "tourist_attraction",

            // Travel & Transport
            "train_station", "bus_station", "subway_station", "airport", "taxi_stand", "transit_station",

            // Lodging
            "hotel", "lodging", "campground", "rv_park",

            // Services
            "atm", "bank", "post_office", "gas_station", "car_rental", "car_repair",
            "car_wash", "parking", "police", "fire_station"
        )


        AlertDialog.Builder(this)
            .setTitle("Available Place Types")
            .setItems(placeTypes.toTypedArray()) { _, which ->
                selectedType = placeTypes[which]
                findViewById<AutoCompleteTextView>(R.id.typeAutoComplete).setText(selectedType, false)
            }
            .setPositiveButton("Close", null)
            .show()
    }
}

/** Distance helper */
private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2.0)
    return R * 2 * atan2(sqrt(a), sqrt(1 - a))
}
