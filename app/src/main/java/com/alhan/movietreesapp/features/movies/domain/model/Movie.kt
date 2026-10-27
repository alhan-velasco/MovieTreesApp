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

        fun compare(a: Movie, b: Movie): Int {
            val c = a.titleKey.compareTo(b.titleKey)
            return if (c != 0) c else a.id.compareTo(b.id)
        }

        val ORDER: Comparator<Movie> = Comparator { a, b -> compare(a, b) }
    }
}
