package com.yoga.backend.members.repository;

import jakarta.persistence.LockModeType;
import java.util.List;

import com.yoga.backend.common.entity.Users;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersRepository extends CrudRepository<Users, Long> {

    Optional<Users> findById(int id);

    Optional<Users> findByEmail(String email);

    Optional<Users> findByNickname(String nickname);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Users u WHERE u.email = :email")
    Optional<Users> findByEmailWithLock(String email);

    @Query("SELECT u FROM Users u JOIN FETCH u.hashtags h WHERE h.name = :hashtagName")
    List<Users> findUsersByHashtag(@Param("hashtagName") String hashtagName);
}