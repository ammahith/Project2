package com.tictac.project2

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

class GameBoardFragment : Fragment() {

    private lateinit var viewModel: GameViewModel

    // 2D array of buttons
    private lateinit var buttons: Array<Array<Button>>

    // Colors for X and O
    private var xColor: Int = 0
    private var oColor: Int = 0
    private var cellBackgroundColor: Int = 0
    private var gridLineColor: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_game_board, container, false)

        // Initialize buttons array
        buttons = Array(3) { Array(3) { Button(context) } }

        // Get references to all buttons
        buttons[0][0] = view.findViewById(R.id.button00)
        buttons[0][1] = view.findViewById(R.id.button01)
        buttons[0][2] = view.findViewById(R.id.button02)
        buttons[1][0] = view.findViewById(R.id.button10)
        buttons[1][1] = view.findViewById(R.id.button11)
        buttons[1][2] = view.findViewById(R.id.button12)
        buttons[2][0] = view.findViewById(R.id.button20)
        buttons[2][1] = view.findViewById(R.id.button21)
        buttons[2][2] = view.findViewById(R.id.button22)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel using standard ViewModelProvider
        viewModel = ViewModelProvider(requireActivity())[GameViewModel::class.java]

        // Initialize with default colors
        updateThemeColors(GameViewModel.GameTheme.MODERN)

        // Set click listeners for all buttons
        for (i in 0..2) {
            for (j in 0..2) {
                buttons[i][j].setOnClickListener {
                    viewModel.makeMove(i, j)
                }
            }
        }

        // Observe theme changes
        viewModel.currentTheme.observe(viewLifecycleOwner, Observer { theme ->
            updateThemeColors(theme)
            updateBoardUI(viewModel.board.value ?: Array(3) { Array(3) { "" } })
        })

        // Observe the board changes
        viewModel.board.observe(viewLifecycleOwner, Observer { board ->
            updateBoardUI(board)
        })

        // Observe recent move for animation
        viewModel.recentMove.observe(viewLifecycleOwner, Observer { move ->
            move?.let { (row, col) ->
                animateMove(buttons[row][col])
            }
        })

        // Observe winning line to highlight it
        viewModel.winningLine.observe(viewLifecycleOwner, Observer { winningCells ->
            if (winningCells.isNotEmpty()) {
                highlightWinningLine(winningCells)
            }
        })
    }

    private fun updateThemeColors(theme: GameViewModel.GameTheme) {
        when (theme) {
            GameViewModel.GameTheme.MODERN -> {
                xColor = ContextCompat.getColor(requireContext(), R.color.modern_x_color)
                oColor = ContextCompat.getColor(requireContext(), R.color.modern_o_color)
                cellBackgroundColor = ContextCompat.getColor(requireContext(), R.color.modern_cell_background)
                gridLineColor = ContextCompat.getColor(requireContext(), R.color.modern_grid_line)
                view?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.modern_background))
            }
            GameViewModel.GameTheme.CLASSIC -> {
                xColor = ContextCompat.getColor(requireContext(), R.color.classic_x_color)
                oColor = ContextCompat.getColor(requireContext(), R.color.classic_o_color)
                cellBackgroundColor = ContextCompat.getColor(requireContext(), R.color.classic_cell_background)
                gridLineColor = ContextCompat.getColor(requireContext(), R.color.classic_grid_line)
                view?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.classic_background))
            }
            GameViewModel.GameTheme.NATURE -> {
                xColor = ContextCompat.getColor(requireContext(), R.color.nature_x_color)
                oColor = ContextCompat.getColor(requireContext(), R.color.nature_o_color)
                cellBackgroundColor = ContextCompat.getColor(requireContext(), R.color.nature_cell_background)
                gridLineColor = ContextCompat.getColor(requireContext(), R.color.nature_grid_line)
                view?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.nature_background))
            }
        }
    }

    private fun updateBoardUI(board: Array<Array<String>>) {
        for (i in 0..2) {
            for (j in 0..2) {
                val button = buttons[i][j]
                button.text = board[i][j]

                // Apply styling based on the content
                when (board[i][j]) {
                    "X" -> {
                        button.setTextColor(xColor)
                        button.backgroundTintList = ColorStateList.valueOf(cellBackgroundColor)
                    }
                    "O" -> {
                        button.setTextColor(oColor)
                        button.backgroundTintList = ColorStateList.valueOf(cellBackgroundColor)
                    }
                    else -> {
                        button.setTextColor(Color.DKGRAY)
                        button.backgroundTintList = ColorStateList.valueOf(cellBackgroundColor)
                    }
                }
            }
        }
    }

    private fun animateMove(button: Button) {
        // Create scale animation
        val scaleX = ObjectAnimator.ofFloat(button, View.SCALE_X, 0.8f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(button, View.SCALE_Y, 0.8f, 1.0f)

        // Create alpha animation
        val alpha = ObjectAnimator.ofFloat(button, View.ALPHA, 0.5f, 1.0f)

        // Play animations together
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, alpha)
        animatorSet.duration = 300
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.start()
    }

    private fun highlightWinningLine(winningCells: List<Pair<Int, Int>>) {
        // Create background for winning cells
        val winBackground = ColorStateList.valueOf(
            when (viewModel.currentPlayer.value) {
                "X" -> Color.argb(50, Color.red(xColor), Color.green(xColor), Color.blue(xColor))
                "O" -> Color.argb(50, Color.red(oColor), Color.green(oColor), Color.blue(oColor))
                else -> Color.LTGRAY
            }
        )

        // Highlight winning cells
        for ((row, col) in winningCells) {
            val button = buttons[row][col]
            button.backgroundTintList = winBackground

            // Animate the winning cells
            val pulse = ObjectAnimator.ofFloat(button, View.SCALE_X, 1.0f, 1.1f, 1.0f)
            pulse.repeatCount = 1
            pulse.duration = 300
            pulse.start()
        }
    }
}