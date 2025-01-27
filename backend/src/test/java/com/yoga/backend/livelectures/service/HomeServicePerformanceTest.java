package com.yoga.backend.livelectures.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yoga.backend.common.entity.LiveLectures;
import com.yoga.backend.common.entity.MyLiveLecture;
import com.yoga.backend.common.entity.Users;
import com.yoga.backend.config.TestConfig;
import com.yoga.backend.config.TestJpaConfig;
import com.yoga.backend.livelectures.dto.HomeResponseDto;
import com.yoga.backend.livelectures.repository.LiveLectureRepository;
import com.yoga.backend.members.repository.UsersRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest(
    properties = "spring.jpa.hibernate.ddl-auto=create-drop",
    classes = {
        HomeServiceImpl.class,
        LiveLectures.class,
        MyLiveLecture.class,
        Users.class,
        TestConfig.class
    }
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Import({TestConfig.class, TestJpaConfig.class})
class HomeServicePerformanceTest {

    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private LiveLectureRepository liveLectureRepository;
    @Autowired
    private HomeService homeService;

    private int testUserId;
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @BeforeAll
    void setUp() {
        // 테스트 유저
        Users teacher = new Users();
        teacher.setEmail("test@test.com");
        teacher.setNickname("테스트 강사");
        teacher.setRole("TEACHER");
        teacher.setProfile_image_url("http://test.com/image.jpg");
        teacher.setProfile_image_url_small("http://test.com/image_small.jpg");
        teacher.setIsDeleted(false);
        teacher.setPwd(".");
        Users savedTeacher = usersRepository.save(teacher);
        testUserId = savedTeacher.getId();

        // 대량의 강의 데이터 생성
        List<LiveLectures> lectures = createMassiveLectureData(savedTeacher);
        liveLectureRepository.saveAll(lectures);
    }

    private List<LiveLectures> createMassiveLectureData(Users teacher) {
        List<LiveLectures> lectures = new ArrayList<>();
        ZonedDateTime startDate = ZonedDateTime.of(2025, 1, 20, 0, 0, 0, 0, KOREA_ZONE);
        ZonedDateTime endDate = ZonedDateTime.of(2028, 1, 20, 0, 0, 0, 0, KOREA_ZONE);
        int lectureCount = 0;

        ZonedDateTime current = startDate;
        while (current.isBefore(endDate)) {
            // 하루 24시간 * 6개 강의
            for (int hour = 0; hour < 24; hour++) {
                for (int slot = 0; slot < 6; slot++) {
                    LiveLectures lecture = new LiveLectures();
                    lecture.setLiveTitle("강의 " + (++lectureCount));
                    lecture.setLiveContent("테스트 강의 내용 " + lectureCount);
                    lecture.setMaxLiveNum(10);

                    // 시작/종료 날짜 설정 (3일 단위로 반복)
                    ZonedDateTime lectureStart = current;
                    ZonedDateTime lectureEnd = current.plusDays(3);
                    lecture.setStartDate(lectureStart.toInstant());
                    lecture.setEndDate(lectureEnd.toInstant());

                    // 시작/종료 시간 설정 (10분 단위)
                    ZonedDateTime timeStart = current
                        .withHour(hour)
                        .withMinute(slot * 10)
                        .withSecond(0);
                    ZonedDateTime timeEnd = timeStart.plusMinutes(9);
                    lecture.setStartTime(timeStart.toInstant());
                    lecture.setEndTime(timeEnd.toInstant());

                    // 요일 설정
                    lecture.setAvailableDay(current.getDayOfWeek().toString().substring(0, 3));

                    lecture.setRegDate(Instant.now());
                    lecture.setUser(teacher);
                    lecture.setIsOnAir(false);

                    lectures.add(lecture);
                }
            }
            current = current.plusDays(1);
        }
        return lectures;
    }

    @Test
    @DisplayName("대량 데이터 조회 성능 테스트")
    void performanceTest() {
        // 웜업
        homeService.getHomeData(testUserId, 0, 30);

        // 여러 페이지 조회 테스트
        int[] pages = {0, 10, 50, 100};
        Map<Integer, Long> pageTimings = new HashMap<>();

        for (int page : pages) {
            long startTime = System.nanoTime();
            List<HomeResponseDto> results = homeService.getHomeData(testUserId, page, 30);
            long endTime = System.nanoTime();

            pageTimings.put(page, (endTime - startTime) / 1_000_000); // 밀리초 변환

            assertFalse(results.isEmpty(), "Results should not be empty for page " + page);
            assertTrue(results.size() <= 30, "Page size should not exceed 30");

            // 첫 번째 페이지의 첫 번째 결과 로깅
            if (page == 0 && !results.isEmpty()) {
                HomeResponseDto firstResult = results.get(0);
                System.out.println("Sample result: " +
                    "\nTitle: " + firstResult.getLiveTitle() +
                    "\nStart Time: " + firstResult.getStartTime() +
                    "\nEnd Time: " + firstResult.getEndTime() +
                    "\nLecture Day: " + firstResult.getLectureDay());
            }
        }

        // 결과 출력
        for (Map.Entry<Integer, Long> entry : pageTimings.entrySet()) {
            System.out.printf("Page %d took %d ms%n", entry.getKey(), entry.getValue());
        }
    }

    @Test
    @DisplayName("메모리 사용량 테스트")
    void memoryUsageTest() {
        Runtime runtime = Runtime.getRuntime();
        System.gc();

        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        List<HomeResponseDto> results = homeService.getHomeData(testUserId, 0, 100);
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();

        long memoryUsed = (afterMemory - beforeMemory) / 1024 / 1024; // MB 변환
        System.out.printf("Memory used: %d MB for %d results%n", memoryUsed, results.size());

        // 각 페이지당 메모리 사용량
        System.out.printf("Memory per result: %.2f KB%n",
            (double) (afterMemory - beforeMemory) / 1024 / results.size());
    }

    @Test
    @DisplayName("데이터 생성 검증")
    void validateDataTest() {
        List<LiveLectures> lectures = liveLectureRepository.findByUserId(testUserId);

        assertFalse(lectures.isEmpty(), "강의 데이터가 생성되어야 함");
        System.out.println("총 강의 수: " + lectures.size());

        // 샘플 강의 검증
        LiveLectures sampleLecture = lectures.get(0);
        System.out.println("샘플 강의 정보:");
        System.out.println("제목: " + sampleLecture.getLiveTitle());
        System.out.println("시작 날짜: " + sampleLecture.getStartDate());
        System.out.println("종료 날짜: " + sampleLecture.getEndDate());
        System.out.println("시작 시간: " + sampleLecture.getStartTime());
        System.out.println("종료 시간: " + sampleLecture.getEndTime());
        System.out.println("가능 요일: " + sampleLecture.getAvailableDay());
    }

    @AfterAll
    void tearDown() {
        liveLectureRepository.deleteAll();
        usersRepository.deleteById((long) testUserId);
    }
}