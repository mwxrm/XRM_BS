package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.entity.CarouselAd;
import com.xrm.xrm_bs.repository.CarouselAdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/carousel")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CarouselAdController {

    private final CarouselAdRepository carouselAdRepository;

    @GetMapping
    public List<CarouselAd> list() {
        return carouselAdRepository.findAll();
    }

    @PostMapping
    public CarouselAd create(@RequestBody CarouselAd ad) {
        return carouselAdRepository.save(ad);
    }

    @PutMapping("/{id}")
    public CarouselAd update(@PathVariable Long id, @RequestBody CarouselAd ad) {
        CarouselAd existing = carouselAdRepository.findById(id).orElseThrow();
        existing.setImageUrl(ad.getImageUrl());
        existing.setLinkUrl(ad.getLinkUrl());
        existing.setSortOrder(ad.getSortOrder());
        existing.setEnabled(ad.getEnabled());
        return carouselAdRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        carouselAdRepository.deleteById(id);
    }
}