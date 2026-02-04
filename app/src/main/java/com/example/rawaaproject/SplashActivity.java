package com.example.rawaaproject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * صفحة البداية (سبلاش) تعرض اسم التطبيق مع انيميشن ثم تنتقل إلى MainActivity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 2500L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splash_logo);
        TextView appName = findViewById(R.id.splash_app_name);
        TextView tagline = findViewById(R.id.splash_tagline);

        logo.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_scale_in));
        appName.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_fade_in));
        tagline.startAnimation(AnimationUtils.loadAnimation(this, R.anim.splash_fade_in));

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }, SPLASH_DELAY_MS);
    }
}
