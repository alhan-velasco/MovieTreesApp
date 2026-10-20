package com.alhan.movietreesapp.features.movies.domain.tree

import com.alhan.movietreesapp.features.movies.domain.model.Movie

class MovieNode(
    val movie: Movie,
    var left: MovieNode? = null,
    var right: MovieNode? = null
)
