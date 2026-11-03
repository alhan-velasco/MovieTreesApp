package com.alhan.movietreesapp.features.movies.domain.model

data class Movie(
    val id: Long,
    val title: String,
    val popularity: Double,
    val voteAverage: Double,
    val voteCount: Int,
    val releaseDate: String
) {
    val titleKey: String = normalize(title)

    companion object {
        fun normalize(title: String): String = title.trim().lowercase()
    }
}
