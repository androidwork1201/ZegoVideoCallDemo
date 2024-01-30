package com.example.zegovideocalldemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.SeekBar;
import android.widget.Toast;

import com.example.zegovideocalldemo.databinding.ActivityMainBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import im.zego.effects.ZegoEffects;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private String licenseData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        setContentView(binding.getRoot());



        //初始化AI 美顏套件資料
        settingAIEffectResource();

        binding.ll.setOnClickListener(v -> {

            Intent intent = new Intent(MainActivity.this, CallActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("userId", generateUserID());
            bundle.putString("userName", "user_" + generateUserID());
            bundle.putString("roomId", "call_video_room");
            bundle.putString("license", licenseData);
            intent.putExtra("bundle", bundle);

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.CAMERA,
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.MODIFY_AUDIO_SETTINGS}, 100);
            } else {
                startActivity(intent);
            }
        });
    }

    /**
     * 隨機產生UserID
     */
    private static String generateUserID() {
        StringBuilder builder = new StringBuilder();
        Random random = new Random();
        while (builder.length() < 5) {
            int number = random.nextInt(10);
            if (builder.length() == 0 && number == 0) continue;
            builder.append(number);
        }
        return builder.toString();
    }

    private void settingAIEffectResource() {
        String encryptInfo = ZegoEffects.getAuthInfo("88688e6673bb3362bfb16f5d6d3833337c2d638fb6fba5d84c0b1ba201e9784f", this);

        new Handler(Looper.getMainLooper()).post(() -> {
            viewModel.getZegoEffectData( "DescribeEffectsLicense",172388699, encryptInfo);
        });

        viewModel.getLicenseData().observe(this, license -> {
            licenseData = license;
        });
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "get Permission success", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "not get Permission success", Toast.LENGTH_SHORT).show();
            }
        }
    }
}