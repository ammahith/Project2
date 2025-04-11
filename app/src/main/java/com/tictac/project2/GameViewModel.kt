package com.tictac.project2

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.random.Random

class GameViewModel : ViewModel() {

    // Game board (3x3 grid)
    private val _board = MutableLiveData<Array<Array<String>>>()
    val board: LiveData<Array<Array<String>>> = _board

    // Current player (X or O)
    private val _currentPlayer = MutableLiveData<String>()
    val currentPlayer: LiveData<String> = _currentPlayer

    // Game status (Player X's Turn, Player O's Turn, Player X Wins, Player O Wins, Draw)
    private val _gameStatus = MutableLiveData<String>()
    val gameStatus: LiveData<String> = _gameStatus

    // Game mode (PvP or PvAI)
    private val _gameMode = MutableLiveData<GameMode>()
    val gameMode: LiveData<GameMode> = _gameMode

    // AI difficulty level
    private val _aiDifficulty = MutableLiveData<AIDifficulty>()
    val aiDifficulty: LiveData<AIDifficulty> = _aiDifficulty

    // Scores
    private val _playerXScore = MutableLiveData<Int>()
    val playerXScore: LiveData<Int> = _playerXScore

    private val _playerOScore = MutableLiveData<Int>()
    val playerOScore: LiveData<Int> = _playerOScore

    // Draws count
    private val _drawsCount = MutableLiveData<Int>()
    val drawsCount: LiveData<Int> = _drawsCount

    // Is game over flag
    private val _isGameOver = MutableLiveData<Boolean>()
    val isGameOver: LiveData<Boolean> = _isGameOver

    // Recent move for animation
    private val _recentMove = MutableLiveData<Pair<Int, Int>?>()
    val recentMove: LiveData<Pair<Int, Int>?> = _recentMove

    // Current theme
    private val _currentTheme = MutableLiveData<GameTheme>()
    val currentTheme: LiveData<GameTheme> = _currentTheme

    // Winning line coordinates for highlighting
    private val _winningLine = MutableLiveData<List<Pair<Int, Int>>>()
    val winningLine: LiveData<List<Pair<Int, Int>>> = _winningLine

    // Show game over menu flag
    private val _showGameOverMenu = MutableLiveData<Boolean>()
    val showGameOverMenu: LiveData<Boolean> = _showGameOverMenu

    enum class GameMode {
        PLAYER_VS_PLAYER,
        PLAYER_VS_AI
    }

    enum class AIDifficulty {
        EASY,    // Random moves
        MEDIUM,  // Some strategy, can be beaten
        HARD     // Optimal play, can't be beaten
    }

    enum class GameTheme {
        MODERN,
        CLASSIC,
        NATURE
    }

    init {
        resetGame()
        _playerXScore.value = 0
        _playerOScore.value = 0
        _drawsCount.value = 0
        _gameMode.value = GameMode.PLAYER_VS_PLAYER
        _aiDifficulty.value = AIDifficulty.MEDIUM
        _currentTheme.value = GameTheme.MODERN
        _showGameOverMenu.value = false
    }

    fun resetGame() {
        // Initialize empty board
        _board.value = Array(3) { Array(3) { "" } }
        _currentPlayer.value = "X"
        _gameStatus.value = "Player X's Turn"
        _isGameOver.value = false
        _recentMove.value = null
        _winningLine.value = emptyList()
        _showGameOverMenu.value = false
    }

    // New method to reset all scores
    fun resetAllScores() {
        _playerXScore.value = 0
        _playerOScore.value = 0
        _drawsCount.value = 0
        resetGame() // Reset the current game as well
    }

    fun setGameMode(mode: GameMode) {
        _gameMode.value = mode
        resetGame()
    }

    fun setAIDifficulty(difficulty: AIDifficulty) {
        _aiDifficulty.value = difficulty
    }

    fun setGameTheme(theme: GameTheme) {
        _currentTheme.value = theme
    }

    fun makeMove(row: Int, col: Int) {
        // Check if the cell is already occupied or the game is over
        if (_board.value!![row][col].isNotEmpty() || _isGameOver.value == true) {
            return
        }

        // Make the move
        val currentBoard = _board.value!!.map { it.clone() }.toTypedArray()
        currentBoard[row][col] = _currentPlayer.value!!
        _board.value = currentBoard

        // Set recent move for animation
        _recentMove.value = Pair(row, col)

        // Check for win or draw
        val winResult = checkWin(row, col)
        if (winResult.first) {
            _gameStatus.value = "Player ${_currentPlayer.value} Wins!"
            _isGameOver.value = true
            _showGameOverMenu.value = true
            _winningLine.value = winResult.second

            // Update score
            if (_currentPlayer.value == "X") {
                _playerXScore.value = _playerXScore.value!! + 1
            } else {
                _playerOScore.value = _playerOScore.value!! + 1
            }

            return
        }

        // Check for draw
        if (isBoardFull()) {
            _gameStatus.value = "It's a Draw!"
            _isGameOver.value = true
            _showGameOverMenu.value = true
            _drawsCount.value = _drawsCount.value!! + 1
            return
        }

        // Switch player
        _currentPlayer.value = if (_currentPlayer.value == "X") "O" else "X"
        _gameStatus.value = "Player ${_currentPlayer.value}'s Turn"

        // If it's AI's turn and game mode is PvAI
        if (_currentPlayer.value == "O" && _gameMode.value == GameMode.PLAYER_VS_AI && !_isGameOver.value!!) {
            // Add slight delay for AI move to feel more natural
            // In reality, you'd use a coroutine or handler here
            // This is a placeholder to indicate where the delay would be
            makeAIMove()
        }
    }

