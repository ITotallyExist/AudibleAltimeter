package com.gregsite.audiblealtimeter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.gregsite.audiblealtimeter.ui.theme.AudibleAltimeterTheme


class MainActivity : ComponentActivity() {
    private var zeroAlt = 0f; //current zero altitude

    private var usingFeet = true; //true for feet, false for meters

    private var delayTime = 5f; //time to wait before announcing altitude again
    private var precision = 10; //precision to which altitude is rounded before being announced

    //get location through android's fused do it for you approach
    private var locationClient: FusedLocationProviderClient? = null;
    val fusedLocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500)
        .setGranularity(Granularity.GRANULARITY_FINE)
        .setMinUpdateIntervalMillis(500) // Fastest interval for updates
        .build()

    //get location directly from GPS
    private var locationManager: LocationManager? = null

    private var gpsAlt = 0f; //raw altitude from GPS data
    private var fusedAlt = 0f; //raw altitude from fused location data

    var gpsAltDisplay by mutableStateOf("0"); //data to be displayed by the ui

    //to set up the app on startup
    private var setupCompleted = false;

    //to do the text to speech loop
    private val ttsHandler = Handler(Looper.getMainLooper())
    private val ttsRunnable = Runnable { doTTSSpeak() }

    //do the actual text to speech
    private var ttsInitialised = false;
    private var ttsSpeaker: TextToSpeech? = null;

    //thing that keeps this app awake
    var wakeLock: WakeLock? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //hide navigation bars
        fullscreenMode(window)

        //set variable to dynamically update the displayed altitude


        setContent {
            AudibleAltimeterTheme {
                MainLayout(this, gpsAltDisplay);
            }
        }

        this.setup();
    }

    //set altimeter zero to MSL
    fun setZeroMSL() {
        this.zeroAlt = 0f;
    }

    //set altimeter zero to current location
    fun setZeroCurrent() {
        this.zeroAlt = this.fusedAlt;
    }

    //set units to feet (true) or meters (false)
    fun setUsingFeet(valueToSet: Boolean) {
        this.usingFeet = valueToSet;
    }

    //returns true if using feet
    fun isUsingFeet(): Boolean {
        return this.usingFeet;
    }

    //set the precision of the announcements
    fun setAnnouncementPrecision(roundTo: Int) {
        this.precision = roundTo;
    }

    //sets the delay between announcements
    fun setAnnouncementDelay(delay: Float) {
        this.delayTime = delay;
    }

    fun getRoundedUnitCalibratedAlt(useFused: Boolean): Int {
        return Math.round(this.getUnitCalibratedAlt(useFused) / this.precision) * this.precision;
    }

    fun getUnitCalibratedAlt(useFused: Boolean): Float {

        var alt = this.gpsAlt;

        if (useFused){
            alt = this.fusedAlt
        }

        if (this.usingFeet) {
            return (convertToFt(alt - this.zeroAlt));
        } else {
            return (alt - this.zeroAlt);
        }
    }

    //updates the current fused altitude variable, async
    fun setupFusedLocLoop() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
                99
            );

            return
        }
        if (this.locationClient != null) {
            this.locationClient!!.requestLocationUpdates(fusedLocationRequest, updateFusedAltCallback, Looper.getMainLooper())
        }

    }

    //callback that sets fused altitude variable and altitude that is on display
    val updateFusedAltCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            if (location != null && location.hasAltitude()) {
                //update altitude
                fusedAlt = location.altitude.toFloat()

                //update display altitude
                gpsAltDisplay = Math.round(getUnitCalibratedAlt(true)).toString();
            }
        }
    }

    //callback that responds to updates from gps provider
    private val gpsLocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (location.hasAltitude()) {
                //update gps altitude
                gpsAlt = location.altitude.toFloat()

                Log.d("pureGPSAlt",gpsAlt.toString())

                //update display altitude
                gpsAltDisplay = Math.round(getUnitCalibratedAlt(false)).toString();
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun setupGPSLoop() {
        //set the location manager (needs to be here so that it happens after oncreate
        this.locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Check for location permissions before requesting updates
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
                99
            );

            return
        }

        //request updates from the gps provider directly
        this.locationManager!!.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            500L,  // Time interval in milliseconds
            0f,  // Minimum distance in meters for updates
            gpsLocationListener
        )


        Log.d("i","here")
    }

    fun stopGPSUpdates() {
        if (locationManager != null) {
            locationManager!!.removeUpdates(gpsLocationListener)
        }
    }

    fun doTTSSpeak(){
        if (ttsInitialised){
            //if initialised
            if (ttsSpeaker != null) {
                ttsSpeaker!!.speak(
                    getRoundedUnitCalibratedAlt(true).toString(),
                    TextToSpeech.QUEUE_FLUSH,
                    null
                );

                Log.d("tts", "loop into")
            }
        }

        ttsHandler.postDelayed((ttsRunnable),(Math.round(delayTime)*1000).toLong());

    }

    //runs the setup code, only ever runs once, to prevent starting a bunch of loops
    fun setup(){
        //see if has already been run
        if (this.setupCompleted){
            return;
        }

        //to stop it from running next time
        this.setupCompleted = true;

        this.locationClient = LocationServices.getFusedLocationProviderClient(this);

        //loop to continuously update the gps altitude
        this.setupFusedLocLoop()

        //initialise tts
        ttsSpeaker = TextToSpeech(this, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInitialised = true

                ttsSpeaker!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onDone(utteranceId: String) {
                        Log.d("tts","tts done");
                        //call this again to do tts after the delay
                        ttsHandler.postDelayed((ttsRunnable),(Math.round(delayTime)*1000).toLong());
                    }

                    override fun onError(utteranceId: String) {
                        Log.d("tts","tts error");
                    }

                    override fun onStart(utteranceId: String) {
                    }
                });
            } else {
                Log.e("TTS", "Init Failed")
            }
        });

        //loop to continuously do voice output
        doTTSSpeak();

        //to keep the app working if user turns off their screen
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag").apply {
                    acquire(10*60*1000L)
                }
            }
    }
}
//basic styling

    //modifiers
