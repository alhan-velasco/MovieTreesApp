package com.alhan.movietreesapp.core.data

import android.content.Context
import com.alhan.movietreesapp.features.movies.domain.model.Movie

class CsvDataSource(private val context: Context) {

    fun readMovies(): List<Movie> {
        val text = context.assets.open("Movies.csv").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val records = parseCsv(text)
        val movieList = ArrayList<Movie>(records.size)

        for (tokens in records.drop(1)) {
            if (tokens.size < 11) continue
            val id = tokens[1].trim().toLongOrNull() ?: continue
            val title = tokens[2].trim()
            if (title.isEmpty()) continue
            movieList.add(
                Movie(
                    id = id,
                    title = title,
                    popularity = tokens[7].trim().toDoubleOrNull() ?: 0.0,
                    releaseDate = tokens[8].trim(),
                    voteAverage = tokens[9].trim().toDoubleOrNull() ?: 0.0,
                    voteCount = tokens[10].trim().toIntOrNull() ?: 0
                )
            )
        }
        return movieList
    }

    private fun parseCsv(text: String): List<List<String>> {
        val records = ArrayList<List<String>>()
        var fields = ArrayList<String>()
        val current = StringBuilder()
        var insideQuotes = false
        var i = 0

        while (i < text.length) {
            val ch = text[i]
            if (insideQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        current.append('"'); i++
                    } else {
                        insideQuotes = false
                    }
                } else {
                    current.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> insideQuotes = true
                    ',' -> { fields.add(current.toString()); current.clear() }
                    '\r' -> { }
                    '\n' -> {
                        fields.add(current.toString()); current.clear()
                        records.add(fields); fields = ArrayList()
                    }
                    else -> current.append(ch)
                }
            }
            i++
        }
        if (current.isNotEmpty() || fields.isNotEmpty()) {
            fields.add(current.toString())
            records.add(fields)
        }
        return records
    }
}
