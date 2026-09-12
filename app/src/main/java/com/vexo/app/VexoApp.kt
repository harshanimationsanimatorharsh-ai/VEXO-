package com.vexo.app

import android.app.Application

class VexoApp : Application() {

    companion object {
        lateinit var instance: VexoApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
