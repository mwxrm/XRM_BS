package com.xrm.xrm_bs.repository;

import com.xrm.xrm_bs.entity.CarouselAd;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarouselAdRepository extends JpaRepository<CarouselAd, Long> {
    List<CarouselAd> findByEnabledTrueOrderBySortOrderAsc();
}