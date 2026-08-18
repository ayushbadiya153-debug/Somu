package com.example

import android.app.Application
import com.example.data.TradingRepository

class NexusTradeApp : Application() {
    lateinit var repository: TradingRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = TradingRepository(this)
    }

    companion object {
        lateinit var instance: NexusTradeApp
            private set
    }
}
