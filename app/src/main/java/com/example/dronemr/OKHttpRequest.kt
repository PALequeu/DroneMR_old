package com.example.dronemr

/**
 * Created by Rohan Jahagirdar on 07-02-2018.
 */


import android.widget.Toast
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.create
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO


class OkHttpRequest(client: OkHttpClient) {
    private var client = client
    var lastMessage = "ok"

    fun sendLocation(jsonMessage: String, url : String) {

        // Lancement de la coroutine principale

        runBlocking {
            launch(IO) {
                var successful = post(jsonMessage, url)

            }
        }

    }

    fun get() {
        val message = "ok"
        val request = Request.Builder()
            .url("https://api.quotable.io/random")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    lastMessage = response.toString()
                    /**
                    for ((name, value) in response.headers) {

                        println("$name: $value")
                    }

                    println(response.body!!.string())
                    */
                }
            }

        })

    }

    suspend fun post(jsonMessage: String, url : String){

        val request = Request.Builder()
            .url(url)
            .post(jsonMessage.toRequestBody(MEDIA_TYPE_MARKDOWN))
            .build()


        withContext(IO) {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                println("JSON envoyé avec succès au serveur.")
            } else {
                println("Erreur lors de l'envoi du JSON au serveur: ${response.code}")
            }

        }



        /**
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            //println(response.body!!.string())
        }
        */
    }

    companion object {
        val MEDIA_TYPE_MARKDOWN = "text/x-markdown; charset=utf-8".toMediaType()
    }


}