package com.yoga.backend.config;

import com.yoga.backend.livelectures.repository.LiveLectureRepository;
import com.yoga.backend.livelectures.repository.MyLiveLectureRepository;
import com.yoga.backend.livelectures.service.HomeService;
import com.yoga.backend.livelectures.service.HomeServiceImpl;
import com.yoga.backend.members.repository.UsersRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {
    @Bean
    public HomeService homeService(
        LiveLectureRepository liveLectureRepository,
        MyLiveLectureRepository myLiveLectureRepository,
        UsersRepository usersRepository
    ) {
        return new HomeServiceImpl(liveLectureRepository, myLiveLectureRepository, usersRepository);
    }
}