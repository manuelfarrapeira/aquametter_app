package com.codefm.aquameter.ui.screens.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.codefm.aquameter.databinding.ActivityHomeBinding

/**
 * Activity de Home/Bienvenida
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mostrar mensaje de bienvenida
        binding.greetingText.text = "¡Bienvenido a Aquameter!"
    }
}

