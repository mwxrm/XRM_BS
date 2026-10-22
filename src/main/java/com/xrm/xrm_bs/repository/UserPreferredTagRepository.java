package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.UserPreferredTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPreferredTagRepository extends JpaRepository<UserPreferredTag, Long> {
    List<UserPreferredTag> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}