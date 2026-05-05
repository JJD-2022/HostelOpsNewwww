package com.hostelops

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.hostelops.databinding.ActivityMainBinding
import com.hostelops.utils.CloudinaryHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CloudinaryHelper.init(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
