package com.example.weatherapp.ui.current

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.databinding.FragmentCurrentBinding
import com.example.weatherapp.model.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt


class CurrentFragment : Fragment() {

    private var _binding: FragmentCurrentBinding? = null
    private val binding get() = _binding!!

    private lateinit var txtTemp: TextView
    private lateinit var txtMain: TextView
    private lateinit var txtMin: TextView
    private lateinit var txtCurrent: TextView
    private lateinit var txtMax: TextView
    private lateinit var layoutProgress: ConstraintLayout
    private lateinit var layoutFragment: ConstraintLayout
    private lateinit var layoutBanner: ConstraintLayout
    private lateinit var layoutCurrent: LinearLayout
    private lateinit var rcyDays: RecyclerView
    private lateinit var  offline: ImageView

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val preferences = this.requireActivity().getSharedPreferences("WeatherApp", Context.MODE_PRIVATE)

        val currentViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        _binding = FragmentCurrentBinding.inflate(inflater, container, false)
        val root: View = binding.root
        txtTemp = binding.txtTemp
        txtMain = binding.txtMain
        txtMin = binding.txtMin
        txtCurrent = binding.txtCurrent
        txtMax = binding.txtMax
        layoutProgress = binding.layoutProgress
        layoutFragment = binding.layoutFragment
        layoutBanner = binding.layoutBanner
        layoutCurrent = binding.layoutCurrent
        rcyDays = binding.rcyDays
        offline = binding.offline

        lifecycleScope.launch (Dispatchers.Main){
            if (activity?.let { checkForInternet(it) } == true) {
                rcyDays.visibility = View.VISIBLE
                offline.visibility = View.GONE
                layoutProgress.visibility = View.VISIBLE
                val currentLocationObj = currentViewModel.getCurrentCoordinates(activity)
                currentLocationObj.await().addOnSuccessListener { location ->
                    lifecycleScope.launch (Dispatchers.Main){
                        val currentWeatherResult = JSONObject(currentViewModel.getCurrentWeatherAPI(location.latitude,location.longitude).await()!!.string())
                        setCurrentView(currentWeatherResult)

                        val myEdit: SharedPreferences.Editor = preferences.edit()
                        myEdit.putString("obj",currentWeatherResult.toString())
                        myEdit.commit();

                        val data = ArrayList<CurrentItemsViewModel>()
                        val current5DayWeatherResult = JSONArray(JSONObject(currentViewModel.getCurrent5DayWeatherAPI(location.latitude,location.longitude).await()!!.string()).getString("list").toString())
                        var nextDay = ""
                        for (i in 0 until current5DayWeatherResult.length()) {
                            val txtTempRow = JSONObject(JSONObject(current5DayWeatherResult.get(i).toString()).get("main").toString()).get("temp").toString().toDouble().roundToInt()
                            val txtMainRow = JSONObject(JSONArray(JSONObject(current5DayWeatherResult.get(i).toString()).get("weather").toString()).get(0).toString()).get("main").toString()
                            val txtDayRow = JSONObject(current5DayWeatherResult.get(i).toString()).get("dt_txt").toString().split(" ")[0]
                            val date = SimpleDateFormat("yyyy-MM-dd").parse(txtDayRow)
                            if(nextDay != txtDayRow){
                                val dayOfWeek = SimpleDateFormat("EEEE", Locale.ENGLISH).format(date.time)
                                data.add(CurrentItemsViewModel(dayOfWeek,txtMainRow,"$txtTempRow\u00B0"))
                                nextDay = txtDayRow
                            }

                        }
                        val adapter = CurrentAdapter(data)
                        rcyDays.adapter = adapter
                        layoutProgress.visibility = View.GONE
                    }
                }
            } else {
                val currentWeatherResult = JSONObject(preferences.getString("obj",null).toString())
                rcyDays.visibility = View.GONE
                offline.visibility = View.VISIBLE
                setCurrentView(currentWeatherResult)
            }
        }
        return root
    }

    @SuppressLint("SetTextI18n")
    fun setCurrentView(currentWeatherResult: JSONObject){
        val temp = JSONObject(currentWeatherResult.get("main").toString()).get("temp").toString().toDouble().roundToInt()
        val main = JSONObject(JSONArray(currentWeatherResult.get("weather").toString()).getString(0).toString()).getString("main")
        val min = JSONObject(currentWeatherResult.get("main").toString()).get("temp_min").toString().toDouble().roundToInt()
        val max = JSONObject(currentWeatherResult.get("main").toString()).get("temp_max").toString().toDouble().roundToInt()

        txtTemp.text = "$temp\u00B0"
        txtMain.text = main.toString()
        txtMin.text = "$min\u00B0"+System.getProperty("line.separator")+"Min"
        txtCurrent.text = "$temp\u00B0"+System.getProperty("line.separator")+"Current"
        txtMax.text = "$max\u00B0"+System.getProperty("line.separator")+"Max"

        when (main) {
            "Rainy" -> {
                layoutFragment.setBackgroundResource(R.drawable.bg_rainy)
                layoutBanner.setBackgroundResource(R.drawable.forest_rainy)
                layoutCurrent.setBackgroundResource(R.drawable.bg_rainy)
                rcyDays.setBackgroundResource(R.drawable.bg_rainy)
            }
            "Cloudy" -> {
                layoutFragment.setBackgroundResource(R.drawable.bg_cloudy)
                layoutBanner.setBackgroundResource(R.drawable.forest_cloudy)
                layoutCurrent.setBackgroundResource(R.drawable.bg_cloudy)
                rcyDays.setBackgroundResource(R.drawable.bg_cloudy)
            }
            else -> {
                layoutFragment.setBackgroundResource(R.drawable.bg_sunny)
                layoutBanner.setBackgroundResource(R.drawable.forest_sunny)
                layoutCurrent.setBackgroundResource(R.drawable.bg_sunny)
                rcyDays.setBackgroundResource(R.drawable.bg_sunny)
            }
        }
    }

    @SuppressLint("ServiceCast")
    private fun checkForInternet(context: Context): Boolean {

        // register activity with the connectivity manager service
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // if the android version is equal to M
        // or greater we need to use the
        // NetworkCapabilities to check what type of
        // network has the internet connection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Returns a Network object corresponding to
            // the currently active default data network.
            val network = connectivityManager.activeNetwork ?: return false

            // Representation of the capabilities of an active network.
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                // Indicates this network uses a Wi-Fi transport,
                // or WiFi has network connectivity
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

                // Indicates this network uses a Cellular transport. or
                // Cellular has network connectivity
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

                // else return false
                else -> false
            }
        } else {
            // if the android version is below M
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}