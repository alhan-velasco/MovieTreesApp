package com.alhan.movietreesapp.features.movies.data

import com.alhan.movietreesapp.core.data.CsvDataSource
import com.alhan.movietreesapp.features.movies.domain.model.Movie
import com.alhan.movietreesapp.features.movies.domain.model.TreeMode
import com.alhan.movietreesapp.features.movies.domain.repository.MovieRepository
import com.alhan.movietreesapp.features.movies.domain.tree.MovieTree

class MovieRepositoryImpl(
    private val csvDataSource: CsvDataSource
) : MovieRepository {

    override fun loadMoviesFromCsv(): List<Movie> {
        return csvDataSource.readMovies()
    }

    override fun buildTree(movies: List<Movie>, mode: TreeMode): MovieTree {
        val tree = MovieTree(mode)
        for (movie in movies) {
            tree.insert(movie)
        }
        return tree
    }

    override fun linearSearch(movies: List<Movie>, title: String): Pair<Movie?, Int> {
        val key = Movie.normalize(title)
        var steps = 0
        for (i in 0 until movies.size) {
            steps++
            val movie = movies[i]
            if (movie.titleKey.equals(key)) {
                return Pair(movie, steps)
            }
        }
        return Pair(null, steps)
    }

    override fun linearInsert(movies: MutableList<Movie>, movie: Movie): Int {
        var steps = 0
        for (i in 0 until movies.size) {
            steps++
        }
        movies.add(movie)
        return steps + 1
    }

    override fun linearDelete(movies: MutableList<Movie>, title: String): Pair<Boolean, Int> {
        val key = Movie.normalize(title)
        var steps = 0
        for (i in 0 until movies.size) {
            steps++
            if (movies[i].titleKey.equals(key)) {
                movies.removeAt(i)
                return Pair(true, steps)
            }
        }
        return Pair(false, steps)
    }
}
