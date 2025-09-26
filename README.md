# CureMyBordem – Nearby Places App

This is my ICE 3 submission for **PROG7314 (Programming 3A)**.  
The app demonstrates working with the **Google Places API**, location services, Retrofit, and RecyclerView in Android.  

---

## ⚙️ How to Run the App

### Requirements
- Android Studio (latest stable version – I used **Android Studio Koala**).
- Android SDK 34+ installed.
- A real device or emulator with Google Play Services.
- Internet connection (required for the Places API).

### Setup Steps
1. Clone this repository or extract the provided ZIP file:
   ```bash
   git clone https://github.com/ST10394807/CureMyBordem.git
Or unzip CureMyBordem.zip.

Open the project in Android Studio.

Configure your Google Maps API Key:

Get an API key from the Google Cloud Console
.

In local.properties, add:

MAPS_API_KEY=your_api_key_here


The app uses BuildConfig.MAPS_API_KEY to inject this key at runtime.

Sync Gradle and build the project.

Run on an emulator or physical Android device.

📱 Features

Current Location Search
Enter a radius (in km) and the app fetches nearby restaurants using the Google Places API.

Custom Location Mode
Toggle "Advanced" to enter a custom latitude & longitude.

Button: Set Location → locks the custom coordinates.

Button: Revert to My Location → goes back to GPS mode.

Results List
Results are shown in a RecyclerView with:

Place name

Address

Distance (sorted ascending)

A “View on Map” button → opens Google Maps directly.

Error Handling
Logs all API calls, handles missing permissions, and shows clear error messages.

🧪 Notes for Lecturer

The app already has logging (AppDebug) so you can trace all API calls and responses via Logcat.

If no API key is provided, the app will not return results (this is expected).

Tested on Android 13 physical device and emulator.

🙋 Author

Braydon (ST10394807)
Proudly coded for PROG7314 – ICE 3.
