package com.ssafy.yoganavi.ui.homeUI.myPage.likeTeacher

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.ssafy.yoganavi.databinding.FragmentLikeTeacherBinding
import com.ssafy.yoganavi.ui.core.BaseFragment
import com.ssafy.yoganavi.ui.homeUI.teacher.teacherList.TeacherListFragmentDirections
import com.ssafy.yoganavi.ui.homeUI.teacher.teacherList.teacher.TeacherAdapter
import com.ssafy.yoganavi.ui.utils.EMPTY_LIKE_TEACHER
import com.ssafy.yoganavi.ui.utils.LIKE_TEACHER
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LikeTeacherFragment : BaseFragment<FragmentLikeTeacherBinding>(
    FragmentLikeTeacherBinding::inflate
) {
    val viewModel: LikeTeacherViewModel by viewModels()
    private val teacherAdapter: TeacherAdapter by lazy {
        TeacherAdapter(
            ::navigateToTeacherFragment,
            ::teacherLikeToggle
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setToolbar(false, LIKE_TEACHER, true)
        binding.rvTeacher.adapter = teacherAdapter
        viewModel.getList()
        initCollect()
    }

    private fun initCollect() = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.teacherList.collectLatest {
                checkEmptyList(it, EMPTY_LIKE_TEACHER)
                teacherAdapter.submitList(it)
            }
        }
    }

    private fun navigateToTeacherFragment(userId: Int = -1) {
        val directions = TeacherListFragmentDirections
            .actionTeacherListFragmentToTeacherDetailFragment(userId)
        findNavController().navigate(directions)
    }

    private fun teacherLikeToggle(teacherId: Int = -1) {
        viewModel.teacherLikeToggle(teacherId)
    }
}
