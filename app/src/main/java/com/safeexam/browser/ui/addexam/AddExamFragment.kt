package com.safeexam.browser.ui.addexam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.safeexam.browser.databinding.FragmentAddExamBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddExamFragment : Fragment() {

    private var _binding: FragmentAddExamBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddExamViewModel by viewModels()
    private val args: AddExamFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-fill URL if coming from QR scanner
        args.prefilledUrl?.let { url ->
            if (url.isNotEmpty()) binding.etUrl.setText(url)
        }

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveState.collect { state ->
                    when (state) {
                        is SaveState.Idle    -> setLoading(false)
                        is SaveState.Saving  -> setLoading(true)
                        is SaveState.Success -> {
                            setLoading(false)
                            Toast.makeText(
                                requireContext(),
                                "Ujian berhasil disimpan",
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().navigateUp()
                        }
                        is SaveState.Error -> {
                            setLoading(false)
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            viewModel.saveExam(
                name = binding.etName.text.toString(),
                url  = binding.etUrl.text.toString()
            )
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSave.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
