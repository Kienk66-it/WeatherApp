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

class HourlyForecastAdapter(private val forecastList: List<ForecastItem>) :
    RecyclerView.Adapter<HourlyForecastAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val time: TextView = itemView.findViewById(R.id.hourlyTime)
        val temp: TextView = itemView.findViewById(R.id.hourlyTemp)
        val icon: ImageView = itemView.findViewById(R.id.hourlyIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hourly_forecast, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val forecast = forecastList[position]
        val date = Date(forecast.dt * 1000)
        holder.time.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        holder.temp.text = "${forecast.main.temp.toInt()}°C"
        val iconUrl = "https://openweathermap.org/img/wn/${forecast.weather[0].icon}.png"
        Glide.with(holder.itemView.context).load(iconUrl).into(holder.icon)
    }

    override fun getItemCount() = forecastList.size
}