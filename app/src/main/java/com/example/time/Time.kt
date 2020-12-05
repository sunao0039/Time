package com.example.time

import android.app.Activity
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TextView
import java.util.*
import android.os.Handler
import android.os.Looper
import kotlin.concurrent.timer

class Time : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time)

        // 時計
        setTimer()
    }

    private fun setTimer() {
        // メインスレッドでHandlerのインスタンスを生成しておく
        val mainHandler: Handler = Handler(Looper.getMainLooper());

        // 1秒ごとに時間を更新する
        timer("clock", period=1000) {
            // mainHandler(UI Thread)にテキストの更新を投げる
            mainHandler.post(Runnable() {
                // 現在日時を取得
                val date = Date()
                // textDateに表示する日付を更新
                findViewById<TextView>(R.id.textDate).text = DateFormat.format("yyyy/MM/dd", date).toString()
                // textTimeに表示する時刻を更新
                findViewById<TextView>(R.id.textTime).text = DateFormat.format("kk:mm:ss", date).toString()
            })
        }
    }
}