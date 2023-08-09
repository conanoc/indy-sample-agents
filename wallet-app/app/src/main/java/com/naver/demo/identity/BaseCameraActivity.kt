package com.naver.demo.identity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.naver.demo.identity.databinding.ActivityQrcodeBinding

abstract class BaseCameraActivity : AppCompatActivity() {

    lateinit var binding: ActivityQrcodeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.cameraView.setLifecycleOwner(this)
    }
}
