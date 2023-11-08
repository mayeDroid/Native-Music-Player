package com.example.myaudioplayer

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myaudioplayer.ui.audio.AudioViewModel
import com.example.myaudioplayer.ui.audio.HomeScreen
import com.example.myaudioplayer.ui.theme.MyAudioPlayerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyAudioPlayerTheme {

                val permissionState = rememberPermissionState(permission = android.Manifest.permission.READ_EXTERNAL_STORAGE)

                val lifeCycleOwner = LocalLifecycleOwner.current

                DisposableEffect(key1 = lifeCycleOwner){
                    val observer = LifecycleEventObserver{
                            _, event ->
                        if (event == Lifecycle.Event.ON_RESUME){
                            permissionState.launchPermissionRequest()

                        }

                    }
                    lifeCycleOwner.lifecycle.addObserver(observer)

                    // this will help to remove the permission request when we navigate to another page
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

                        val audioLists = audioViewModel.audioList

                        HomeScreen(
                            progress = audioViewModel.currentAudioProgress.value,
                            onProgressChange = {
                                               audioViewModel.seekTo(it)
                            },
                            isAudioPlaying = audioViewModel.isAudioPlaying,
                            audio = audioLists,
                            currentPlayingAudio = audioViewModel.currentPlayingAudio.value,
                            onStart = {
                                      audioViewModel.playAudio(it)
                            },
                            onItemClick = {
                                audioViewModel.playAudio(it)
                            },

                            onNext = { audioViewModel.skipToNext() }
                        )

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