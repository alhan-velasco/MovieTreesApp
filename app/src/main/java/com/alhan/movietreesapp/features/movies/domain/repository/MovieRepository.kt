package com.alhan.movietreesapp.features.movies.domain.repository

import com.alhan.movietreesapp.features.movies.domain.model.Movie
import com.alhan.movietreesapp.features.movies.domain.model.TreeMode
import com.alhan.movietreesapp.features.movies.domain.tree.MovieTree

interface MovieRepository {
    fun loadMoviesFromCsv(): List<Movie>
    fun buildTree(movies: List<Movie>, mode: TreeMode): MovieTree
    fun linearSearch(movies: List<Movie>, title: String): Pair<Movie?, Int>
    fun linearInsertOrdered(movies: MutableList<Movie>, movie: Movie): Int
    fun linearDelete(movies: MutableList<Movie>, title: String): Boolean
}
