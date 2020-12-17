package io.github.kazenomachi.time

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.timer

class TimeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time)

        // メインスレッドでHandlerのインスタンスを生成しておく
        val mainHandler: Handler = Handler(Looper.getMainLooper());

        // 時計
        setTimer(mainHandler)
        // 天気
        setCurrentWeather(mainHandler)
        // schedules
        setSchedule(mainHandler)
    }

    private fun setTimer(mainHandler: Handler) {
        // 1秒ごとに時間を更新する
        timer("clock", period=1000) {
            // mainHandler(UI Thread)にテキストの更新を投げる
            mainHandler.post(Runnable() {
                // 現在日時を取得
                val date = Date()
                // textDateに表示する日付を更新
                findViewById<TextView>(R.id.textDate).text = DateFormat.format(getString(R.string.date_format), date).toString()
                // textTimeに表示する時刻を更新
                findViewById<TextView>(R.id.textTime).text = DateFormat.format(getString(R.string.time_format), date).toString()
            })
        }
    }

    private fun setCurrentWeather(mainHandler: Handler) {
        val apiKey = getMetaValue("openWeatherMapApiKey")

        // 大阪市の緯度、経度
        val latitude = "34.6937"
        val longitude = "135.5021"
        val url = URL("https://api.openweathermap.org/data/2.5/onecall?lat=$latitude&lon=$longitude&exclude=minutely,hourly&appid=$apiKey&lang=ja&units=metric")

        // 5分ごとに現在の天気を更新する
        timer("clock", period=1000 * 60 * 5) {
            val executor: ExecutorService = Executors.newSingleThreadExecutor()
            executor.execute(Runnable() {
                // OpenWeatherMapのOne Call APIを叩いて、天気を取得する
                val result = fetch(url)
                val resultJSON = JSONObject(result)

                mainHandler.post(Runnable() {
                    // 現在の天気
                    val current = resultJSON.getJSONObject("current")
                    val weather = current.getJSONArray("weather").getJSONObject(0)

                    // 現在の天気を表すアイコン
                    val currentWeatherIcon = weather.getString("icon")
                    val imgView: ImageView = findViewById(R.id.currentWeatherIcon)
                    setWeatherIcon(currentWeatherIcon, imgView)

                    // 現在の天気（日本語）
                    val currentWeatherDescription = weather.getString("description")
                    findViewById<TextView>(R.id.currentWeather).text = currentWeatherDescription

                    // 現在の気温
                    val currentTemp = current.getString("feels_like")
                    findViewById<TextView>(R.id.currentTemp).text = "$currentTemp°"
                })
            })
        }
    }

    private fun setWeatherIcon(weather: String, imgView: ImageView) {
        // OpenWeatherMapのWeather iconsを取得し、表示する
        val fileName = "$weather@4x.png"
        val url = "https://openweathermap.org/img/wn/$fileName"

        // 指定したURLの画像に置き換える
        Picasso.get().load(url).into(imgView)
    }

    private fun setSchedule(mainHandler: Handler) {
        val timeTreeApiKey = getMetaValue("timeTreeApiKey")
        val timeTreeCalendarId = getMetaValue("timeTreeCalendarId")
        val url = URL("https://timetreeapis.com/calendars/$timeTreeCalendarId/upcoming_events?timezone=Asia/Tokyo&days=1&include=creator,label,attendees")
        val headers: Map<String, String> = mapOf(
                Pair("Accept", "application/vnd.timetree.v1+json"),
                Pair( "Authorization", "Bearer $timeTreeApiKey")
        )

        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        executor.execute(Runnable() {
            val result = fetch(url, headers)
            val resultJSON = JSONObject(result)
            val results = resultJSON.getJSONArray("data")

            var schedules: ArrayList<Schedule> = ArrayList()

            for (i in 0 until results.length()) {
                val schedule = results.getJSONObject(i)
                val attributes = schedule.getJSONObject("attributes")

                val title = attributes.getString("title")
                val allDay = attributes.getBoolean("all_day")
                val startAt = attributes.getString("start_at")
                val endAt = attributes.getString("end_at")

                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
                val timeFormat = SimpleDateFormat("HH:mm")
                timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"))

                val startTime: String? = timeFormat.format(dateFormat.parse(startAt)).toString()
                val endTime: String? = timeFormat.format(dateFormat.parse(endAt)).toString()

                schedules.add(Schedule(title, startTime, endTime, allDay))
            }

            mainHandler.post(Runnable() {
                findViewById<RecyclerView>(R.id.schedulesView).also { recyclerView: RecyclerView ->
                    recyclerView.adapter = SchedulesAdapter(this, schedules)
                    recyclerView.layoutManager = LinearLayoutManager(this)
                }
            })
        })
    }

    private fun fetch(url: URL, headers: Map<String, String> = mapOf()): String {
        // 受け取ったURLに接続する
        val connection = url.openConnection() as HttpURLConnection
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
        connection.connect()

        // 1行ずつ取得して文字列を返す
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val buffer = StringBuffer()
        var line: String?
        while (true) {
            line = reader.readLine()
            if (line == null) break
            buffer.append(line)
        }
        return buffer.toString()
    }

    private fun getMetaValue(key: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            info.metaData.getString(key).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            ""
        }
    }
}