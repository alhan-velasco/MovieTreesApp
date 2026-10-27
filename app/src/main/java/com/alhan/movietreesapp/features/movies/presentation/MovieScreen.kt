package com.alhan.movietreesapp.features.movies.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alhan.movietreesapp.features.movies.domain.model.TreeMode
import java.util.Locale

private val TreeColor = Color(0xFF2E7D32)
private val ListColor = Color(0xFFC62828)

private fun formatTime(ms: Double): String {
    return when {
        ms == 0.0 -> "0 ms"
        ms < 0.001 -> String.format(Locale.US, "%.1f ns", ms * 1_000_000.0)
        ms < 0.1 -> String.format(Locale.US, "%.2f µs", ms * 1_000.0)
        else -> String.format(Locale.US, "%.4f ms", ms)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieScreen(viewModel: MovieViewModel) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var searchText by remember { mutableStateOf("UNABOMBER") }
    var newTitleText by remember { mutableStateOf("Película de Prueba") }

    val isOrdered = state.currentMode == TreeMode.ORDERED || state.currentMode == TreeMode.LIST
    val treeSearchBigO = if (isOrdered) "O(log N)" else "O(N) degenerado"
    val treeInsertBigO = when (state.currentMode) {
        TreeMode.ORDERED, TreeMode.LIST -> "O(log N)"
        TreeMode.STACK -> "O(1) en raíz"
        TreeMode.QUEUE -> "O(N) al final"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Text(
                    text = "MovieTrees",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Árbol Binario vs Lista Lineal",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Películas", "${state.totalLoaded}", Modifier.weight(1f))
                    StatPill("Altura", "${state.treeHeight}", Modifier.weight(1f))
                    StatPill("log₂ N", String.format(Locale.US, "%.1f", state.theoreticalLog2N), Modifier.weight(1f))
                }
            }
        }

        if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.operationMessage.isNotEmpty()) {
                Text(
                    text = state.operationMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(title = "Escenario de comparación") {
                val modes = listOf(TreeMode.ORDERED, TreeMode.STACK, TreeMode.QUEUE, TreeMode.LIST)
                val labels = listOf("Ordenado", "Pila", "Cola", "Lista")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.currentMode == mode,
                            onClick = { if (!state.isLoading) viewModel.loadDataset(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
                        ) { Text(labels[index], maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (state.currentMode) {
                        TreeMode.ORDERED -> "BST: cada nodo va a la izquierda o derecha según el título. Búsqueda O(log N)."
                        TreeMode.STACK -> "Pila (LIFO): cada película se vuelve la nueva raíz. Árbol degenerado O(N)."
                        TreeMode.QUEUE -> "Cola (FIFO): cada película se agrega al final derecho. Árbol degenerado O(N)."
                        TreeMode.LIST -> "Lista Lineal (mutableListOf): recorrido secuencial puro elemento por elemento O(N)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SectionCard(title = "Búsqueda por título") {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Título de la película") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.searchMovie(searchText) }),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.searchMovie(searchText) },
                        enabled = !state.isLoading,
                        modifier = Modifier.weight(1f)
                    ) { Text("Buscar") }
                    OutlinedButton(
                        onClick = { viewModel.deleteMovie(searchText) },
                        enabled = !state.isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f)
                    ) { Text("Eliminar") }
                }

                if (state.treeSearchSteps > 0 || state.linearSearchSteps > 0) {
                    Spacer(Modifier.height(16.dp))
                    val maxSteps = maxOf(state.treeSearchSteps, state.linearSearchSteps, 1)
                    if (state.currentMode == TreeMode.LIST) {
                        ComparisonBar(
                            label = "Lista Lineal (mutableListOf) · O(N)",
                            steps = state.linearSearchSteps,
                            timeMs = state.linearSearchTimeMs,
                            fraction = state.linearSearchSteps.toFloat() / maxSteps,
                            color = ListColor
                        )
                        Spacer(Modifier.height(12.dp))
                        ComparisonBar(
                            label = "Árbol BST · $treeSearchBigO",
                            steps = state.treeSearchSteps,
                            timeMs = state.treeSearchTimeMs,
                            fraction = state.treeSearchSteps.toFloat() / maxSteps,
                            color = TreeColor
                        )
                    } else {
                        ComparisonBar(
                            label = "Árbol · $treeSearchBigO",
                            steps = state.treeSearchSteps,
                            timeMs = state.treeSearchTimeMs,
                            fraction = state.treeSearchSteps.toFloat() / maxSteps,
                            color = TreeColor
                        )
                        Spacer(Modifier.height(12.dp))
                        ComparisonBar(
                            label = "Lista · O(N)",
                            steps = state.linearSearchSteps,
                            timeMs = state.linearSearchTimeMs,
                            fraction = state.linearSearchSteps.toFloat() / maxSteps,
                            color = ListColor
                        )
                    }
                    if (state.treeSearchSteps > 0 && state.linearSearchSteps > 0) {
                        Spacer(Modifier.height(10.dp))
                        val ratio = state.linearSearchSteps.toDouble() / state.treeSearchSteps
                        Text(
                            text = if (ratio >= 1) "El árbol hizo ${String.format(Locale.US, "%.1f", ratio)}× menos comparaciones"
                            else "La lista hizo ${String.format(Locale.US, "%.1f", 1 / ratio)}× menos comparaciones",
                            fontWeight = FontWeight.SemiBold,
                            color = if (ratio >= 1) TreeColor else ListColor
                        )
                    }
                }
            }

            AnimatedVisibility(visible = state.searchResult != null) {
                state.searchResult?.let { movie ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = movie.title.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = movie.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("Estreno: ${movie.releaseDate}   Calificación: ${movie.voteAverage} (${movie.voteCount})")
                                Text(
                                    "ID ${movie.id} · Popularidad ${movie.popularity}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            SectionCard(title = "Inserción dinámica") {
                OutlinedTextField(
                    value = newTitleText,
                    onValueChange = { newTitleText = it },
                    label = { Text("Título de la nueva película") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = { viewModel.benchmarkInsert(newTitleText) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Insertar en ambas estructuras") }

                if (state.treeInsertTimeMs > 0.0 || state.linearInsertTimeMs > 0.0) {
                    Spacer(Modifier.height(16.dp))
                    val maxSteps = maxOf(state.treeInsertSteps, state.linearInsertSteps, 1)
                    if (state.currentMode == TreeMode.LIST) {
                        ComparisonBar(
                            label = "Lista ordenada (mutableListOf) · O(N) + desplazamiento",
                            steps = state.linearInsertSteps,
                            timeMs = state.linearInsertTimeMs,
                            fraction = state.linearInsertSteps.toFloat() / maxSteps,
                            color = ListColor
                        )
                        Spacer(Modifier.height(12.dp))
                        ComparisonBar(
                            label = "Árbol BST · $treeInsertBigO",
                            steps = state.treeInsertSteps,
                            timeMs = state.treeInsertTimeMs,
                            fraction = state.treeInsertSteps.toFloat() / maxSteps,
                            color = TreeColor
                        )
                    } else {
                        ComparisonBar(
                            label = "Árbol · $treeInsertBigO",
                            steps = state.treeInsertSteps,
                            timeMs = state.treeInsertTimeMs,
                            fraction = state.treeInsertSteps.toFloat() / maxSteps,
                            color = TreeColor
                        )
                        Spacer(Modifier.height(12.dp))
                        ComparisonBar(
                            label = "Lista ordenada · O(N) + desplazamiento",
                            steps = state.linearInsertSteps,
                            timeMs = state.linearInsertTimeMs,
                            fraction = state.linearInsertSteps.toFloat() / maxSteps,
                            color = ListColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ComparisonBar(label: String, steps: Int, timeMs: Double, fraction: Float, color: Color) {
    val animated by animateFloatAsState(targetValue = fraction.coerceIn(0.02f, 1f), label = "bar")
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.SemiBold, color = color, style = MaterialTheme.typography.bodyMedium)
            Text(formatTime(timeMs), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(color.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(RoundedCornerShape(7.dp))
                    .background(color)
            )
        }
        Text("$steps pasos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
