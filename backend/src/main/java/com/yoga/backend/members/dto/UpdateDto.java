package com.yoga.backend.members.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateDto {
    private String email;
    private String password;
    private String nickname;
    private boolean isTeacher;
    private int authnumber;
    private String imageUrl;
}