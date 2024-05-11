package com.v2ray.ang.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.MyContextWrapper
import com.v2ray.ang.R
import com.v2ray.ang.dto.SubscriptionItem
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.AngConfigManager
import com.v2ray.ang.util.MmkvManager
import com.v2ray.ang.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class lzssLogin : AppCompatActivity() {
    lateinit var usernameInput: EditText
    lateinit var passwordInput: EditText
    lateinit var loginStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        RESULT_OK = 0
        println(RESULT_OK)
        super.onCreate(null)
        setContentView(R.layout.activity_lzss_login)

        usernameInput = findViewById(R.id.userInput_email)
        passwordInput = findViewById(R.id.userInput_password)

        loginStatus = findViewById(R.id.login_status)

    }


    fun checkUser(view: View?) {
        var userEmail = usernameInput.text
        var userPassword = passwordInput.text

        println(userEmail)
        println(userPassword)

        Thread {
            val url = URL("https://lz-ss.jp/api/v1/passport/auth/login")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            try {
                val jsonObject = JSONObject().apply {
                    put("email", userEmail)
                    put("password", userPassword)
                }
                val postData = jsonObject.toString()
                val out = BufferedOutputStream(connection.outputStream)
                val writer = OutputStreamWriter(out, "UTF-8")
                writer.write(postData)
                writer.flush()
                writer.close()

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    // Handle the response data here
                    println("Response: $response")
                    loginStatus.setText("Login Successful!")
                    val jsonResponse = JSONObject(response)
                    val data = jsonResponse.getJSONObject("data")
                    setUserAuth(data)
                } else {
                    // Handle error response
                    println("Error: ${connection.responseMessage}")
                    loginStatus.setText("Error: ${connection.responseMessage}")
                }
            } finally {
                connection.disconnect()
            }
        }.start()
    }

    fun setUserAuth(userJson: JSONObject) {
        //println("$userJson")
        var userTokan = userJson.getString("token")
        var userAdmin = userJson.getInt("is_admin")
       var userAuthData = userJson.getString("auth_data")

        loginStatus.setText("Reading your account, Please Wait...")

        Thread {
            val url = URL("https://lz-ss.jp/api/v1/user/info?auth_data=$userAuthData")
            println(url)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            try {
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use(BufferedReader::readText)

                    // Parse the JSON response
                    val jsonResponse = JSONObject(response)
                    val data = jsonResponse.getJSONObject("data")
                    val email = data.getString("email")
                    loginStatus.setText("Hi $email, Getting the link, Please Wait...")

                    getSubLink(userAuthData)
                    // Now you can use the username as needed
                    println("$jsonResponse")

                } else {
                    // Handle error response
                    println("Error: ${connection.responseMessage}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection.disconnect()
            }
        }.start()
    }

    fun getSubLink(userAuth: String) {
        Thread {
            val url = URL("https://lz-ss.jp/api/v1/user/getSubscribe?auth_data=$userAuth")
            println(url)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            try {
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use(BufferedReader::readText)

                    // Parse the JSON response
                    val jsonResponse = JSONObject(response)
                    val data = jsonResponse.getJSONObject("data")
                    val subscribeUrl = data.getString("subscribe_url")
                    // Now you can use the username as needed
                    println("$jsonResponse")
                    loginStatus.setText("$subscribeUrl")

                    deleteAll()

                    importUrlAsSubscription(subscribeUrl)
                    loginStatus.setText("Done!")
                    RESULT_OK = 1
                    println(RESULT_OK)
                    finish()

                } else {
                    // Handle error response
                    println("Error: ${connection.responseMessage}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                connection.disconnect()
            }
        }.start()
    }

    private fun importUrlAsSubscription(mySub: String) {
        MmkvManager.importUrlAsSubscriptionLZSS(mySub)
    }

    private fun deleteAll() {
        MmkvManager.removeAllSubscription()
        MmkvManager.removeAllServer()
    }

    companion object {
        var RESULT_OK = 0
    }

}