val outerGridModifier = Modifier
        .border(width = 2.dp, color = Color.Black)
        .background(Color.LightGray);
val innerGridModifier = Modifier
    .border(width = 2.dp, color = Color.DarkGray)
    .background(Color.LightGray);
val buttonModifier = Modifier.padding(6.dp)

    //shapes
val buttonShape = CutCornerShape(4.dp);

    //colors
val selectedColor = Color.Blue;

fun convertToFt(numberInMeters: Float): Float{
    return (numberInMeters*3.28084f);
}


fun fullscreenMode(window: Window) {
    //get a controller object for the window insets
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView);

    //pick the bars to hide (navigation but not notification in this case because notification isnt covering anything up)
    windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())

    //specify how user unhides the bars
    windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE;
}

//todo: call this every time things change (button press or altitude update
@Composable
fun MainLayout(mainActivity: MainActivity, gpsAltDisplay: String) {
    Column{
        //display
        //TODO: get this to constantly update with current altitude
        Text(gpsAltDisplay, textAlign = TextAlign.Center, modifier = outerGridModifier.background(Color.White)
            .fillMaxHeight(0.5f)
            .fillMaxWidth()
            .wrapContentHeight(), fontSize  = 150.sp)
        //measurement settings
        Row(modifier = outerGridModifier
            .fillMaxHeight(0.5f)
            .fillMaxWidth()
            .background(Color.Green)){
            //zero
            Column(modifier = innerGridModifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)){
                //header
                Text("Set zero point", textAlign = TextAlign.Center, modifier = innerGridModifier.fillMaxWidth())

                //to show which button is selected (adjusts the border width to give selected button a border)
                var wgs84Selected by remember { mutableStateOf(2.dp) }
                var currentSelected by remember { mutableStateOf(0.dp) }

                Button(modifier = buttonModifier
                    .fillMaxHeight(0.5f)
                    .fillMaxWidth(), shape = buttonShape, onClick = {
                    mainActivity.setZeroMSL();
                    wgs84Selected = 2.dp;
                    currentSelected = 0.dp;
                                                                                                                    }, border = BorderStroke(wgs84Selected, selectedColor)){
                    Text("Zero at WGS84 ellipsoid")
                }
                Button(modifier = buttonModifier
                    .fillMaxHeight()
                    .fillMaxWidth(), shape = buttonShape, onClick = {
                    mainActivity.setZeroCurrent();
                    wgs84Selected = 0.dp;
                    currentSelected = 2.dp;
                                                                                                                }, border = BorderStroke(currentSelected, selectedColor)){
                    Text("Zero at current altitude")
                }
            }
            //units
            Row(modifier = innerGridModifier
                .fillMaxHeight()
                .fillMaxWidth()){
                Column(modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()){
                    //header
                    Text("Select units", textAlign = TextAlign.Center, modifier = innerGridModifier.fillMaxWidth())
                    //choices
                    Column (modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()){
                        //to show which button is selected (adjusts the border width to give selected button a border)
                        var feetSelected by remember { mutableStateOf(2.dp) }
                        var metersSelected by remember { mutableStateOf(0.dp) }

                        Button(modifier = buttonModifier
                            .fillMaxHeight(0.5f)
                            .fillMaxWidth(), shape = buttonShape, onClick = {
                            mainActivity.setUsingFeet(true);
                            feetSelected = 2.dp;
                            metersSelected = 0.dp;
                                                                                                                            }, border = BorderStroke(feetSelected, selectedColor)){
                            Text("Feet")
                        }
                        Button(modifier = buttonModifier
                            .fillMaxHeight()
                            .fillMaxWidth(), shape = buttonShape, onClick = {
                            mainActivity.setUsingFeet(false);
                            feetSelected = 0.dp;
                            metersSelected = 2.dp;
                                                                                                                        }, border = BorderStroke(metersSelected, selectedColor)){
                            Text("Meters")
                        }
                    }

                }


            }
        }
        //voice settings
        Column(modifier = outerGridModifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(Color.Blue)){
            //header
            Text("Voice settings", textAlign = TextAlign.Center, modifier = innerGridModifier.fillMaxWidth())
            //two sections
            Row (modifier = innerGridModifier
                .fillMaxHeight()
                .fillMaxWidth()){
                //precision
                Column (
                    innerGridModifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.5f)) {
                    //header (height is one quarter of the voice area's height, but we need to account for the earlier header)
                    Text("Precision of announcements", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

                    //buttons
                    Row(modifier = innerGridModifier
                        .fillMaxHeight()
                        .fillMaxWidth()){
                        var selected1 by remember { mutableStateOf(0.dp) }
                        var selected5 by remember { mutableStateOf(0.dp) }
                        var selected10 by remember { mutableStateOf(2.dp) }
                        var selected100 by remember { mutableStateOf(0.dp) }

                        Column(modifier = innerGridModifier
                            .fillMaxHeight()
                            .fillMaxWidth()) {
                            Row(modifier = Modifier
                                .fillMaxHeight(0.5f)
                                .fillMaxWidth()) {
                                Button(modifier = buttonModifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.5f), shape = buttonShape, onClick = {
                                    mainActivity.setAnnouncementPrecision(1);
                                    selected1 = 2.dp;
                                    selected5 = 0.dp;
                                    selected10 = 0.dp;
                                    selected100 = 0.dp;
                                }, border = BorderStroke(selected1, selectedColor)) {
                                    Text("1")
                                }
                                Button(modifier = buttonModifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(), shape = buttonShape, onClick = {
                                    mainActivity.setAnnouncementPrecision(5);
                                    selected1 = 0.dp;
                                    selected5 = 2.dp;
                                    selected10 = 0.dp;
                                    selected100 = 0.dp;
                                }, border = BorderStroke(selected5, selectedColor)) {
                                    Text("5")
                                }
                            }
                            Row(modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth()) {
                                Button(modifier = buttonModifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.5f), shape = buttonShape, onClick = {
                                    mainActivity.setAnnouncementPrecision(10);
                                    selected1 = 0.dp;
                                    selected5 = 0.dp;
                                    selected10 = 2.dp;
                                    selected100 = 0.dp; }, border = BorderStroke(selected10, selectedColor)) {
                                    Text("10")
                                }
                                Button(modifier = buttonModifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(), shape = buttonShape, onClick = {
                                    mainActivity.setAnnouncementPrecision(100);
                                    selected1 = 0.dp;
                                    selected5 = 0.dp;
                                    selected10 = 0.dp;
                                    selected100 = 2.dp; }, border = BorderStroke(selected100, selectedColor)) {
                                    Text("100")
                                }
                            }
                        }
                    }

                }
                //delay
                Column (
                    innerGridModifier
                        .fillMaxHeight()
                        .fillMaxWidth()) {
                    //header (height is one quarter of the voice area's height, but we need to account for the earlier header)
                    Text("Delay between announcements", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    var selected0 by remember { mutableStateOf(0.dp) }
                    var selected5 by remember { mutableStateOf(2.dp) }
                    var selected15 by remember { mutableStateOf(0.dp) }
                    var selected60 by remember { mutableStateOf(0.dp) }
                    //buttons
                    Column(modifier = innerGridModifier
                        .fillMaxHeight()
                        .fillMaxWidth()) {
                        Row(modifier = Modifier
                            .fillMaxHeight(0.5f)
                            .fillMaxWidth()) {
                            Button(modifier = buttonModifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.5f), shape = buttonShape, onClick = {
                                mainActivity.setAnnouncementDelay(0f);
                                selected0 = 2.dp;
                                selected5 = 0.dp;
                                selected15 = 0.dp;
                                selected60 = 0.dp;
                            }, border = BorderStroke(selected0, selectedColor)) {
                                Text("0s")
                            }
                            Button(modifier = buttonModifier
                                .fillMaxHeight()
                                .fillMaxWidth(), shape = buttonShape, onClick = {
                                mainActivity.setAnnouncementDelay(5f);
                                selected0 = 0.dp;
                                selected5 = 2.dp;
                                selected15 = 0.dp;
                                selected60 = 0.dp;
                            }, border = BorderStroke(selected5, selectedColor)) {
                                Text("5s")
                            }
                        }
                        Row(modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth()) {
                            Button(modifier = buttonModifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.5f), shape = buttonShape, onClick = {
                                mainActivity.setAnnouncementDelay(15f);
                                selected0 = 0.dp;
                                selected5 = 0.dp;
                                selected15 = 2.dp;
                                selected60 = 0.dp;
                            }, border = BorderStroke(selected15, selectedColor)) {
                                Text("15s")
                            }
                            Button(modifier = buttonModifier
                                .fillMaxHeight()
                                .fillMaxWidth(), shape = buttonShape, onClick = {
                                mainActivity.setAnnouncementDelay(60f);
                                selected0 = 0.dp;
                                selected5 = 0.dp;
                                selected15 = 0.dp;
                                selected60 = 2.dp;
                            }, border = BorderStroke(selected60, selectedColor)) {
                                Text("60s")
                            }
                        }
                    }

                }
            }

        }
    }
}



@Preview(showBackground = false)
@Composable
fun LayoutPreview() {
    AudibleAltimeterTheme {
        MainLayout(MainActivity(), "i");
    }
}