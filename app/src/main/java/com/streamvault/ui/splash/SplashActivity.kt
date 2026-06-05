package com.streamvault.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.streamvault.R
import com.streamvault.databinding.ActivitySplashBinding
import com.streamvault.ui.home.MainActivity

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Animación entrada
        binding.ivLogo.alpha = 0f
        binding.tvAppName.alpha = 0f
        binding.tvSubtitle.alpha = 0f
        binding.tvAuthor.alpha = 0f
        binding.tvWelcome.alpha = 0f
        binding.progressSplash.alpha = 0f

        binding.ivLogo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).setStartDelay(200).start()
        binding.tvAppName.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(600).start()
        binding.tvSubtitle.animate().alpha(1f).setDuration(400).setStartDelay(900).start()
        binding.tvAuthor.animate().alpha(1f).setDuration(400).setStartDelay(1100).start()
        binding.tvWelcome.animate().alpha(1f).setDuration(400).setStartDelay(1300).start()
        binding.progressSplash.animate().alpha(1f).setDuration(300).setStartDelay(1500).start()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 3000)
    }
}
