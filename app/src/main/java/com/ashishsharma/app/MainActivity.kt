package com.ashishsharma.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ashishsharma.appupdate.AutoUpdate
import com.ashishsharma.appupdate.Logs

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        Logs.log(AutoUpdate.getAppId(this))
    }
}