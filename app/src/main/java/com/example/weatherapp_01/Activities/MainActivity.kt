package com.example.weatherapp_01.Activities
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.weatherapp_01.Models.WeatherModel
import com.example.weatherapp_01.R
import com.example.weatherapp_01.Utilites.ApiUtilities
import com.example.weatherapp_01.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import kotlin.math.roundToInt
class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationProvider:FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE=101
    private val apiKet="642174014a8fc6b2f5e3b92aea925730"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)
        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()
        binding.btnNavigate.setOnClickListener {
            // Create an Intent to navigate to the AnotherActivity
            val intent = Intent(this@MainActivity, iot::class.java)
            startActivity(intent)
        }
        binding.citySearch.setOnEditorActionListener { textView, i, keyEvent ->
            if (i == EditorInfo.IME_ACTION_SEARCH) {
                getCityWether(binding.citySearch.text.toString())
                val view = this.currentFocus
                if (view != null){
                val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                binding.citySearch.clearFocus()
            }
            return@setOnEditorActionListener true
        } else {
            return@setOnEditorActionListener false
            }
        }
        binding.currentLocation.setOnClickListener {
            getCurrentLocation()
            binding.scrollable.visibility = View.GONE
            binding.mainFrame.visibility = View.VISIBLE
            binding.mm.visibility = View.VISIBLE
        }
        }
    private fun getCityWether(city:String){
        binding.scrollable.visibility = View.VISIBLE
        binding.mm.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
        ApiUtilities.getApiInterface()?.getCityWeatherData(city,apiKet)?.enqueue(
            object :Callback<WeatherModel>{
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {
                    if (response.isSuccessful){
                        binding.progressBar.visibility=View.GONE
                        binding.scrollable.visibility = View.VISIBLE
                        response.body()?.let {
                            setData(it)
                        }
                    }
                    else{
                        Toast.makeText(this@MainActivity, "Location can not be found try again", Toast.LENGTH_LONG).show()
                        binding.scrollable.visibility = View.GONE
                        binding.progressBar.visibility = View.VISIBLE
                        binding.mm.visibility = View.GONE
                    }
                }
                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {
                    binding.scrollable.visibility = View.VISIBLE
                    binding.mm.visibility = View.VISIBLE
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        )
    }

        private fun fetchCurrentLocationWeather(latitude:String,longitude:String){
            ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude,longitude,apiKet)
                ?.enqueue(object :Callback<WeatherModel>{
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun onResponse(
                        call: Call<WeatherModel>,
                        response: Response<WeatherModel>
                    ) {
                        if(response.isSuccessful){
                            binding.scrollable.visibility = View.VISIBLE
                            binding.progressBar.visibility=View.GONE
                            response.body()?.let {
                                setData(it)
                            }
                        }
                    }
                    override fun onFailure(call: Call<WeatherModel>, t: Throwable) {
                    }
                })
        }
    private fun getCurrentLocation(){
        if (checkPermissions()){
            if (isLocationEnabled()){
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )!=PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )!=PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission()
                    return
                }
                fusedLocationProvider.lastLocation
                    .addOnSuccessListener { location->
                        if (location!= null){
                            currentLocation = location
                            binding.progressBar.visibility = View.VISIBLE
                            fetchCurrentLocationWeather(
                                location.latitude.toString(),
                                location.longitude.toString()
                            )
                        }
                    }
            }
            else{
                val intent=Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        else{
            requestPermission()
        }
    }
    private fun requestPermission(){
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_REQUEST_CODE
        )
    }
    private fun isLocationEnabled():Boolean{
        val locationManager:LocationManager=getSystemService(Context.LOCATION_SERVICE)
        as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    private fun checkPermissions():Boolean{
        if(ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )==PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        )==PackageManager.PERMISSION_GRANTED){
            return true
        }
        return false
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode==LOCATION_REQUEST_CODE){
            if (grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                getCurrentLocation()
            }else{

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setData(body:WeatherModel){
        binding.apply {
            val currentDate= SimpleDateFormat("dd/MM/yyyy hh:mm").format(Date())
            dateTime.text = currentDate.toString()
            maxTemp.text = "Max "+k2c(body?.main?.temp_max!!)+"°C"
            minTemp.text="Min "+k2c(body?.main?.temp_min!!)+"°C"
            temp.text=""+k2c(body?.main?.temp!!)+"°C"
            weatherTitle.text=body.weather[0].main
            sunriseValue.text=ts2td(body.sys.sunrise.toLong())
            sunsetValue.text=ts2td(body.sys.sunset.toLong())
            pressureValue.text=body.main.pressure.toString()+" hPa"
            humidityValue.text=body.main.humidity.toString()+"%"
            tempFValue.text=""+k2c(body.main.temp).times(1.8).plus(32).roundToInt()+""
            citySearch.setText(body.name)
            feelsLike.text="feel's like "+k2c(body?.main?.feels_like!!)+"°C"
            windValue.text=body.wind.speed.toString()+" m/s"
            groundValue.text=body.main.grnd_level.toString()+" hPa"
            seaValue.text=body.main.sea_level.toString()+" hPa"
            countryValue.text=body.sys.country
            city.text = body.name
        }
        updateUI(body.weather[0].id)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    private fun updateUI(id: Int) {
        binding.apply {
            when(id){
                in 200..232->{
                    weatherImage.setImageResource(R.drawable.elevend)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.clouds_bg)
                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.clouds_bg)
                }

                in 300..321->{
                    weatherImage.setImageResource(R.drawable.ninedn)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.drizzle_bg)
                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.drizzle_bg)
                }
                in 500..504->{
                    weatherImage.setImageResource(R.drawable.tend)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.drizzle_bg)
                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.drizzle_bg)
                }
                 511->{
                    weatherImage.setImageResource(R.drawable.thirteend)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.drizzle_bg)
                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.drizzle_bg)
                }
                in 520..531->{
                weatherImage.setImageResource(R.drawable.ninedn)
                mainLayout.background=ContextCompat
                    .getDrawable(this@MainActivity,R.drawable.drizzle_bg)
                optionsLayout.background=ContextCompat
                    .getDrawable(this@MainActivity,R.drawable.drizzle_bg)
            }

                in 600..622->{
                    weatherImage.setImageResource(R.drawable.thirteend)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.snow_bg)
                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.snow_bg)
                }

                in 701..781->{
                    weatherImage.setImageResource(R.drawable.fiftydn)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.atmosphere_bg)
                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.atmosphere_bg)
                }

                800->{
                    weatherImage.setImageResource(R.drawable.oned)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.clear_bg)
                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.clear_bg)
                }
                800->{
                    weatherImage.setImageResource(R.drawable.oned)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.clear_bg)
                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.clear_bg)
                }

                801->{
                    weatherImage.setImageResource(R.drawable.twod)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.snow_bg)
                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.snow_bg)
                }

                802->{
                    weatherImage.setImageResource(R.drawable.threedn)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.atmosphere_bg)
                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.atmosphere_bg)
                }

                803->{
                    weatherImage.setImageResource(R.drawable.fourdn)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.clouds_bg)
                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.clouds_bg)
                }

                804->{
                    weatherImage.setImageResource(R.drawable.fourdn)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.clouds_bg)
                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.clouds_bg)
                }
                else->{
                    weatherImage.setImageResource(R.drawable.ic_unknown)
                    mainLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.unknown_bg)
                    optionsLayout.background=ContextCompat
                        .getDrawable(this@MainActivity,R.drawable.unknown_bg)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ts2td(ts: Long): String {
        val localTime=ts.let {
            Instant.ofEpochSecond(it)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
        }
        return localTime.toString()
    }
    private fun k2c(t: Double): Double {
        var intTemp=t
        intTemp=intTemp.minus(273)
        return intTemp.toBigDecimal().setScale(1,RoundingMode.UP).toDouble()
    }
}

