package com.gregsite.audiblealtimeter

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.gregsite.audiblealtimeter.ui.theme.AudibleAltimeterTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Timer
import java.util.TimerTask

class MainActivity : ComponentActivity() {
    private var calibrationAlt = 0f; //current calibration altitude

    private var usingFeet = true; //true for feet, false for meters

    private var delayTime = 60f; //time to wait before announcing altitude again
    private var precision = 10; //precision to which altitude is rounded before being announced

    private var locationClient: FusedLocationProviderClient? = null;

    private var gpsAlt = 0f; //raw altitude from GPS data

    var gpsAltDisplay by mutableStateOf("0"); //data to be displayed by the ui

    private var setupCompleted = false;

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

    //set altimeter calibration to MSL
    fun setCalibrationMSL() {
        this.calibrationAlt = 0f;
    }

    //set altimeter calibration to current location
    fun setCalibrationCurrent() {
        this.calibrationAlt = this.gpsAlt;
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

    fun getRoundedUnitCalibratedAlt(): Int {
        return Math.round(this.getUnitCalibratedAlt() / this.precision) * this.precision;
    }

    fun getUnitCalibratedAlt(): Float {
        if (this.usingFeet) {
            return (convertToFt(this.gpsAlt - this.calibrationAlt));
        } else {
            return (this.gpsAlt - this.calibrationAlt);
        }
    }

    private fun createGPSUpdateTask(mainActivity: MainActivity) = object : TimerTask() {
        override fun run() {
            mainActivity.updateCurrentGPSAlt();
        }
    }

    //updates the current gps altitude variable, async
    fun updateCurrentGPSAlt() {
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
            this.locationClient!!.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        if (location.hasAltitude()) {
                            //set the gps altitude
                            this.gpsAlt = location.altitude.toFloat();

                            //update the display
                            this.gpsAltDisplay = Math.round(this.getUnitCalibratedAlt()).toString();
                        }
                    }
                }
        }

    }

    //runs the setup code, only ever runs once, to prevent starting a bunch of loops
    fun setup(){
        //see if has already been run
        if (setupCompleted){
            return;
        }

        //to stop it from running next time
        setupCompleted = true;

        this.locationClient = LocationServices.getFusedLocationProviderClient(this);

        //loop to continuously update the gps altitude
        Timer().schedule(createGPSUpdateTask(this), 0, 500)

        //TODO:loop to coninuously do the voice output

    }
}
//basic styling

    //modifiers