    private fun makeAIMove() {
        when (_aiDifficulty.value) {
            AIDifficulty.EASY -> makeRandomMove()
            AIDifficulty.MEDIUM -> makeMediumAIMove()
            AIDifficulty.HARD -> makeOptimalAIMove()
            else -> makeRandomMove()
        }
    }

    private fun makeRandomMove() {
        // Simple AI: Just make a random move to an empty cell
        val emptyCells = mutableListOf<Pair<Int, Int>>()

        for (i in 0..2) {
            for (j in 0..2) {
                if (_board.value!![i][j].isEmpty()) {
                    emptyCells.add(Pair(i, j))
                }
            }
        }

        if (emptyCells.isNotEmpty()) {
            val randomMove = emptyCells[Random.nextInt(emptyCells.size)]
            makeMove(randomMove.first, randomMove.second)
        }
    }

    private fun makeMediumAIMove() {
        // Check if AI can win in the next move
        val winningMove = findWinningMove("O")
        if (winningMove != null) {
            makeMove(winningMove.first, winningMove.second)
            return
        }

        // Check if player can win in the next move and block it
        val blockingMove = findWinningMove("X")
        if (blockingMove != null) {
            makeMove(blockingMove.first, blockingMove.second)
            return
        }

        // If center is empty, take it
        if (_board.value!![1][1].isEmpty()) {
            makeMove(1, 1)
            return
        }

        // Otherwise make a random move
        makeRandomMove()
    }

    private fun makeOptimalAIMove() {
        // Implement minimax algorithm for optimal play
        // For simplicity, we'll use a predefined strategy for common scenarios

        val board = _board.value!!

        // Check if AI can win in the next move
        val winningMove = findWinningMove("O")
        if (winningMove != null) {
            makeMove(winningMove.first, winningMove.second)
            return
        }

        // Check if player can win in the next move and block it
        val blockingMove = findWinningMove("X")
        if (blockingMove != null) {
            makeMove(blockingMove.first, blockingMove.second)
            return
        }

        // Take center if available
        if (board[1][1].isEmpty()) {
            makeMove(1, 1)
            return
        }

        // Take a corner if available
        val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
        val emptyCorners = corners.filter { board[it.first][it.second].isEmpty() }
        if (emptyCorners.isNotEmpty()) {
            val corner = emptyCorners[Random.nextInt(emptyCorners.size)]
            makeMove(corner.first, corner.second)
            return
        }

        // Take any remaining side
        val sides = listOf(Pair(0, 1), Pair(1, 0), Pair(1, 2), Pair(2, 1))
        val emptySides = sides.filter { board[it.first][it.second].isEmpty() }
        if (emptySides.isNotEmpty()) {
            val side = emptySides[Random.nextInt(emptySides.size)]
            makeMove(side.first, side.second)
            return
        }
    }

    private fun findWinningMove(player: String): Pair<Int, Int>? {
        val board = _board.value!!

        // Check rows
        for (i in 0..2) {
            if (board[i].count { it == player } == 2 && board[i].contains("")) {
                val j = board[i].indexOf("")
                if (j != -1) return Pair(i, j)
            }
        }

        // Check columns
        for (j in 0..2) {
            val column = (0..2).map { board[it][j] }
            if (column.count { it == player } == 2 && column.contains("")) {
                val i = column.indexOf("")
                if (i != -1) return Pair(i, j)
            }
        }

        // Check diagonals
        val diag1 = listOf(board[0][0], board[1][1], board[2][2])
        if (diag1.count { it == player } == 2 && diag1.contains("")) {
            val idx = diag1.indexOf("")
            return Pair(idx, idx)
        }

        val diag2 = listOf(board[0][2], board[1][1], board[2][0])
        if (diag2.count { it == player } == 2 && diag2.contains("")) {
            val idx = diag2.indexOf("")
            return when (idx) {
                0 -> Pair(0, 2)
                1 -> Pair(1, 1)
                2 -> Pair(2, 0)
                else -> null
            }
        }

        return null
    }

    private fun checkWin(row: Int, col: Int): Pair<Boolean, List<Pair<Int, Int>>> {
        val player = _currentPlayer.value!!
        val board = _board.value!!

        // Check row
        if (board[row][0] == player && board[row][1] == player && board[row][2] == player) {
            return Pair(true, listOf(Pair(row, 0), Pair(row, 1), Pair(row, 2)))
        }

        // Check column
        if (board[0][col] == player && board[1][col] == player && board[2][col] == player) {
            return Pair(true, listOf(Pair(0, col), Pair(1, col), Pair(2, col)))
        }

        // Check diagonal from top-left to bottom-right
        if (row == col && board[0][0] == player && board[1][1] == player && board[2][2] == player) {
            return Pair(true, listOf(Pair(0, 0), Pair(1, 1), Pair(2, 2)))
        }

        // Check diagonal from top-right to bottom-left
        if (row + col == 2 && board[0][2] == player && board[1][1] == player && board[2][0] == player) {
            return Pair(true, listOf(Pair(0, 2), Pair(1, 1), Pair(2, 0)))
        }

        return Pair(false, emptyList())
    }

    private fun isBoardFull(): Boolean {
        for (i in 0..2) {
            for (j in 0..2) {
                if (_board.value!![i][j].isEmpty()) {
                    return false
                }
            }
        }
        return true
    }
}