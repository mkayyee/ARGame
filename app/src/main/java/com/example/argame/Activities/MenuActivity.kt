package com.example.argame.Activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.argame.R

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_menu)
    }
}
