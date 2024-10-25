package com.gregsite.audiblealtimeter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

//basic styling

    //modifiers
val outerGridModifier = Modifier.border(width= 2.dp, color = Color.Black).background(Color.LightGray);
val innerGridModifier = Modifier.border(width= 2.dp, color = Color.DarkGray).background(Color.LightGray);
val buttonModifier = Modifier.padding(4.dp)

    //shapes
val buttonShape = CutCornerShape(4.dp);

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
        Text("5", textAlign = TextAlign.Center, modifier = outerGridModifier.fillMaxHeight(0.5f).fillMaxWidth().wrapContentHeight(), fontSize  = 96.sp)
        //measurement settings
        Row(modifier = outerGridModifier.fillMaxHeight(0.5f).fillMaxWidth().background(Color.Green)){
            //calibration
            Column(modifier = innerGridModifier.fillMaxHeight().fillMaxWidth(0.5f)){
                //header
                Text("Set zero point", textAlign = TextAlign.Center, modifier = innerGridModifier.fillMaxWidth())

                Button(modifier = buttonModifier.fillMaxHeight(0.5f).fillMaxWidth(), shape = buttonShape, onClick = {mainActivity.setCalibrationMSL()}){
                    Text("Calibrate to MSL")
                }
                Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(), shape = buttonShape, onClick = {mainActivity.setCalibrationCurrent()}){
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
                        Button(modifier = buttonModifier.fillMaxHeight(0.5f).fillMaxWidth(), shape = buttonShape, onClick = {mainActivity.setUsingFeet(true)}){
                            Text("Feet")
                        }
                        Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(), shape = buttonShape, onClick = {mainActivity.setUsingFeet(false)}){
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
                        Column(modifier = innerGridModifier.fillMaxHeight().fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth()) {
                                Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(0.5f), shape = buttonShape, onClick = { mainActivity.setAnnouncementPrecision(1) }) {
                                    Text("1")
                                }
                                Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(), shape = buttonShape, onClick = { mainActivity.setAnnouncementPrecision(5) }) {
                                    Text("5")
                                }
                            }
                            Row(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
                                Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(0.5f), shape = buttonShape, onClick = { mainActivity.setAnnouncementPrecision(10) }) {
                                    Text("10")
                                }
                                Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(), shape = buttonShape, onClick = { mainActivity.setAnnouncementPrecision(100) }) {
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

                    //buttons
                    Column(modifier = innerGridModifier.fillMaxHeight().fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth()) {
                            Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(0.5f), shape = buttonShape, onClick = { mainActivity.setAnnouncementDelay(0f) }) {
                                Text("0s")
                            }
                            Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(), shape = buttonShape, onClick = { mainActivity.setAnnouncementDelay(5f) }) {
                                Text("5s")
                            }
                        }
                        Row(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
                            Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(0.5f), shape = buttonShape, onClick = { mainActivity.setAnnouncementDelay(15f) }) {
                                Text("15s")
                            }
                            Button(modifier = buttonModifier.fillMaxHeight().fillMaxWidth(), shape = buttonShape, onClick = { mainActivity.setAnnouncementDelay(60f) }) {
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