package kien.prj.weatherapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class DailyForecastAdapter(private val dailyForecastList: List<DailyForecastItem>) :
    RecyclerView.Adapter<DailyForecastAdapter.ViewHolder>() {

    data class DailyForecastItem(
        val dt: Long,
        val tempMax: Float,
        val tempMin: Float,
        val weather: Weather
    )

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeDate: TextView = itemView.findViewById(R.id.dailyTimeDate)
        val icon: ImageView = itemView.findViewById(R.id.dailyIcon)
        val tempRange: TextView = itemView.findViewById(R.id.dailyTempRange)
        val description: TextView = itemView.findViewById(R.id.dailyDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_forecast, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val forecast = dailyForecastList[position]
        val date = Date(forecast.dt * 1000)

        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        val dayOfWeek = SimpleDateFormat("EEEE", Locale("vi")).format(date)
        val dateStr = SimpleDateFormat("dd/MM", Locale.getDefault()).format(date)
        holder.timeDate.text = "$time $dayOfWeek $dateStr"

        val iconCode = forecast.weather.icon
        val iconUrl = "https://openweathermap.org/img/wn/$iconCode.png"
        Glide.with(holder.itemView.context).load(iconUrl).into(holder.icon)

        holder.tempRange.text = "${forecast.tempMax.toInt()}°C / ${forecast.tempMin.toInt()}°C"
        holder.description.text = translateWeatherDescription(forecast.weather.description)
    }

    override fun getItemCount() = dailyForecastList.size

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
}