package com.devssocial.localodge

import android.app.Application
import android.graphics.Typeface
import androidx.annotation.NonNull
import es.dmoral.toasty.Toasty
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump


class LocalodgeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Calligraphy
        ViewPump.init(
            ViewPump.builder()
                .addInterceptor(
                    CalligraphyInterceptor(
                        CalligraphyConfig.Builder()
                            .setDefaultFontPath("fonts/Montserrat-Medium.ttf")
                            .setFontAttrId(R.attr.fontPath)
                            .build()
                    )
                )
                .build()
        )

        // Toasty
        val assetManager = applicationContext.resources.assets
        Toasty.Config.getInstance()
            .setToastTypeface(Typeface.createFromAsset(assetManager, "fonts/Montserrat-SemiBold.ttf"))
            .apply()
    }

}