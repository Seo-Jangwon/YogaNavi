package com.ssafy.yoganavi.ui.homeUI.myPage.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.ssafy.yoganavi.R
import com.ssafy.yoganavi.data.source.mypage.ProfileData
import com.ssafy.yoganavi.databinding.FragmentProfileBinding
import com.ssafy.yoganavi.ui.core.BaseFragment
import com.ssafy.yoganavi.ui.utils.MY_PAGE
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ProfileFragment : BaseFragment<FragmentProfileBinding>(FragmentProfileBinding::inflate) {
    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setToolbar(true, MY_PAGE, false)
        initListener()
        getProfileData()
    }

    private fun getProfileData() = viewModel.getProfileData(::bindData)

    private suspend fun bindData(profileData: ProfileData) = withContext(Dispatchers.Main) {
        with(binding) {
            tvName.text = profileData.nickname
            Glide.with(requireContext())
                .load(profileData.imageUrl)
                .into(ivIcon)

            if (profileData.teacher) {
                tvManagementVideo.visibility = View.VISIBLE
                tvManagementLive.visibility = View.VISIBLE
                tvRegisterNotice.visibility = View.VISIBLE
            }
        }
    }

    private fun initListener() {
        with(binding) {
            tvLikeTeacher.setOnClickListener { findNavController().navigate(R.id.action_profileFragment_to_likeTeacherFragment) }
            tvLikeLecture.setOnClickListener { findNavController().navigate(R.id.action_profileFragment_to_likeLectureFragment) }
            tvMyList.setOnClickListener { findNavController().navigate(R.id.action_profileFragment_to_myListFragment) }
            tvModify.setOnClickListener { findNavController().navigate(R.id.action_profileFragment_to_modifyFragment) }
            tvManagementLive.setOnClickListener { findNavController().navigate(R.id.action_profileFragment_to_managementLiveFragment) }
            tvManagementVideo.setOnClickListener { findNavController().navigate(R.id.action_profileFragment_to_managementVideoFragment) }
            tvRegisterNotice.setOnClickListener { findNavController().navigate(R.id.action_profileFragment_to_noticeFragment) }
            tvLogout.setOnClickListener {
                viewModel.clearUserData()
                logout()
            }
        }
    }
}