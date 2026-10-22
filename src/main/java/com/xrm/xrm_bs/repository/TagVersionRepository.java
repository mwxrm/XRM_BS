package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.TagVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TagVersionRepository extends JpaRepository<TagVersion, Long> {
    List<TagVersion> findByTagIdOrderByVersionDesc(Long tagId);
    Integer countByTagId(Long tagId);
}