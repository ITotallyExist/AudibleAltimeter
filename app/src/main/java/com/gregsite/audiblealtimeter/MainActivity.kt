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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudibleAltimeterTheme {
                MainLayout();
            }
        }
    }
}

@Preview(showBackground = false)
@Composable
fun MainLayout() {
    Column{
        Box(modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth().background(Color.Red)){

        }
        Row(modifier = Modifier.fillMaxHeight(0.5f).fillMaxWidth().background(Color.Green)){

        }
        Row(modifier = Modifier.fillMaxHeight().fillMaxWidth().background(Color.Blue)){

        }
    }
}

//@Preview(showBackground = false)
//@Composable
//fun GreetingPreview() {
//    AudibleAltimeterTheme {
//        Greeting("Greg")
//    }
//}