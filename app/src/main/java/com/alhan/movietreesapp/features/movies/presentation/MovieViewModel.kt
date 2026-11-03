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
    val loadTimeMs: Double = 0.0,
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

    private val _uiState = MutableStateFlow(MovieUiState())
    val uiState: StateFlow<MovieUiState> = _uiState.asStateFlow()

    private val mutex = Mutex()

    private val rawList: MutableList<Movie> = mutableListOf()

    private var movieTree: MovieTree = MovieTree(TreeMode.ORDERED)

    private var lastQuery: String? = null

    init {
        loadDataset(TreeMode.ORDERED)
    }

    fun loadDataset(mode: TreeMode) {
        viewModelScope.launch(Dispatchers.Default) {
            mutex.withLock {
                _uiState.update { it.copy(isLoading = true, operationMessage = "Cargando CSV en memoria RAM...") }

                try {
                    val loadTime = measureNanoTime {
                        val loadedFromCsv = repository.loadMoviesFromCsv()
                        rawList.clear()
                        rawList.addAll(loadedFromCsv)
                        val treeModeToBuild = if (mode == TreeMode.LIST) TreeMode.ORDERED else mode
                        movieTree = repository.buildTree(rawList, treeModeToBuild)
                    } / 1_000_000.0

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentMode = mode,
                            loadTimeMs = loadTime,
                            searchResult = null,
                            treeSearchSteps = 0, treeSearchTimeMs = 0.0,
                            linearSearchSteps = 0, linearSearchTimeMs = 0.0,
                            treeInsertSteps = 0, treeInsertTimeMs = 0.0,
                            linearInsertSteps = 0, linearInsertTimeMs = 0.0,
                            operationMessage = "CSV cargado en RAM: ${rawList.size} registros en bruto (Modo ${modeLabel(mode)})"
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
        var treeResult: Movie? = null
        var treeSteps = 0
        val treeTime = measureNanoTime {
            val res = movieTree.searchByTitle(title)
            treeResult = res.first
            treeSteps = res.second
        } / 1_000_000.0

        var linearResult: Movie? = null
        var linearSteps = 0
        val linearTime = measureNanoTime {
            val res = repository.linearSearch(rawList, title)
            linearResult = res.first
            linearSteps = res.second
        } / 1_000_000.0

        val foundMovie = treeResult ?: linearResult

        _uiState.update {
            it.copy(
                searchResult = foundMovie,
                treeSearchSteps = treeSteps,
                treeSearchTimeMs = treeTime,
                linearSearchSteps = linearSteps,
                linearSearchTimeMs = linearTime,
                operationMessage = if (foundMovie != null)
                    "Película encontrada en ambas estructuras."
                else "Película \"$title\" no encontrada."
            )
        }
    }

    fun benchmarkInsert(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.Default) {
            mutex.withLock {
                if (repository.linearSearch(rawList, title).first != null) {
                    _uiState.update { it.copy(operationMessage = "Ya existe una película titulada \"$title\". No se insertó.") }
                    return@withLock
                }

                var maxId = 0L
                for (m in rawList) {
                    if (m.id > maxId) maxId = m.id
                }

                val newMovie = Movie(
                    id = maxId + 1,
                    title = title.trim(),
                    popularity = 50.0,
                    voteAverage = 8.0,
                    voteCount = 100,
                    releaseDate = "2026-11-03"
                )

                var treeSteps = 0
                val treeTime = measureNanoTime {
                    treeSteps = movieTree.insert(newMovie)
                } / 1_000_000.0

                var linearSteps = 0
                val linearTime = measureNanoTime {
                    linearSteps = repository.linearInsert(rawList, newMovie)
                } / 1_000_000.0

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
                val deletedFromTree = movieTree.deleteByTitle(title)
                val (deletedFromList, _) = repository.linearDelete(rawList, title)
                val deleted = deletedFromTree || deletedFromList
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
                listSize = rawList.size,
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
    TreeMode.LIST -> "Lista en Bruto"
}
