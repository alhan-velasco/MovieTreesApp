package com.alhan.movietreesapp.core.di

import android.content.Context
import com.alhan.movietreesapp.core.data.CsvDataSource
import com.alhan.movietreesapp.features.movies.data.MovieRepositoryImpl
import com.alhan.movietreesapp.features.movies.domain.repository.MovieRepository

class AppContainer(context: Context) {
    private val csvDataSource = CsvDataSource(context)
    val movieRepository: MovieRepository = MovieRepositoryImpl(csvDataSource)
}
