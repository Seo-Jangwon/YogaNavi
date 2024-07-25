package com.yoga.backend.mypage.recorded;


import com.yoga.backend.mypage.recorded.dto.DeleteDto;
import com.yoga.backend.mypage.recorded.dto.LectureCreationStatus;
import com.yoga.backend.mypage.recorded.dto.LectureDto;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

public interface RecordedService {

    List<LectureDto> getMyLectures(int userId);

    List<LectureDto> getLikeLectures(int userId);

    void saveLecture(LectureDto lectureDto);

    LectureDto getLectureDetails(Long recordedId, int userId);

    boolean updateLecture(LectureDto lectureDto);

    void deleteLectures(DeleteDto deleteDto, int userId);

    boolean toggleLike(Long recordedId, int userId);

}