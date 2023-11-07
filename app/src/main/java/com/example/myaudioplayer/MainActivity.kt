package com.example.myaudioplayer

import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myaudioplayer.ui.audio.AudioViewModel
import com.example.myaudioplayer.ui.theme.MyAudioPlayerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyAudioPlayerTheme {

                val permissionState = rememberPermissionState(permission = android.Manifest.permission.MANAGE_EXTERNAL_STORAGE)

                val lifeCycleOwner = LocalLifecycleOwner.current

                DisposableEffect(key1 = lifeCycleOwner){

                    val observer = LifecycleEventObserver{
                            _, event ->
                        if (event == Lifecycle.Event.ON_RESUME){
                            permissionState.launchPermissionRequest()

                        }

                    }
                    lifeCycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        lifeCycleOwner.lifecycle.removeObserver(observer)

                    }
                }

                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (permissionState.status.isGranted){
                        val audioViewModel = viewModel(modelClass = AudioViewModel::class.java)

                        val audioList = audioViewModel.audioList

                    }
                    else{
                        Box (contentAlignment = Alignment.Center) {
                            Text(text = "Grant permission first")

                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyAudioPlayerTheme {
        Greeting("Android")
    }
}