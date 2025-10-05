package com.example.widgetbloom

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // UI Components
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var affirmationText: TextView
    private lateinit var weatherDisplay: TextView
    private lateinit var cityInput: EditText
    private lateinit var getWeatherButton: Button
    private lateinit var citySelectionLayout: LinearLayout
    private lateinit var userPhoto: ImageView
    private lateinit var rootLayout: LinearLayout
    private lateinit var allCardViews: List<CardView>

    // Data
    private val affirmations = listOf(
        "I am enough",
        "I radiate positivity",
        "I deserve peace",
        "I am growing every day",
        "I am proud of who I am"
    )

    private val themes = mapOf(
        "lavender" to Theme("#f5f0ff", "#7d33b8"),
        "mint" to Theme("#e6fff5", "#66cdaa"),
        "babyblue" to Theme("#e0f7ff", "#87ceeb"),
        "pink" to Theme("#fff0f5", "#ff69b4")
    )

    // Handlers
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var weatherApi: WeatherApi

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            userPhoto.setImageURI(it)
            userPhoto.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupRetrofit()
        setupLocation()
        
        // Initial updates
        updateDate()
        showAffirmation()
        getUserWeather()
        applyTheme("lavender")

        // Start periodic updates
        startClockUpdate()
        startAffirmationUpdate()

        // Setup listeners
        setupListeners()
    }

    private fun initializeViews() {
        rootLayout = findViewById(R.id.root_layout)
        clockText = findViewById(R.id.clock_text)
        dateText = findViewById(R.id.date_text)
        affirmationText = findViewById(R.id.affirmation_text)
        weatherDisplay = findViewById(R.id.weather_display)
        cityInput = findViewById(R.id.city_input)
        getWeatherButton = findViewById(R.id.get_weather_button)
        citySelectionLayout = findViewById(R.id.city_selection_layout)
        userPhoto = findViewById(R.id.user_photo)

        allCardViews = listOf(
            findViewById(R.id.clock_card),
            findViewById(R.id.affirmation_card),
            findViewById(R.id.weather_card),
            findViewById(R.id.photo_card)
        )
    }

    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        weatherApi = retrofit.create(WeatherApi::class.java)
    }

    private fun setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupListeners() {
        getWeatherButton.setOnClickListener {
            val city = cityInput.text.toString()
            if (city.isNotEmpty()) {
                fetchWeatherByCity(city)
            }
        }

        findViewById<Button>(R.id.upload_photo_button).setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        findViewById<Button>(R.id.theme_lavender).setOnClickListener { applyTheme("lavender") }
        findViewById<Button>(R.id.theme_mint).setOnClickListener { applyTheme("mint") }
        findViewById<Button>(R.id.theme_babyblue).setOnClickListener { applyTheme("babyblue") }
        findViewById<Button>(R.id.theme_pink).setOnClickListener { applyTheme("pink") }
        findViewById<Button>(R.id.email_feedback).setOnClickListener { sendFeedback() }
    }

    private fun startClockUpdate() {
        handler.post(object : Runnable {
            override fun run() {
                updateClock()
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun startAffirmationUpdate() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                showAffirmation()
                handler.postDelayed(this, 10000)
            }
        }, 10000)
    }

    private fun updateClock() {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        clockText.text = sdf.format(Date())
    }

    private fun updateDate() {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        dateText.text = "Today is ${sdf.format(Date())}."
    }

    private fun showAffirmation() {
        val randomAffirmation = affirmations.random()
        affirmationText.text = randomAffirmation
    }

    private fun applyTheme(themeName: String) {
        themes[themeName]?.let { theme ->
            rootLayout.setBackgroundColor(Color.parseColor(theme.bg))
            val borderColor = Color.parseColor(theme.border)
            
            allCardViews.forEach { card ->
                card.setCardBackgroundColor(Color.WHITE)
                card.strokeColor = borderColor
                card.strokeWidth = 4
            }

            // Update button colors
            val buttons = listOf(
                R.id.theme_lavender, R.id.theme_mint, 
                R.id.theme_babyblue, R.id.theme_pink,
                R.id.get_weather_button, R.id.upload_photo_button,
                R.id.email_feedback
            )
            
            buttons.forEach { buttonId ->
                findViewById<Button>(buttonId)?.let { button ->
                    button.setTextColor(borderColor)
                    button.strokeColor = android.content.res.ColorStateList.valueOf(borderColor)
                }
            }
        }
    }

    private fun getUserWeather() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    fetchWeatherByCoords(location.latitude, location.longitude)
                } else {
                    showCityInput()
                }
            }.addOnFailureListener {
                showCityInput()
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun showCityInput() {
        weatherDisplay.text = "Location services denied."
        citySelectionLayout.visibility = View.VISIBLE
    }

    private fun fetchWeatherByCity(city: String) {
        weatherApi.getWeatherByCity(city, API_KEY, "metric")
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { displayWeather(it) }
                    } else {
                        weatherDisplay.text = "City not found."
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    weatherDisplay.text = "Could not retrieve weather."
                }
            })
    }

    private fun fetchWeatherByCoords(lat: Double, lon: Double) {
        weatherApi.getWeatherByCoords(lat, lon, API_KEY, "metric")
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { displayWeather(it) }
                        citySelectionLayout.visibility = View.GONE
                    } else {
                        showCityInput()
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    showCityInput()
                }
            })
    }

    private fun displayWeather(weather: WeatherResponse) {
        val temp = weather.main.temp.toInt()
        val condition = weather.weather[0].main
        weatherDisplay.text = "${weather.name}: ${temp}°C, $condition"
    }

    private fun sendFeedback() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:yasin.anisa@icloud.com")
            putExtra(Intent.EXTRA_SUBJECT, "WidgetBloom Feedback")
        }
        startActivity(Intent.createChooser(intent, "Send Email"))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserWeather()
            } else {
                showCityInput()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val API_KEY = "f7094f47ec179648ceacab8ddc4f2266"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}

data class Theme(val bg: String, val border: String)
