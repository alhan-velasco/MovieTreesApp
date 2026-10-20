package com.alhan.movietreesapp.features.movies.domain.model

data class Movie(
    val id: Long,
    val title: String,
    val popularity: Double,
    val voteAverage: Double,
    val voteCount: Int,
    val releaseDate: String
) {
    /** Clave de ordenamiento/búsqueda: título normalizado (sin mayúsculas ni espacios extremos). */
    val titleKey: String = normalize(title)

    companion object {
        fun normalize(title: String): String = title.trim().lowercase()

        /** Orden total: por título y, si hay títulos repetidos (remakes), por id. */
        fun compare(a: Movie, b: Movie): Int {
            val c = a.titleKey.compareTo(b.titleKey)
            return if (c != 0) c else a.id.compareTo(b.id)
        }

        val ORDER: Comparator<Movie> = Comparator { a, b -> compare(a, b) }
    }
}
