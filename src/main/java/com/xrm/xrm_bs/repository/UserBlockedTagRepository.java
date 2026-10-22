package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.UserBlockedTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBlockedTagRepository extends JpaRepository<UserBlockedTag, Long> {
    List<UserBlockedTag> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}