package com.v2ray.ang.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.v2ray.ang.util.MyContextWrapper
import com.v2ray.ang.R
import com.v2ray.ang.dto.SubscriptionItem
import com.v2ray.ang.extension.toast
import com.v2ray.ang.util.MmkvManager
import com.v2ray.ang.util.Utils
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

    private val subStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_SUB, MMKV.MULTI_PROCESS_MODE) }
    private val editSubId by lazy { intent.getStringExtra("subId").orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lzss_login)

        usernameInput = findViewById(R.id.userInput_email)
        passwordInput = findViewById(R.id.userInput_password)

        loginStatus = findViewById(R.id.login_status)

    }

    fun postRequest() {

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

                    saveServer(subscribeUrl)
                    loginStatus.setText("Updating subscription...")
                    MainActivity().importConfigViaSub()



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

    private fun saveServer(subLink: String): Boolean {
        val subItem: SubscriptionItem
        val json = subStorage?.decodeString(editSubId)
        var subId = editSubId
        if (!json.isNullOrBlank()) {
            subItem = Gson().fromJson(json, SubscriptionItem::class.java)
        } else {
            subId = Utils.getUuid()
            subItem = SubscriptionItem()
        }

        subItem.remarks = "LZ-SS.JP"
        subItem.url = subLink
        subItem.enabled = true

        if (TextUtils.isEmpty(subItem.remarks)) {
            toast(R.string.sub_setting_remarks)
            return false
        }
//        if (TextUtils.isEmpty(subItem.url)) {
//            toast(R.string.sub_setting_url)
//            return false
//        }

        subStorage?.encode(subId, Gson().toJson(subItem))
        toast(R.string.toast_success)
        finish()
        return true
    }



}