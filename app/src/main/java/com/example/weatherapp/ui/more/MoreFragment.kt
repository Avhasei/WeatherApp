package com.example.weatherapp.ui.more

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.databinding.FragmentMoreBinding
import com.example.weatherapp.db.FavoriteDB
import com.example.weatherapp.model.SharedViewModel
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

class MoreFragment : Fragment() {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    @SuppressLint("Range")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        var selectedPlace: Place? = null
        val moreViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        _binding = FragmentMoreBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val btnSave: Button = binding.btnSave
        val layoutProgress: ConstraintLayout = binding.layoutProgress
        val layoutFragment: ConstraintLayout = binding.layoutFragment
        val rcySaved: RecyclerView = binding.rcySaved
        val offline: ImageView = binding.offline
        val autocompleteFragment : AutocompleteSupportFragment = childFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment


        lifecycleScope.launch (Dispatchers.Main){
            if (activity?.let { checkForInternet(it) } == true) {
                layoutProgress.visibility = View.VISIBLE

                val currentLocationObj = moreViewModel.getCurrentCoordinates(activity)
                currentLocationObj.await().addOnSuccessListener { location ->
                    lifecycleScope.launch (Dispatchers.Main){
                        val data = ArrayList<MoreItemsViewModel>()

                        val currentWeatherResult = JSONObject(moreViewModel.getCurrentWeatherAPI(location.latitude,location.longitude).await()!!.string())
                        val temp = JSONObject(currentWeatherResult.get("main").toString()).get("temp").toString().toDouble().roundToInt()
                        data.add(MoreItemsViewModel("current","$temp\u00B0"))

                        when (JSONObject(JSONArray(currentWeatherResult.get("weather").toString()).getString(0).toString()).getString("main")) {
                            "Rainy" -> {
                                layoutFragment.setBackgroundResource(R.drawable.bg_rainy)
                            }
                            "Cloudy" -> {
                                layoutFragment.setBackgroundResource(R.drawable.bg_cloudy)
                            }
                            else -> {
                                layoutFragment.setBackgroundResource(R.drawable.bg_sunny)
                            }
                        }

                        val favCursor = moreViewModel.getFav(activity).await()
                        if (favCursor != null) {
                            while(favCursor.moveToNext()){
                                val lat = favCursor.getString(favCursor.getColumnIndex(FavoriteDB.LAT_COl)).toString()
                                val lon = favCursor.getString(favCursor.getColumnIndex(FavoriteDB.LON_COL)).toString()
                                val currentWeatherResultCursor = JSONObject(moreViewModel.getCurrentWeatherAPI(lat.toDouble(),lon.toDouble()).await()!!.string())
                                val nameCursor = currentWeatherResultCursor.get("name").toString()
                                val tempCursor = JSONObject(currentWeatherResultCursor.get("main").toString()).get("temp").toString().toDouble().roundToInt()
                                data.add(MoreItemsViewModel(nameCursor,"$tempCursor\u00B0"))
                            }
                        }

                        val adapter = MoreAdapter(data)
                        rcySaved.adapter = adapter
                        layoutProgress.visibility = View.GONE
                    }
                }
            }else{
                offline.visibility = View.VISIBLE
                rcySaved.visibility = View.GONE
                btnSave.visibility = View.GONE
                autocompleteFragment.view?.visibility = View.GONE
            }
        }

        btnSave.setOnClickListener {
            moreViewModel.addFav(this.activity, selectedPlace?.latLng?.latitude.toString(),selectedPlace?.latLng?.longitude.toString())
            activity?.recreate();
        }

        if (!Places.isInitialized()) {
            this.context?.let { Places.initialize(it, getString(R.string.google_api_key), Locale.ENGLISH) };
        }

        autocompleteFragment.view?.setBackgroundColor(Color.WHITE);
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                // TODO: Get info about the selected place.
                Log.i("Test", "Place: ${place.name}, ${place.latLng}")
                selectedPlace = place
                lifecycleScope.launch (Dispatchers.Main){
                    btnSave.visibility = View.VISIBLE
                    autocompleteFragment.view?.findViewById<View>(com.google.android.libraries.places.R.id.places_autocomplete_clear_button)?.setOnClickListener {
                        autocompleteFragment.setText("");
                        autocompleteFragment.setHint("Search a place")
                        btnSave.visibility = View.GONE
                    }
                }
            }

            override fun onError(p0: Status) {
                // TODO: Handle the error.
                Log.i("Test", "An error occurred: $p0")
                lifecycleScope.launch (Dispatchers.Main){
                    btnSave.visibility = View.GONE
                }
            }
        })
        return root
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