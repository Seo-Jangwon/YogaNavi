package com.ssafy.yoganavi.data.source.dto.teacher

import com.google.gson.annotations.SerializedName

data class TeacherData(

    @SerializedName("nickname")
    val teacherName: String,
    @SerializedName("profileImageUrl")
    val teacherProfile: String,
    val teacherSmallProfile: String,
    @SerializedName("id")
    val teacherId: Int,
    @SerializedName("hashtags")
    val hashtags: List<String>,
    val liked: Boolean,
    val likes: Int,
)