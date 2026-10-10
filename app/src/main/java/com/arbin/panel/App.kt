package com.arbin.panel

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ShizukuHelper.init(this)
    }
}
