package com.alhan.movietreesapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.alhan.movietreesapp.core.di.AppContainer
import com.alhan.movietreesapp.features.movies.presentation.MovieScreen
import com.alhan.movietreesapp.features.movies.presentation.MovieViewModel
import com.alhan.movietreesapp.ui.theme.MovieTreesAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = AppContainer(applicationContext)
        val viewModel = MovieViewModel(appContainer.movieRepository)

        setContent {
            MovieTreesAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        MovieScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
