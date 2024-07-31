package com.ssafy.yoganavi.ui.homeUI.teacher.teacherDetail.teacherDetail

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ssafy.yoganavi.data.source.dto.lecture.LectureData
import com.ssafy.yoganavi.databinding.ListItemTeacherLectureRecycleBinding
import com.ssafy.yoganavi.ui.homeUI.teacher.teacherDetail.teacherDetail.lecture.TeacherDetailLectureAdapter

class TeacherDetailViewHolder(
    private val binding: ListItemTeacherLectureRecycleBinding
) : ViewHolder(binding.root) {
    private val teacherDetailLectureAdapter = TeacherDetailLectureAdapter()
    fun bind(lectureDataList: List<LectureData>) {
        binding.rvLecture.adapter = teacherDetailLectureAdapter
        teacherDetailLectureAdapter.submitList(lectureDataList)
    }
}