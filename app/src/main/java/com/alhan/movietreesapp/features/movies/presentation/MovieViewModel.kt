package com.alhan.movietreesapp.features.movies.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhan.movietreesapp.features.movies.domain.model.Movie
import com.alhan.movietreesapp.features.movies.domain.model.TreeMode
import com.alhan.movietreesapp.features.movies.domain.repository.MovieRepository
import com.alhan.movietreesapp.features.movies.domain.tree.MovieTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.ln
import kotlin.system.measureNanoTime

data class MovieUiState(
    val isLoading: Boolean = false,
    val totalLoaded: Int = 0,
    val listSize: Int = 0,
    val currentMode: TreeMode = TreeMode.ORDERED,
    val treeHeight: Int = 0,
    val theoreticalLog2N: Double = 0.0,
    val searchResult: Movie? = null,
    val treeSearchSteps: Int = 0,
    val treeSearchTimeMs: Double = 0.0,
    val linearSearchSteps: Int = 0,
    val linearSearchTimeMs: Double = 0.0,
    val treeInsertSteps: Int = 0,
    val treeInsertTimeMs: Double = 0.0,
    val linearInsertSteps: Int = 0,
    val linearInsertTimeMs: Double = 0.0,
    val operationMessage: String = ""
)

class MovieViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    companion object {
        const val SEARCH_REPETITIONS = 200
    }

    private val _uiState = MutableStateFlow(MovieUiState())
    val uiState: StateFlow<MovieUiState> = _uiState.asStateFlow()

    private val mutex = Mutex()

    private val insertionOrder: MutableList<Movie> = mutableListOf()

    private val sortedList: MutableList<Movie> = mutableListOf()

    private var movieTree: MovieTree = MovieTree(TreeMode.ORDERED)

    private var orderedTree: MovieTree = MovieTree(TreeMode.ORDERED)

    private var lastQuery: String? = null

    init {
        loadDataset(TreeMode.ORDERED)
    }

    fun loadDataset(mode: TreeMode) {
        viewModelScope.launch(Dispatchers.Default) {
            mutex.withLock {
                _uiState.update { it.copy(isLoading = true, operationMessage = "Cargando películas en memoria RAM...") }

                try {
                    if (insertionOrder.isEmpty()) {
                        val loaded = repository.loadMoviesFromCsv().distinctBy { it.id }
                        insertionOrder.addAll(loaded)
                        sortedList.addAll(loaded.sortedWith(Movie.ORDER))
                        orderedTree = repository.buildTree(insertionOrder, TreeMode.ORDERED)
                    }
                    movieTree = if (mode == TreeMode.ORDERED || mode == TreeMode.LIST) {
                        orderedTree
                    } else {
                        repository.buildTree(insertionOrder, mode)
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentMode = mode,
                            searchResult = null,
                            treeSearchSteps = 0, treeSearchTimeMs = 0.0,
                            linearSearchSteps = 0, linearSearchTimeMs = 0.0,
                            treeInsertSteps = 0, treeInsertTimeMs = 0.0,
                            linearInsertSteps = 0, linearInsertTimeMs = 0.0,
                            operationMessage = "Estructuras listas: ${movieTree.nodeCount} películas (Modo ${modeLabel(mode)})"
                        )
                    }
                    refreshStats()

                    lastQuery?.let { runSearch(it) }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(isLoading = false, operationMessage = "Error al cargar: ${e.message ?: e::class.simpleName}")
                    }
                }
            }
        }
    }

    fun searchMovie(title: String) {
        if (title.isBlank()) return
        lastQuery = title
        viewModelScope.launch(Dispatchers.Default) {
            mutex.withLock { runSearch(title) }
        }
    }

    private fun runSearch(title: String) {
        run {
            run {
                movieTree.searchByTitle(title)
                repository.linearSearch(sortedList, title)

                val (treeResult, treeSteps) = movieTree.searchByTitle(title)
                val treeTime = measureNanoTime {
                    repeat(SEARCH_REPETITIONS) { movieTree.searchByTitle(title) }
                } / 1_000_000.0 / SEARCH_REPETITIONS

                val (_, linearSteps) = repository.linearSearch(sortedList, title)
                val linearTime = measureNanoTime {
                    repeat(SEARCH_REPETITIONS) { repository.linearSearch(sortedList, title) }
                } / 1_000_000.0 / SEARCH_REPETITIONS

                _uiState.update {
                    it.copy(
                        searchResult = treeResult,
                        treeSearchSteps = treeSteps,
                        treeSearchTimeMs = treeTime,
                        linearSearchSteps = linearSteps,
                        linearSearchTimeMs = linearTime,
                        operationMessage = if (treeResult != null)
                            "Película encontrada en ambas estructuras (promedio de $SEARCH_REPETITIONS búsquedas)."
                        else "Película \"$title\" no encontrada."
                    )
                }
            }
        }
    }

    fun benchmarkInsert(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.Default) {
            mutex.withLock {
                if (repository.linearSearch(sortedList, title).first != null) {
                    _uiState.update { it.copy(operationMessage = "Ya existe una película titulada \"$title\". No se insertó.") }
                    return@withLock
                }

                val newMovie = Movie(
                    id = (insertionOrder.maxOfOrNull { it.id } ?: 0L) + 1,
                    title = title.trim(),
                    popularity = 50.0,
                    voteAverage = 8.0,
                    voteCount = 100,
                    releaseDate = "2026-10-19"
                )

                var treeSteps = 0
                val treeTime = measureNanoTime {
                    treeSteps = movieTree.insert(newMovie)
                } / 1_000_000.0
                if (movieTree !== orderedTree) {
                    orderedTree.insert(newMovie)
                }

                var linearSteps = 0
                val linearTime = measureNanoTime {
                    linearSteps = repository.linearInsertOrdered(sortedList, newMovie)
                } / 1_000_000.0

                insertionOrder.add(newMovie)

                _uiState.update {
                    it.copy(
                        treeInsertSteps = treeSteps,
                        treeInsertTimeMs = treeTime,
                        linearInsertSteps = linearSteps,
                        linearInsertTimeMs = linearTime,
                        operationMessage = "Película '${newMovie.title}' insertada en ambas estructuras."
                    )
                }
                refreshStats()
            }
        }
    }

    fun deleteMovie(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.Default) {
            mutex.withLock {
                val target = movieTree.searchByTitle(title).first
                val deleted = movieTree.deleteByTitle(title)
                if (deleted && target != null) {
                    if (movieTree !== orderedTree) {
                        orderedTree.deleteByTitle(title)
                    }
                    sortedList.removeAll { it.id == target.id }
                    insertionOrder.removeAll { it.id == target.id }
                }
                _uiState.update {
                    it.copy(
                        searchResult = if (deleted) null else it.searchResult,
                        operationMessage = if (deleted) "Película \"$title\" eliminada de ambas estructuras."
                        else "Título no encontrado."
                    )
                }
                refreshStats()
            }
        }
    }

    private fun refreshStats() {
        val n = movieTree.nodeCount
        _uiState.update {
            it.copy(
                totalLoaded = n,
                listSize = sortedList.size,
                treeHeight = movieTree.height(),
                theoreticalLog2N = if (n > 0) ln(n.toDouble()) / ln(2.0) else 0.0
            )
        }
    }
}

fun modeLabel(mode: TreeMode): String = when (mode) {
    TreeMode.ORDERED -> "Ordenado (BST)"
    TreeMode.STACK -> "Pila (LIFO)"
    TreeMode.QUEUE -> "Cola (FIFO)"
    TreeMode.LIST -> "Lista Lineal"
}
