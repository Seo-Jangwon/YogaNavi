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
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
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
        System.out.println("\n=== 웜업 실행 ===");
        homeService.getHomeData(testUserId, 0, 30);

        // 여러 페이지 조회 테스트
        int[] pages = {0, 10, 50, 100};
        Map<Integer, List<Long>> pageTimings = new HashMap<>();

        System.out.println("\n=== 성능 테스트 시작 ===");
        int iterations = 5; // 각 페이지당 반복 측정 횟수

        for (int page : pages) {
            List<Long> timings = new ArrayList<>();
            List<Integer> resultSizes = new ArrayList<>();

            for (int i = 0; i < iterations; i++) {
                System.gc(); // 각 반복 전 GC 실행

                long startTime = System.nanoTime();
                List<HomeResponseDto> results = homeService.getHomeData(testUserId, page, 30);
                long endTime = System.nanoTime();
                long executionTime = (endTime - startTime) / 1_000_000; // 밀리초 변환

                timings.add(executionTime);
                resultSizes.add(results.size());

                assertFalse(results.isEmpty(), "Results should not be empty for page " + page);
                assertTrue(results.size() <= 30, "Page size should not exceed 30");

                // 첫 번째 반복의 첫 결과만 로깅
                if (i == 0 && !results.isEmpty()) {
                    HomeResponseDto firstResult = results.get(0);
                    System.out.println("\nPage " + page + " 샘플 데이터:");
                    System.out.println("- 제목: " + firstResult.getLiveTitle());
                    System.out.println("- 강사: " + firstResult.getNickname());
                    System.out.println("- 시작 시간: " + firstResult.getStartTime());
                    System.out.println("- 종료 시간: " + firstResult.getEndTime());
                    System.out.println("- 요일: " + firstResult.getLectureDay());
                    System.out.println("- 최대 수강 인원: " + firstResult.getMaxLiveNum());
                }
            }
            pageTimings.put(page, timings);

            // 각 페이지의 통계 출력
            double avgTime = timings.stream().mapToLong(Long::valueOf).average().orElse(0.0);
            long minTime = timings.stream().mapToLong(Long::valueOf).min().orElse(0);
            long maxTime = timings.stream().mapToLong(Long::valueOf).max().orElse(0);
            double stdDev = calculateStdDev(timings, avgTime);

            System.out.println(String.format("\n=== 페이지 %d 성능 통계 ===", page));
            System.out.println(String.format("평균 실행 시간: %.2f ms", avgTime));
            System.out.println(String.format("최소 실행 시간: %d ms", minTime));
            System.out.println(String.format("최대 실행 시간: %d ms", maxTime));
            System.out.println(String.format("표준 편차: %.2f ms", stdDev));
            System.out.println(String.format("결과 건수: %d", resultSizes.get(0)));
        }
    }

    private double calculateStdDev(List<Long> values, double mean) {
        double sumSquaredDiff = values.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .sum();
        return Math.sqrt(sumSquaredDiff / values.size());
    }

    @Test
    @DisplayName("메모리 사용량 테스트")
    void memoryUsageTest() {
        Runtime runtime = Runtime.getRuntime();
        System.out.println("\n=== 메모리 사용량 테스트 시작 ===");

        // 초기 메모리 상태 출력
        System.out.println("초기 메모리 상태:");
        printMemoryStats(runtime);

        System.gc();
        try {
            Thread.sleep(100); // GC 완료 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 다양한 페이지 크기로 테스트
        int[] pageSizes = {30, 50, 100};

        for (int pageSize : pageSizes) {
            long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
            List<HomeResponseDto> results = homeService.getHomeData(testUserId, 0, pageSize);
            long afterMemory = runtime.totalMemory() - runtime.freeMemory();

            long memoryUsed = afterMemory - beforeMemory;

            System.out.println(String.format("\n=== 페이지 크기: %d ===", pageSize));
            System.out.println(String.format("총 메모리 사용량: %.2f MB", memoryUsed / 1024.0 / 1024.0));
            System.out.println(String.format("결과 건수: %d", results.size()));
            System.out.println(String.format("레코드당 평균 메모리: %.2f KB",
                (memoryUsed / 1024.0) / results.size()));

            // 현재 메모리 상태 출력
            System.out.println("\n현재 메모리 상태:");
            printMemoryStats(runtime);

            System.gc();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void printMemoryStats(Runtime runtime) {
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = total - free;
        long max = runtime.maxMemory();

        System.out.println(String.format("최대 메모리: %.2f MB", max / 1024.0 / 1024.0));
        System.out.println(String.format("할당된 메모리: %.2f MB", total / 1024.0 / 1024.0));
        System.out.println(String.format("사용중인 메모리: %.2f MB", used / 1024.0 / 1024.0));
        System.out.println(String.format("가용 메모리: %.2f MB", free / 1024.0 / 1024.0));
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

    @Test
    @DisplayName("동시 접속 부하 테스트")
    void concurrentLoadTest() {

        int[] userCounts = {10, 20, 30, 50};  // 점진적 부하 증가
        int requestsPerUser = 5;               // 사용자당 요청 수
        int timeoutSeconds = 60;               // 전체 테스트 타임아웃

        // 웜업
        System.out.println("\n=== 부하 테스트 웜업 ===");
        homeService.getHomeData(testUserId, 0, 30);

        for (int numberOfUsers : userCounts) {
            System.out.println("\n=== " + numberOfUsers + "명 동시 사용자 테스트 시작 ===");

            ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(numberOfUsers, Runtime.getRuntime().availableProcessors() * 2)
            );
            CountDownLatch latch = new CountDownLatch(numberOfUsers * requestsPerUser);
            ConcurrentHashMap<Integer, List<Long>> userTimings = new ConcurrentHashMap<>();
            AtomicInteger failedRequests = new AtomicInteger(0);

            long startTime = System.currentTimeMillis();

            // 각 사용자별 테스트
            for (int userId = 0; userId < numberOfUsers; userId++) {
                final int currentUserId = userId;
                userTimings.put(currentUserId, new CopyOnWriteArrayList<>());

                for (int request = 0; request < requestsPerUser; request++) {
                    executor.submit(() -> {
                        try {
                            long requestStart = System.nanoTime();
                            List<HomeResponseDto> results = homeService.getHomeData(testUserId, 0,
                                30);
                            long requestEnd = System.nanoTime();

                            userTimings.get(currentUserId)
                                .add((requestEnd - requestStart) / 1_000_000); // 밀리초 변환

                            assertFalse(results.isEmpty(), "Results should not be empty");
                            assertTrue(results.size() <= 30, "Page size should not exceed 30");

                        } catch (Exception e) {
                            failedRequests.incrementAndGet();
                            System.err.println(
                                "Error in user " + currentUserId + ": " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });
                }
            }

            // 모든 요청 완료 대기
            try {
                boolean completed = latch.await(timeoutSeconds, TimeUnit.SECONDS);
                if (!completed) {
                    System.err.println("테스트가 타임아웃으로 종료됨");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("테스트가 중단됨");
            }

            long totalTime = System.currentTimeMillis() - startTime;

            // 결과 분석 및 출력
            System.out.println("\n=== " + numberOfUsers + "명 테스트 결과 ===");
            System.out.println("총 실행 시간: " + totalTime + "ms");
            System.out.println("실패한 요청 수: " + failedRequests.get());

            // 응답 시간 통계
            List<Long> allTimings = userTimings.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

            if (!allTimings.isEmpty()) {
                DoubleSummaryStatistics stats = allTimings.stream()
                    .mapToDouble(Long::doubleValue)
                    .summaryStatistics();

                double standardDeviation = calculateStdDev(allTimings, stats.getAverage());

                System.out.println(String.format("\n응답 시간 통계:"));
                System.out.println(String.format("평균: %.2f ms", stats.getAverage()));
                System.out.println(String.format("최소: %.2f ms", stats.getMin()));
                System.out.println(String.format("최대: %.2f ms", stats.getMax()));
                System.out.println(String.format("표준편차: %.2f ms", standardDeviation));

                // 백분위 응답 시간
                List<Long> sortedTimings = new ArrayList<>(allTimings);
                Collections.sort(sortedTimings);

                System.out.println("\n백분위 응답 시간:");
                System.out.println(String.format("50th percentile: %d ms",
                    sortedTimings.get((int) (sortedTimings.size() * 0.5))));
                System.out.println(String.format("90th percentile: %d ms",
                    sortedTimings.get((int) (sortedTimings.size() * 0.9))));
                System.out.println(String.format("95th percentile: %d ms",
                    sortedTimings.get((int) (sortedTimings.size() * 0.95))));
                System.out.println(String.format("99th percentile: %d ms",
                    sortedTimings.get((int) (sortedTimings.size() * 0.99))));

                // 검증
                assertTrue(failedRequests.get() == 0, "모든 요청이 성공해야 함");
                assertTrue(stats.getAverage() < 1000, "평균 응답시간이 1초 미만이어야 함");
            }

            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }

            System.gc();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    @AfterAll
    void tearDown() {
        liveLectureRepository.deleteAll();
        usersRepository.deleteById((long) testUserId);
    }
}