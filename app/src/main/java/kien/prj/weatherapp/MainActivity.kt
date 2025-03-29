package kien.prj.weatherapp

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kien.prj.weatherapp.databinding.ActivityMainBinding
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val apiKey = "7ca9836def07d34b1479b714ba907427"
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/2.5/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val weatherApi = retrofit.create(WeatherApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load danh sách thành phố bất đồng bộ
        loadCitiesAsync()

        binding.hourlyForecast.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.dailyForecast.layoutManager = LinearLayoutManager(this)

        // Xử lý sự kiện click vào drawableEnd (dấu x)
        binding.searchCity.setOnTouchListener { _, event ->
            val DRAWABLE_END = 2 // Vị trí drawableEnd trong compound drawables
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val drawableEnd = binding.searchCity.compoundDrawables[DRAWABLE_END]
                if (drawableEnd != null && event.rawX >= (binding.searchCity.right - drawableEnd.bounds.width())) {
                    binding.searchCity.text.clear() // Xóa nội dung
                    return@setOnTouchListener true
                }
            }
            false
        }

        binding.searchButton.setOnClickListener {
            val city = binding.searchCity.text.toString().trim()
            if (city.isNotEmpty()) {
                getWeatherData(city)
            } else {
                Toast.makeText(this, getString(R.string.enter_city_error), Toast.LENGTH_SHORT).show()
            }
        }

        requestLocationPermission()
    }

    // Load danh sách thành phố bất đồng bộ
    private fun loadCitiesAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            val cityList = loadCitiesFromJson()
            withContext(Dispatchers.Main) {
                Log.d("MainActivity", "Số thành phố tải được: ${cityList.size}")
                if (cityList.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Không tải được danh sách thành phố", Toast.LENGTH_SHORT).show()
                }
                val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_dropdown_item_1line, cityList)
                binding.searchCity.setAdapter(adapter)
                binding.searchCity.threshold = 1
                Log.d("MainActivity", "Adapter đã được gán với ${adapter.count} mục")
            }
        }
    }

    // Data class để map với JSON
    data class City(val name: String, val country: String)

    // Đọc JSON bằng Gson
    private fun loadCitiesFromJson(): List<String> {
        val cityList = mutableListOf<String>()
        try {
            val inputStream = assets.open("city.list.json")
            val reader = InputStreamReader(inputStream)
            val gson = Gson()
            val cityType = object : TypeToken<List<City>>() {}.type
            val cities: List<City> = gson.fromJson(reader, cityType)
            reader.close()

            // Lọc thành phố Việt Nam và giới hạn số lượng
            val maxCities = 10000
            cities.filter { it.country == "VN" }.take(maxCities).forEach { city ->
                cityList.add("${city.name}, ${city.country}")
            }
            cities.filter { it.country == "KR" }.take(maxCities).forEach { city ->
                cityList.add("${city.name}, ${city.country}")
            }
            Log.d("MainActivity", "Đã đọc xong JSON, số thành phố VN: ${cityList.size}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Lỗi tải file JSON: ${e.message}")
            runOnUiThread {
                Toast.makeText(this, "Lỗi tải danh sách thành phố: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        return cityList
    }

    private fun getWeatherData(city: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val currentWeather = weatherApi.getCurrentWeather(city, apiKey)
                updateCurrentWeatherUI(currentWeather)

                val forecast = weatherApi.getForecast(city, apiKey)
                updateForecastUI(forecast)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCurrentWeatherUI(weather: WeatherResponse) {
        binding.cityName.text = weather.name ?: "Thành phố không xác định"
        binding.currentTemp.text = String.format("%.1f°C", weather.main.temp)
        binding.weatherDescription.text = translateWeatherDescription(weather.weather.getOrNull(0)?.description ?: "N/A")
        binding.feelsLike.text = getString(R.string.feels_like, weather.main.feels_like)
        binding.humidity.text = getString(R.string.humidity, weather.main.humidity)

        val iconCode = weather.weather.getOrNull(0)?.icon
        if (iconCode != null) {
            val iconUrl = "https://api.openweathermap.org/img/wn/$iconCode@2x.png"
            Glide.with(this).load(iconUrl).into(binding.weatherIcon)
        }

        val weatherCondition = weather.weather.getOrNull(0)?.main?.lowercase() ?: "unknown"
        val cardView = binding.currentWeatherCard
        when (weatherCondition) {
            "clear" -> cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#FFF9C4"))
            "clouds" -> cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#ECEFF1"))
            "rain", "drizzle" -> cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#B0BEC5"))
            "thunderstorm" -> cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#CFD8DC"))
            "snow" -> cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#F5F7FA"))
            else -> cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
        }

        // Gợi ý trang phục
        val temp = weather.main.temp
        val humidity = weather.main.humidity
        val windSpeed = weather.wind.speed
        val clothingSuggestion = when {
            weatherCondition in listOf("rain", "drizzle", "thunderstorm") -> "Áo mưa, ô, giày chống nước"
            temp < 15 -> "Áo ấm, áo khoác, quần dài"
            temp in 15.0..25.0 -> "Áo thun, quần dài hoặc ngắn"
            temp > 25 && humidity > 80 -> "Áo thun, quần ngắn, quần áo thoáng khí"
            temp > 25 -> "Áo thun, quần ngắn, mũ"
            windSpeed > 10 -> "Áo gió, khăn quàng"
            else -> "Quần áo thoải mái"
        }
        binding.clothingSuggestion.text = "Gợi ý trang phục: $clothingSuggestion"

        // Gợi ý hoạt động
        val activitySuggestion = suggestActivity(weather)
        binding.activitySuggestion.text = "Gợi ý hoạt động: $activitySuggestion"

        // Thêm thời gian mặt trời mọc và lặn
        val sunrise = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(weather.sys.sunrise * 1000))
        val sunset = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(weather.sys.sunset * 1000))
        binding.feelsLike.text = "Cảm giác: ${weather.main.feels_like}°C\nMặt trời mọc: $sunrise\nMặt trời lặn: $sunset"

    }

    private fun updateForecastUI(forecast: ForecastResponse) {
        val hourlyList = forecast.list.take(5)
        binding.hourlyForecast.adapter = HourlyForecastAdapter(hourlyList)

        val dailyMap = mutableMapOf<String, MutableList<ForecastItem>>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        for (item in forecast.list) {
            val day = dateFormat.format(Date(item.dt * 1000))
            if (!dailyMap.containsKey(day)) {
                dailyMap[day] = mutableListOf()
            }
            dailyMap[day]?.add(item)
        }

        val dailyList = dailyMap.entries.take(7).map { entry ->
            val items = entry.value
            val tempMax = items.maxOf { it.main.temp }
            val tempMin = items.minOf { it.main.temp }
            val representativeItem = items.find { Date(it.dt * 1000).hours == 12 } ?: items[0]
            DailyForecastAdapter.DailyForecastItem(
                dt = representativeItem.dt,
                tempMax = tempMax,
                tempMin = tempMin,
                weather = representativeItem.weather[0]
            )
        }

        binding.dailyForecast.adapter = DailyForecastAdapter(dailyList)
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getWeatherData("Hà Nội")
        } else {
            Toast.makeText(this, getString(R.string.location_denied), Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun translateWeatherDescription(description: String): String {
        return when (description.lowercase()) {
            "clear sky" -> "Trời quang đãng"
            "few clouds" -> "Ít mây"
            "scattered clouds" -> "Mây rải rác"
            "broken clouds" -> "Mây đứt quãng"
            "overcast clouds" -> "Trời nhiều mây"
            "light rain" -> "Mưa nhẹ"
            "moderate rain" -> "Mưa vừa"
            "heavy rain" -> "Mưa lớn"
            "thunderstorm" -> "Giông bão"
            "snow" -> "Tuyết rơi"
            "light snow" -> "Tuyết nhẹ"
            "mist" -> "Sương mù"
            "fog" -> "Sương mù dày"
            else -> description.replaceFirstChar { it.uppercase() }
        }
    }

    private fun suggestActivity(weather: WeatherResponse): String {
        val temp = weather.main.temp
        val humidity = weather.main.humidity
        val windSpeed = weather.wind.speed
        val weatherCondition = weather.weather.getOrNull(0)?.main?.lowercase() ?: "unknown"

        return when {
            // Mưa hoặc giông bão
            weatherCondition in listOf("rain", "drizzle", "thunderstorm") ->
                "Ở trong nhà: Xem phim, đọc sách, hoặc chơi trò chơi điện tử"

            // Nhiệt độ thấp
            temp < 15 ->
                "Hoạt động trong nhà: Nấu ăn, tập yoga, hoặc thưởng thức trà nóng"

            // Nhiệt độ trung bình, thời tiết đẹp
            temp in 15.0..25.0 && weatherCondition == "clear" ->
                "Ngoài trời: Đi dạo công viên, chạy bộ, hoặc picnic nhẹ"

            // Nhiệt độ cao, độ ẩm cao
            temp > 25 && humidity > 80 ->
                "Trong nhà: Nghỉ ngơi, uống nước, hoặc làm việc sáng tạo"

            // Nhiệt độ cao, trời nắng
            temp > 25 && weatherCondition == "clear" ->
                "Ngoài trời: Bơi lội, chơi thể thao (nhớ đội mũ và uống đủ nước)"

            // Gió mạnh
            windSpeed > 10 ->
                "Trong nhà: Vẽ tranh, học một kỹ năng mới, hoặc xem tài liệu"

            // Sương mù
            weatherCondition in listOf("mist", "fog") ->
                "Trong nhà: Thư giãn, nghe nhạc, hoặc sắp xếp lại nhà cửa"

            // Trường hợp mặc định
            else ->
                "Hoạt động nhẹ: Đi bộ, đọc sách ngoài ban công, hoặc thư giãn"
        }
    }

}