package com.alhan.movietreesapp.features.movies.domain.tree

import com.alhan.movietreesapp.features.movies.domain.model.Movie
import com.alhan.movietreesapp.features.movies.domain.model.TreeMode

/**
 * Árbol binario de películas con clave = título.
 *
 * - ORDERED: BST estándar (izquierda < nodo < derecha). Búsqueda O(log N) promedio.
 * - STACK:   cada inserción se vuelve la nueva raíz (cadena a la izquierda). Inserción O(1), búsqueda O(N).
 * - QUEUE:   cada inserción va al final de la cadena derecha. Inserción O(N), búsqueda O(N).
 *
 * En STACK/QUEUE el árbol queda degenerado (no respeta el orden por título), por lo que
 * la búsqueda y la eliminación deben recorrer los nodos uno por uno.
 */
class MovieTree(val mode: TreeMode) {
    var root: MovieNode? = null
        private set

    var nodeCount: Int = 0
        private set

    /** Inserta según el modo del árbol. Devuelve los pasos realizados, o -1 si ya existía. */
    fun insert(movie: Movie): Int = when (mode) {
        TreeMode.ORDERED -> insertOrdered(movie)
        TreeMode.STACK -> insertStackMode(movie)
        TreeMode.QUEUE -> insertQueueMode(movie)
    }

    // Escenario 1: Modo Ordenado (BST) - O(log N) promedio, O(N) peor caso
    private fun insertOrdered(movie: Movie): Int {
        val newNode = MovieNode(movie)
        var current = root
        if (current == null) {
            root = newNode
            nodeCount++
            return 1
        }
        var steps = 0
        while (true) {
            steps++
            val c = Movie.compare(movie, current!!.movie)
            if (c == 0) return -1 // Duplicado: se ignora
            if (c < 0) {
                if (current.left == null) { current.left = newNode; break }
                current = current.left
            } else {
                if (current.right == null) { current.right = newNode; break }
                current = current.right
            }
        }
        nodeCount++
        return steps
    }

    // Escenario 2: Modo Pila (LIFO) - Inserción en la raíz O(1)
    private fun insertStackMode(movie: Movie): Int {
        val newNode = MovieNode(movie)
        newNode.left = root
        root = newNode
        nodeCount++
        return 1
    }

    // Escenario 3: Modo Cola (FIFO) - Inserción al final O(N)
    private fun insertQueueMode(movie: Movie): Int {
        val newNode = MovieNode(movie)
        var steps = 1
        if (root == null) {
            root = newNode
        } else {
            var temp = root!!
            while (temp.right != null) {
                temp = temp.right!!
                steps++
            }
            temp.right = newNode
        }
        nodeCount++
        return steps
    }

    /** Búsqueda por título. Devuelve la película encontrada y el número de nodos visitados. */
    fun searchByTitle(title: String): Pair<Movie?, Int> {
        val key = Movie.normalize(title)
        return if (mode == TreeMode.ORDERED) searchOrdered(key) else searchFullTraversal(key)
    }

    // BST: cada comparación descarta un subárbol completo -> O(log N) promedio
    private fun searchOrdered(key: String): Pair<Movie?, Int> {
        var steps = 0
        var current = root
        while (current != null) {
            steps++
            val c = key.compareTo(current.movie.titleKey)
            if (c == 0) return Pair(current.movie, steps)
            current = if (c < 0) current.left else current.right
        }
        return Pair(null, steps)
    }

    // Árbol degenerado (Pila/Cola): no hay orden que aprovechar -> recorrido completo O(N)
    private fun searchFullTraversal(key: String): Pair<Movie?, Int> {
        var steps = 0
        val stack = ArrayDeque<MovieNode>()
        root?.let { stack.addLast(it) }
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            steps++
            if (node.movie.titleKey == key) return Pair(node.movie, steps)
            node.right?.let { stack.addLast(it) }
            node.left?.let { stack.addLast(it) }
        }
        return Pair(null, steps)
    }

    /** Elimina la primera película cuyo título coincida. */
    fun deleteByTitle(title: String): Boolean {
        val target = searchByTitle(title).first ?: return false
        return if (mode == TreeMode.ORDERED) {
            root = deleteRec(root, target)
            nodeCount--
            true
        } else {
            deleteUnordered(target)
        }
    }

    // Eliminación BST: reemplaza por el sucesor inmediato (mínimo del subárbol derecho)
    private fun deleteRec(current: MovieNode?, target: Movie): MovieNode? {
        if (current == null) return null
        val c = Movie.compare(target, current.movie)
        when {
            c < 0 -> current.left = deleteRec(current.left, target)
            c > 0 -> current.right = deleteRec(current.right, target)
            else -> {
                if (current.left == null) return current.right
                if (current.right == null) return current.left
                val minNode = findMin(current.right!!)
                return MovieNode(minNode.movie, current.left, deleteRec(current.right, minNode.movie))
            }
        }
        return current
    }

    private fun findMin(node: MovieNode): MovieNode {
        var current = node
        while (current.left != null) current = current.left!!
        return current
    }

    // Eliminación en árbol degenerado: se localiza el nodo y su padre y se re-enlaza el hijo
    private fun deleteUnordered(target: Movie): Boolean {
        var parent: MovieNode? = null
        var found: MovieNode? = null
        val stack = ArrayDeque<Pair<MovieNode, MovieNode?>>()
        root?.let { stack.addLast(Pair(it, null)) }
        while (stack.isNotEmpty()) {
            val (node, p) = stack.removeLast()
            if (node.movie == target) { found = node; parent = p; break }
            node.right?.let { stack.addLast(Pair(it, node)) }
            node.left?.let { stack.addLast(Pair(it, node)) }
        }
        val node = found ?: return false

        val replacement: MovieNode? = when {
            node.left == null -> node.right
            node.right == null -> node.left
            else -> {
                var rightmost = node.left!!
                while (rightmost.right != null) rightmost = rightmost.right!!
                rightmost.right = node.right
                node.left
            }
        }
        when {
            parent == null -> root = replacement
            parent.left === node -> parent.left = replacement
            else -> parent.right = replacement
        }
        nodeCount--
        return true
    }

    /** Altura real del árbol (número de niveles), calculada de forma iterativa. */
    fun height(): Int {
        var levels = 0
        var currentLevel = ArrayList<MovieNode>()
        root?.let { currentLevel.add(it) }
        while (currentLevel.isNotEmpty()) {
            levels++
            val next = ArrayList<MovieNode>()
            for (n in currentLevel) {
                n.left?.let { next.add(it) }
                n.right?.let { next.add(it) }
            }
            currentLevel = next
        }
        return levels
    }

    fun clear() {
        root = null
        nodeCount = 0
    }
}