val outerGridModifier = Modifier.border(width= 2.dp, color = Color.Black).background(Color.LightGray);
val innerGridModifier = Modifier.border(width= 2.dp, color = Color.DarkGray).background(Color.LightGray);
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
        Text(gpsAltDisplay, textAlign = TextAlign.Center, modifier = outerGridModifier.fillMaxHeight(0.5f).fillMaxWidth().wrapContentHeight(), fontSize  = 96.sp)
        //measurement settings
        Row(modifier = outerGridModifier.fillMaxHeight(0.5f).fillMaxWidth().background(Color.Green)){
            //calibration
            Column(modifier = innerGridModifier.fillMaxHeight().fillMaxWidth(0.5f)){
                //header
                Text("Set zero point", textAlign = TextAlign.Center, modifier = innerGridModifier.fillMaxWidth())

                //to show which button is selected (adjusts the border width to give selected button a border)
                var wgs84Selected by remember { mutableStateOf(2.dp) }
                var currentSelected by remember { mutableStateOf(0.dp) }

                Button(modifier = buttonModifier.fillMaxHeight(0.5f).fillMaxWidth(), shape = buttonShape, onClick = {
                    mainActivity.setCalibrationMSL();
                    wgs84Selected = 2.dp;
                    currentSelected = 0.dp;
                                                                                                                    }, border = BorderStroke(wgs84Selected, selectedColor)){
                    Text("Calibrate to WGS84 ellipsoid")
                }
                Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(), shape = buttonShape, onClick = {
                    mainActivity.setCalibrationCurrent();
                    wgs84Selected = 0.dp;
                    currentSelected = 2.dp;
                                                                                                                }, border = BorderStroke(currentSelected, selectedColor)){
                    Text("Calibrate to current altitude")
                }
            }
            //units
            Row(modifier = innerGridModifier.fillMaxHeight().fillMaxWidth()){
                Column(modifier = Modifier.fillMaxHeight().fillMaxWidth()){
                    //header
                    Text("Select units", textAlign = TextAlign.Center, modifier = innerGridModifier.fillMaxWidth())
                    //choices
                    Column (modifier = Modifier.fillMaxHeight().fillMaxWidth()){
                        //to show which button is selected (adjusts the border width to give selected button a border)
                        var feetSelected by remember { mutableStateOf(2.dp) }
                        var metersSelected by remember { mutableStateOf(0.dp) }

                        Button(modifier = buttonModifier.fillMaxHeight(0.5f).fillMaxWidth(), shape = buttonShape, onClick = {
                            mainActivity.setUsingFeet(true);
                            feetSelected = 2.dp;
                            metersSelected = 0.dp;
                                                                                                                            }, border = BorderStroke(feetSelected, selectedColor)){
                            Text("Feet")
                        }
                        Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(), shape = buttonShape, onClick = {
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
        Column(modifier = outerGridModifier.fillMaxHeight().fillMaxWidth().background(Color.Blue)){
            //header
            Text("Voice settings", textAlign = TextAlign.Center, modifier = innerGridModifier.fillMaxWidth())
            //two sections
            Row (modifier = innerGridModifier.fillMaxHeight().fillMaxWidth()){
                //precision
                Column (innerGridModifier.fillMaxHeight().fillMaxWidth(0.5f)) {
                    //header (height is one quarter of the voice area's height, but we need to account for the earlier header)
                    Text("Precision of announcements", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

                    //buttons
                    Row(modifier = innerGridModifier.fillMaxHeight().fillMaxWidth()){
                        var selected1 by remember { mutableStateOf(0.dp) }
                        var selected5 by remember { mutableStateOf(0.dp) }
                        var selected10 by remember { mutableStateOf(2.dp) }
                        var selected100 by remember { mutableStateOf(0.dp) }

                        Column(modifier = innerGridModifier.fillMaxHeight().fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth()) {
                                Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(0.5f), shape = buttonShape, onClick = {
                                    mainActivity.setAnnouncementPrecision(1);
                                    selected1 = 2.dp;
                                    selected5 = 0.dp;
                                    selected10 = 0.dp;
                                    selected100 = 0.dp;
                                }, border = BorderStroke(selected1, selectedColor)) {
                                    Text("1")
                                }
                                Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(), shape = buttonShape, onClick = {
                                    mainActivity.setAnnouncementPrecision(5);
                                    selected1 = 0.dp;
                                    selected5 = 2.dp;
                                    selected10 = 0.dp;
                                    selected100 = 0.dp;
                                }, border = BorderStroke(selected5, selectedColor)) {
                                    Text("5")
                                }
                            }
                            Row(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
                                Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(0.5f), shape = buttonShape, onClick = {
                                    mainActivity.setAnnouncementPrecision(10);
                                    selected1 = 0.dp;
                                    selected5 = 0.dp;
                                    selected10 = 2.dp;
                                    selected100 = 0.dp; }, border = BorderStroke(selected10, selectedColor)) {
                                    Text("10")
                                }
                                Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(), shape = buttonShape, onClick = {
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
                Column (innerGridModifier.fillMaxHeight().fillMaxWidth()) {
                    //header (height is one quarter of the voice area's height, but we need to account for the earlier header)
                    Text("Delay between announcements", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    var selected0 by remember { mutableStateOf(0.dp) }
                    var selected5 by remember { mutableStateOf(0.dp) }
                    var selected15 by remember { mutableStateOf(0.dp) }
                    var selected60 by remember { mutableStateOf(2.dp) }
                    //buttons
                    Column(modifier = innerGridModifier.fillMaxHeight().fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth()) {
                            Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(0.5f), shape = buttonShape, onClick = {
                                mainActivity.setAnnouncementDelay(0f);
                                selected0 = 2.dp;
                                selected5 = 0.dp;
                                selected15 = 0.dp;
                                selected60 = 0.dp;
                            }, border = BorderStroke(selected0, selectedColor)) {
                                Text("0s")
                            }
                            Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(), shape = buttonShape, onClick = {
                                mainActivity.setAnnouncementDelay(5f);
                                selected0 = 0.dp;
                                selected5 = 2.dp;
                                selected15 = 0.dp;
                                selected60 = 0.dp;
                            }, border = BorderStroke(selected5, selectedColor)) {
                                Text("5s")
                            }
                        }
                        Row(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
                            Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(0.5f), shape = buttonShape, onClick = {
                                mainActivity.setAnnouncementDelay(15f);
                                selected0 = 0.dp;
                                selected5 = 0.dp;
                                selected15 = 2.dp;
                                selected60 = 0.dp;
                            }, border = BorderStroke(selected15, selectedColor)) {
                                Text("15s")
                            }
                            Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(), shape = buttonShape, onClick = {
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