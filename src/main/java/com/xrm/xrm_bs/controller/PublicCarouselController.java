package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.entity.CarouselAd;
import com.xrm.xrm_bs.repository.CarouselAdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicCarouselController {

    private final CarouselAdRepository carouselAdRepository;

    @GetMapping("/carousel")
    public ResponseEntity<List<CarouselAd>> getCarousel() {
        // 只返回启用的广告，按排序字段排序
        List<CarouselAd> ads = carouselAdRepository.findByEnabledTrueOrderBySortOrderAsc();
        return ResponseEntity.ok(ads);
    }
}