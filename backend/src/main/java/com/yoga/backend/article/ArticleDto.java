package com.yoga.backend.article;

import lombok.Data;

/**
 * 게시글(공지사항) DTO 클래스
 */
@Data
public class ArticleDto {
    private String title; // 게시글 제목
    private String content; // 게시글 내용
    private String imageUrl; // 이미지 URL
}
