package com.tictac.project2

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.widget.Toast

class GameStatusFragment : Fragment() {

    private lateinit var viewModel: GameViewModel

    // Declare textViewGameStatus as nullable since it's no longer in the layout
    private var textViewGameStatus: TextView? = null
    private lateinit var buttonReset: Button
    private lateinit var buttonNewGame: Button
    private lateinit var buttonContainer: LinearLayout
    private lateinit var radioButtonPvP: RadioButton
    private lateinit var radioButtonPvAI: RadioButton
    private lateinit var textViewPlayerXScore: TextView
    private lateinit var textViewPlayerOScore: TextView
    private lateinit var spinnerTheme: Spinner
    private lateinit var spinnerDifficulty: Spinner
    private lateinit var textViewDraws: TextView
    private lateinit var gameOverOverlay: FrameLayout
    private lateinit var textViewGameResult: TextView

    // Colors for theme
    private var textColor: Int = 0
    private var xColor: Int = 0
    private var oColor: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_game_status, container, false)

        try {
            // Try to find the view, but don't crash if it's not found
            textViewGameStatus = view.findViewById(R.id.textViewGameStatus)

            // Required views
            buttonContainer = view.findViewById(R.id.buttonContainer)
            buttonReset = view.findViewById(R.id.buttonReset)
            buttonNewGame = view.findViewById(R.id.buttonNewGame)
            radioButtonPvP = view.findViewById(R.id.radioButtonPvP)
            radioButtonPvAI = view.findViewById(R.id.radioButtonPvAI)
            textViewPlayerXScore = view.findViewById(R.id.textViewPlayerXScore)
            textViewPlayerOScore = view.findViewById(R.id.textViewPlayerOScore)
            spinnerTheme = view.findViewById(R.id.spinnerTheme)
            gameOverOverlay = view.findViewById(R.id.gameOverOverlay)
            textViewGameResult = view.findViewById(R.id.textViewGameResult)

            // Optional views
            view.findViewById<Spinner?>(R.id.spinnerDifficulty)?.let {
                spinnerDifficulty = it
            }

            view.findViewById<TextView?>(R.id.textViewDraws)?.let {
                textViewDraws = it
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[GameViewModel::class.java]

        // Initialize with default theme colors
        updateThemeColors(GameViewModel.GameTheme.MODERN)

        // Setup spinner adapters with custom layout
        setupCustomSpinners()

        // Setup spinner item selection listeners
        setupThemeSpinner()

        // Setup difficulty spinner if it exists
        if (::spinnerDifficulty.isInitialized) {
            setupDifficultySpinner()
        }

        // Observe theme changes
        viewModel.currentTheme.observe(viewLifecycleOwner, Observer { theme ->
            updateThemeColors(theme)
            updateUI()
        })

        // Observe game over menu state
        viewModel.showGameOverMenu.observe(viewLifecycleOwner, Observer { show ->
            if (show) {
                // Show game over UI
                buttonContainer.visibility = View.VISIBLE
                buttonReset.text = "PLAY AGAIN"

                // Show overlay with result
                gameOverOverlay.visibility = View.VISIBLE
                textViewGameResult.text = viewModel.gameStatus.value

                // Hide theme and difficulty spinners when showing game over menu
                spinnerTheme.visibility = View.GONE
                if (::spinnerDifficulty.isInitialized) {
                    spinnerDifficulty.visibility = View.GONE
                }

                // Animate in
                gameOverOverlay.alpha = 0f
                gameOverOverlay.animate().alpha(1f).setDuration(300).start()
            } else {
                // Hide game over UI
                buttonContainer.visibility = View.GONE
                gameOverOverlay.visibility = View.GONE

                // Show theme spinner again
                spinnerTheme.visibility = View.VISIBLE

                // Show difficulty spinner if in AI mode
                if (::spinnerDifficulty.isInitialized) {
                    if (viewModel.gameMode.value == GameViewModel.GameMode.PLAYER_VS_AI) {
                        spinnerDifficulty.visibility = View.VISIBLE
                    } else {
                        spinnerDifficulty.visibility = View.GONE
                    }
                }
            }
        })

        // Setup observers
        viewModel.gameStatus.observe(viewLifecycleOwner, Observer { status ->
            // Only update textViewGameStatus if it exists
            textViewGameStatus?.let { textView ->
                textView.text = status

                // Update text color based on whose turn it is or game result
                when {
                    status.contains("X") -> textView.setTextColor(xColor)
                    status.contains("O") -> textView.setTextColor(oColor)
                    else -> textView.setTextColor(textColor)
                }
            }

            // Update game result text if game is over
            if (viewModel.isGameOver.value == true) {
                textViewGameResult.text = status
            }
        })

        viewModel.playerXScore.observe(viewLifecycleOwner, Observer { score ->
            textViewPlayerXScore.text = "Player X: $score"
        })

        viewModel.playerOScore.observe(viewLifecycleOwner, Observer { score ->
            textViewPlayerOScore.text = "Player O: $score"
        })

        // Observe draws if the TextView exists
        if (::textViewDraws.isInitialized) {
            viewModel.drawsCount.observe(viewLifecycleOwner, Observer { draws ->
                textViewDraws.text = "Draws: $draws"
            })
        }

        viewModel.gameMode.observe(viewLifecycleOwner, Observer { mode ->
            when (mode) {
                GameViewModel.GameMode.PLAYER_VS_PLAYER -> radioButtonPvP.isChecked = true
                GameViewModel.GameMode.PLAYER_VS_AI -> radioButtonPvAI.isChecked = true
            }

            // Show/hide difficulty spinner based on game mode
            if (::spinnerDifficulty.isInitialized) {
                spinnerDifficulty.visibility = if (mode == GameViewModel.GameMode.PLAYER_VS_AI) View.VISIBLE else View.GONE
            }
        })

        // Setup click listeners
        buttonReset.setOnClickListener {
            viewModel.resetGame()
        }

        buttonNewGame.setOnClickListener {
            viewModel.resetAllScores()
            Toast.makeText(context, "All scores reset to 0", Toast.LENGTH_SHORT).show()
        }

        radioButtonPvP.setOnClickListener {
            viewModel.setGameMode(GameViewModel.GameMode.PLAYER_VS_PLAYER)
        }

        radioButtonPvAI.setOnClickListener {
            viewModel.setGameMode(GameViewModel.GameMode.PLAYER_VS_AI)
        }
    }

    private fun setupCustomSpinners() {
        try {
            // Use simple default adapters
            val themeAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.theme_options,
                android.R.layout.simple_spinner_item
            )
            themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerTheme.adapter = themeAdapter

            if (::spinnerDifficulty.isInitialized) {
                val difficultyAdapter = ArrayAdapter.createFromResource(
                    requireContext(),
                    R.array.difficulty_levels,
                    android.R.layout.simple_spinner_item
                )
                difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerDifficulty.adapter = difficultyAdapter
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateThemeColors(theme: GameViewModel.GameTheme) {
        when (theme) {
            GameViewModel.GameTheme.MODERN -> {
                textColor = ContextCompat.getColor(requireContext(), R.color.modern_text)
                xColor = ContextCompat.getColor(requireContext(), R.color.modern_x_color)
                oColor = ContextCompat.getColor(requireContext(), R.color.modern_o_color)
                view?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.modern_background))
            }
            GameViewModel.GameTheme.CLASSIC -> {
                textColor = ContextCompat.getColor(requireContext(), R.color.classic_text)
                xColor = ContextCompat.getColor(requireContext(), R.color.classic_x_color)
                oColor = ContextCompat.getColor(requireContext(), R.color.classic_o_color)
                view?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.classic_background))
            }
            GameViewModel.GameTheme.NATURE -> {
                textColor = ContextCompat.getColor(requireContext(), R.color.nature_text)
                xColor = ContextCompat.getColor(requireContext(), R.color.nature_x_color)
                oColor = ContextCompat.getColor(requireContext(), R.color.nature_o_color)
                view?.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.nature_background))
            }
        }
    }

    private fun updateUI() {
        // Update text colors
        textViewPlayerXScore.setTextColor(xColor)
        textViewPlayerOScore.setTextColor(oColor)

        // Update radio button text colors
        radioButtonPvP.setTextColor(textColor)
        radioButtonPvAI.setTextColor(textColor)

        // Update button colors
        buttonReset.backgroundTintList = ColorStateList.valueOf(
            when (viewModel.currentTheme.value) {
                GameViewModel.GameTheme.MODERN -> ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                GameViewModel.GameTheme.CLASSIC -> ContextCompat.getColor(requireContext(), R.color.classic_grid_line)
                GameViewModel.GameTheme.NATURE -> ContextCompat.getColor(requireContext(), R.color.nature_grid_line)
                else -> ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            }
        )
        buttonReset.setTextColor(
            when (viewModel.currentTheme.value) {
                GameViewModel.GameTheme.MODERN, GameViewModel.GameTheme.CLASSIC -> Color.WHITE
                GameViewModel.GameTheme.NATURE -> ContextCompat.getColor(requireContext(), R.color.nature_cell_background)
                else -> Color.WHITE
            }
        )

        // Update new game button colors
        buttonNewGame.backgroundTintList = ColorStateList.valueOf(
            when (viewModel.currentTheme.value) {
                GameViewModel.GameTheme.MODERN -> ContextCompat.getColor(requireContext(), R.color.colorAccent)
                GameViewModel.GameTheme.CLASSIC -> ContextCompat.getColor(requireContext(), R.color.classic_grid_line)
                GameViewModel.GameTheme.NATURE -> ContextCompat.getColor(requireContext(), R.color.nature_grid_line)
                else -> ContextCompat.getColor(requireContext(), R.color.colorAccent)
            }
        )
        buttonNewGame.setTextColor(
            when (viewModel.currentTheme.value) {
                GameViewModel.GameTheme.MODERN, GameViewModel.GameTheme.CLASSIC -> Color.WHITE
                GameViewModel.GameTheme.NATURE -> ContextCompat.getColor(requireContext(), R.color.nature_cell_background)
                else -> Color.WHITE
            }
        )

        // Update draws text color if it exists
        if (::textViewDraws.isInitialized) {
            textViewDraws.setTextColor(textColor)
        }

        // Update textViewGameStatus if it exists
        textViewGameStatus?.setTextColor(textColor)

        // Update game over UI
        textViewGameResult.setTextColor(
            when {
                textViewGameResult.text.contains("X") -> xColor
                textViewGameResult.text.contains("O") -> oColor
                else -> textColor
            }
        )

        // Fix spinner text colors
        (spinnerTheme.selectedView as? TextView)?.setTextColor(Color.WHITE)
        if (::spinnerDifficulty.isInitialized) {
            (spinnerDifficulty.selectedView as? TextView)?.setTextColor(Color.WHITE)
        }
    }

    private fun setupThemeSpinner() {
        spinnerTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Set text color for selected item
                (view as? TextView)?.setTextColor(Color.WHITE)

                val theme = when (position) {
                    0 -> GameViewModel.GameTheme.MODERN
                    1 -> GameViewModel.GameTheme.CLASSIC
                    2 -> GameViewModel.GameTheme.NATURE
                    else -> GameViewModel.GameTheme.MODERN
                }
                viewModel.setGameTheme(theme)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun setupDifficultySpinner() {
        spinnerDifficulty.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Set text color for selected item
                (view as? TextView)?.setTextColor(Color.WHITE)

                val difficulty = when (position) {
                    0 -> GameViewModel.AIDifficulty.EASY
                    1 -> GameViewModel.AIDifficulty.MEDIUM
                    2 -> GameViewModel.AIDifficulty.HARD
                    else -> GameViewModel.AIDifficulty.MEDIUM
                }
                viewModel.setAIDifficulty(difficulty)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        // Initially hide the difficulty spinner if not in AI mode
        spinnerDifficulty.visibility = if (viewModel.gameMode.value == GameViewModel.GameMode.PLAYER_VS_AI) View.VISIBLE else View.GONE
    }
}