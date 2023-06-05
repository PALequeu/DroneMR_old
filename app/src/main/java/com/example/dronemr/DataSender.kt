package com.example.dronemr

import okhttp3.OkHttpClient

class DataSender(client: OkHttpClient): Runnable {
    private var client = OkHttpClient()
    private lateinit var jsonMessage : String
    private lateinit var ServerURL : String

    override fun run() {


    }

}