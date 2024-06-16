Libraries I have made use of are:
implementation("com.squareup.okhttp3:okhttp:4.12.0") : The library is used to connect to "openweathermap" API's
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") : The library is used to log all the API connections on the logs
implementation("com.google.android.gms:play-services-location:21.3.0") : The library is used to get current location via "FusedLocationProviderClient" class
implementation("com.google.android.libraries.places:places:3.5.0") : The library is used to get the list of places when searching for them on the AutocompleteSupportFragment fragment

I have excluded my API key for google places.
I have just also excluded my API key for openweathermap.
