package com.example.cpn // Declares the package for this Kotlin file

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class TokenViewModel : ViewModel() { // Defines the TokenViewModel class, inheriting from ViewModel

    // to call the fetchFcmToken() call every time i use init {} block
    init { // Initializer block, executed when an instance of TokenViewModel is created
        fetchFcmToken() // Calls the function to fetch the FCM token immediately upon ViewModel creation
    }

    // ues ViewModel to get Token asynchronously
    // MutableStateFlow is a hot flow that holds a single value and emits updates to collectors.
    // It's mutable, meaning its value can be changed.
    val _myToken = MutableStateFlow<String?>(null) // Private mutable StateFlow to hold the FCM token, initialized to null
    // StateFlow is a read-only interface for MutableStateFlow.
    // It's exposed publicly to allow UI components to observe the token without modifying it directly.
    val myToken: StateFlow<String?> = _myToken // Public immutable StateFlow for observing the FCM token

    fun fetchFcmToken() { // Function to asynchronously fetch the FCM token
        // unless this view model is live this function stay live
        // viewModelScope.launch ensures the coroutine runs as long as the ViewModel is active.
        // If the ViewModel is cleared (e.g., activity destroyed), the coroutine is cancelled.
        viewModelScope.launch {
            try { // Standard try-catch block for error handling in asynchronous operations
                // FirebaseMessaging.getInstance().token returns a Task<String>.
                // .await() suspends the coroutine until the Task completes and returns its result (the token).
                val token:String = FirebaseMessaging.getInstance().token.await()
                _myToken.value = token // Updates the MutableStateFlow with the fetched token, notifying observers
                if(token!=null){ // Checks if the token was successfully retrieved (not null)
                    sendRegistrationToServer(token,"Deepak Guleria") // Calls function to send token to your server
                }
            } catch (e: Exception) { // Catches any exceptions that occur during token fetching
                e.printStackTrace() // Prints the stack trace for debugging
                _myToken.value = "Error Getting Token" // Updates the StateFlow with an error message
            }
        }
    }

    // send teh token to sever for notification
    fun sendRegistrationToServer(token:String,UserId:String){ // Function to send the FCM token to your Ktor server
        // viewModelScope.launch(Dispatchers.IO) launches a coroutine on the IO dispatcher.
        // Dispatchers.IO is suitable for network operations and disk I/O, as it's optimized for blocking operations.
        viewModelScope.launch(Dispatchers.IO) {
            val client = OkHttpClient.Builder().build() // Creates an instance of OkHttpClient for making HTTP requests
            // Defines the content type for the request body as JSON with UTF-8 encoding.
            val JsonMediaType = "application/json; charset=utf-8".toMediaType()

            // make request body through Gson
            // Creates a Kotlin Map to hold the data for the request body (token and userId)
            val requestBodyMap = mapOf(
                "token" to token, // Maps the FCM token to the "token" key
                "userId" to UserId // Maps the provided UserId to the "userId" key
            )

            // Converts the Kotlin Map into a JSON string using Gson.
            val jsonBody = Gson().toJson(requestBodyMap)
            Log.d("json Data","$jsonBody") // Logs the JSON request body for debugging

            // here we create the request
            // Builds an HTTP POST request using OkHttp's Request.Builder
            val request = Request.Builder()
                // Android Emulator se local PC ke liye 10.0.2.2 use hota hai
                // if you are using the real device then use your pc ip address
                // Sets the URL for the HTTP request.
                // "http://10.0.2.2:8080" is for connecting to your host machine from an Android Emulator.
                // "http://192.168.1.3:8080" is an example for a real device, you'd replace 192.168.1.3 with your PC's actual IP.
                .url("http://192.168.1.3:8080/register-device-token")
                // Sets the HTTP method to POST and attaches the JSON request body.
                .post(jsonBody.toRequestBody(JsonMediaType))
                .build() // Builds the final Request object


            try{ // Standard try-catch block for handling network operation errors
                // Executes the HTTP request synchronously (on the Dispatchers.IO thread).
                val response = client.newCall(request).execute()
                if(response.isSuccessful){ // Checks if the HTTP response status code indicates success (2xx range)
                    // Logs a success message and the response body (if any)
                    Log.d("SERVER_REG", "Token sent to server successfully: ${response.body?.string()}")
                }else{ // If the response was not successful
                    // Logs an error message including the HTTP status code, message, and response body
                    Log.e("SERVER_REG", "Failed to send token to server: ${response.code} ${response.message} - ${response.body?.string()}")

                }
            }catch (e: Exception){ // Catches any exceptions that occur during the network call (e.g., no internet, connection refused)
                Log.e("SERVER_REG", "Error sending token to server", e) // Logs the error
            }

        }
    }
}