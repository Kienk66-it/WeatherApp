package kien.prj.weatherapp

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>,
    val name: String,
    val wind: Wind,
    val sys: Sys // Thêm sys
)

data class Sys(
    val sunrise: Long,
    val sunset: Long
)

data class ForecastResponse(
    val list: List<ForecastItem>
)

data class Main(
    val temp: Float,
    val feels_like: Float,
    val humidity: Int,
    val pressure: Int
)

data class Weather(
    val description: String,
    val icon: String,
    val main: String, // Thêm main để khớp với API
    val id: Int? = null // Tùy chọn: thêm id nếu bạn muốn dùng
)

data class Wind(
    val speed: Float,
    val deg: Int
)

data class ForecastItem(
    val dt: Long,
    val main: Main,
    val weather: List<Weather>,
    val wind: Wind
)