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
        movies.forEach { tree.insert(it) }
        return tree
    }

    override fun linearSearch(movies: List<Movie>, title: String): Pair<Movie?, Int> {
        val key = Movie.normalize(title)
        var steps = 0
        for (movie in movies) {
            steps++
            if (movie.titleKey == key) return Pair(movie, steps)
        }
        return Pair(null, steps)
    }

    override fun linearInsertOrdered(movies: MutableList<Movie>, movie: Movie): Int {
        var steps = 0
        var insertIndex = movies.size
        for (i in movies.indices) {
            steps++
            if (Movie.compare(movie, movies[i]) < 0) {
                insertIndex = i
                break
            }
        }
        movies.add(insertIndex, movie)
        return steps
    }

    override fun linearDelete(movies: MutableList<Movie>, title: String): Boolean {
        val key = Movie.normalize(title)
        val index = movies.indexOfFirst { it.titleKey == key }
        if (index < 0) return false
        movies.removeAt(index)
        return true
    }
}
