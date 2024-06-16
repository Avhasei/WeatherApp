package com.example.weatherapp.model

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.db.FavoriteDB
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor


class SharedViewModel : ViewModel(){
    private val TAG = "SharedViewModel"
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    private val cancellationTokenSource = CancellationTokenSource()
    private var appId: String = "f44b2fb436ee3b2ed44c6c38d9346bef"
    private var url: String = "https://api.openweathermap.org/data/2.5/"

    fun getCurrentCoordinates(activity: FragmentActivity?): Deferred<Task<Location>> {
        val response = viewModelScope.async(Dispatchers.IO){
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity!!.applicationContext)
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG,"Location Not Enabled")
                withContext(Dispatchers.Main){
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        1
                    )
                }
            }
            return@async fusedLocationProviderClient.getCurrentLocation(priority, cancellationTokenSource.token)
        }
        return response
    }

    fun getCurrentWeatherAPI(lat: Double?, lon: Double?): Deferred<ResponseBody?> {
        return viewModelScope.async(Dispatchers.IO){
            val logging = HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
            val client: OkHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
            val request = Request.Builder()
                .url(url+"weather?lat=$lat&lon=$lon&appid=$appId&units=metric")
                .get()
                .build()
            return@async try {
                client.newCall(request).execute().body
            } catch(e : Exception){
                Log.d(TAG,e.message.toString())
                null
            }
        }
    }

    fun getCurrent5DayWeatherAPI(lat: Double?, lon: Double?): Deferred<ResponseBody?> {
        return viewModelScope.async (Dispatchers.IO) {
            val logging = HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
            val client: OkHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
            val request = Request.Builder()
                .url(url+"forecast?lat=$lat&lon=$lon&appid=$appId&units=metric")
                .get()
                .build()
            return@async try {
                client.newCall(request).execute().body
            } catch(e : Exception){
                Log.d(TAG,e.message.toString())
                null
            }
        }
    }

    fun getFav(activity: FragmentActivity?): Deferred<Cursor?> {
        return viewModelScope.async (Dispatchers.IO) {
            val db = activity?.let { FavoriteDB(it.applicationContext, null) }
            return@async try {
                return@async db?.getFav()
            } catch(e : Exception){
                Log.d(TAG,e.message.toString())
                null
            }
        }
    }

    fun addFav(activity: FragmentActivity?,lat: String, lon:String): Deferred<String> {
        return viewModelScope.async (Dispatchers.IO) {
            val db = activity?.let { FavoriteDB(it.applicationContext, null) }
            if (db != null) {
                db.addFav(lat,lon)
                return@async "pass"
            }else{
                return@async "fail"
            }
        }
    }
}