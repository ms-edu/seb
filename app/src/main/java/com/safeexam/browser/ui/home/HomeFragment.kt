package com.safeexam.browser.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.safeexam.browser.R
import com.safeexam.browser.data.db.entity.Exam
import com.safeexam.browser.databinding.FragmentHomeBinding
import com.safeexam.browser.ui.exam.ExamActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: ExamListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = ExamListAdapter(
            onExamClick = { exam -> launchExam(exam) },
            onDeleteClick = { exam -> viewModel.deleteExam(exam) }
        )
        binding.recyclerExams.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HomeFragment.adapter
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.exams.collect { exams ->
                    adapter.submitList(exams)
                    binding.emptyState.visibility =
                        if (exams.isEmpty()) View.VISIBLE else View.GONE
                    binding.recyclerExams.visibility =
                        if (exams.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun setupListeners() {
        binding.fabAddExam.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_addExam)
        }
        binding.btnScanQr.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_qrScanner)
        }
    }

    private fun launchExam(exam: Exam) {
        val intent = Intent(requireContext(), ExamActivity::class.java).apply {
            putExtra(ExamActivity.EXTRA_EXAM_ID, exam.id)
            putExtra(ExamActivity.EXTRA_EXAM_URL, exam.url)
            putExtra(ExamActivity.EXTRA_EXAM_NAME, exam.namaUjian)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
