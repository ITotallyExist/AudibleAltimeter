package com.gregsite.audiblealtimeter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gregsite.audiblealtimeter.ui.theme.AudibleAltimeterTheme

class MainActivity : ComponentActivity() {
    private var calibration = 0; //current calibration altitude

    private var usingFeet = true; //true for feet, false for meters

    private var delayTime = 60f; //time to wait before announcing altitude again
    private var precision = 10; //precision to which altitude is rounded before being announced

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudibleAltimeterTheme {
                MainLayout(this);
            }
        }
    }

    //set altimeter calibration to MSL
    fun setCalibrationMSL(){
        this.calibration = 0;
    }

    //set altimeter calibration to current location
    fun setCalibrationCurrent(){
        this.calibration = getCurrentAlt();
    }

    //set units to feet (true) or meters (false)
    fun setUsingFeet(valueToSet: Boolean){
        this.usingFeet = valueToSet;
    }

    //returns true if using feet
    fun isUsingFeet(): Boolean {
        return this.usingFeet;
    }

    //set the precision of the announcements
    fun setAnnouncementPrecision(roundTo: Int){
        this.precision = roundTo;
    }

    //sets the delay between announcements
    fun setAnnouncementDelay(delay: Float){
        this.delayTime = delay;
    }
}

fun convertToFt(numberInMeters: Float): Float{
    return (numberInMeters*3.28084f);
}

fun getCurrentAlt(): Int {
    return (5);
}

@Composable
fun MainLayout(mainActivity: MainActivity) {
    Column{
        //display
        Box(modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth().background(Color.Red)){
            Text("5")
        }
        //measurement settings
        Row(modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth().background(Color.Green)){
            //calibration
            Column(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.75f)){
                Button(modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth(), onClick = {mainActivity.setCalibrationMSL()}){
                    Text("Set zero to MSL")
                }
                Button(modifier = Modifier.fillMaxHeight().fillMaxWidth(), onClick = {mainActivity.setCalibrationCurrent()}){
                    Text("Set zero to current altitude")
                }
            }
            //units
            Row(modifier = Modifier.fillMaxHeight().fillMaxWidth()){
                Column(modifier = Modifier.fillMaxHeight().fillMaxWidth()){
                    //header
                    Box(modifier = Modifier.fillMaxHeight(0.33333f).fillMaxWidth()){
                        Text("Select Units")
                    }
                    //choices
                    Column (modifier = Modifier.fillMaxHeight().fillMaxWidth()){
                        Button(modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth(), onClick = {mainActivity.setUsingFeet(true)}){
                            Text("Feet")
                        }
                        Button(modifier = Modifier.fillMaxHeight().fillMaxWidth(), onClick = {mainActivity.setUsingFeet(false)}){
                            Text("Meters")
                        }
                    }

                }


            }
        }
        //voice settings
        Column(modifier = Modifier.fillMaxHeight().fillMaxWidth().background(Color.Blue)){
            //header
            Box(modifier = Modifier.fillMaxHeight(0.25f).fillMaxWidth()){
                Text("Voice settings")
            }
            //two sections
            Row (modifier = Modifier.fillMaxHeight().fillMaxWidth()){
                //precision
                Column (Modifier.fillMaxHeight().fillMaxWidth(0.5f)) {
                    //header (height is one quarter of the voice area's height, but we need to account for the earlier header)
                    Box(modifier = Modifier.fillMaxHeight(0.33333333333f).fillMaxWidth()){
                        Text("Precision of announcements")
                    }

                    //buttons
                    Row(modifier = Modifier.fillMaxHeight().fillMaxWidth()){
                        Button(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.3333333333f), onClick = {mainActivity.setAnnouncementPrecision(1)}){
                            Text("1")
                        }
                        Button(modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f), onClick = {mainActivity.setAnnouncementPrecision(10)}){
                            Text("10")
                        }
                        Button(modifier = Modifier.fillMaxHeight().fillMaxWidth(), onClick = {mainActivity.setAnnouncementPrecision(100)}){
                            Text("100")
                        }
                    }

                }
                //delay
                Column (Modifier.fillMaxHeight().fillMaxWidth()) {
                    //header (height is one quarter of the voice area's height, but we need to account for the earlier header)
                    Box(modifier = Modifier.fillMaxHeight(0.33333333333f).fillMaxWidth()){
                        Text("Delay between announcements")
                    }

                    //buttons
                    Column(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth()) {
                            Button(
                                modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f),
                                onClick = { mainActivity.setAnnouncementDelay(0f) }) {
                                Text("0s")
                            }
                            Button(
                                modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                                onClick = { mainActivity.setAnnouncementDelay(5f) }) {
                                Text("5s")
                            }
                        }
                        Row(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
                            Button(
                                modifier = Modifier.fillMaxHeight().fillMaxWidth(0.5f),
                                onClick = { mainActivity.setAnnouncementDelay(15f) }) {
                                Text("15s")
                            }
                            Button(
                                modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                                onClick = { mainActivity.setAnnouncementDelay(60f) }) {
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
        MainLayout(MainActivity());
    }
}