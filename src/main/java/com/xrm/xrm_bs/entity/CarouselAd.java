package com.xrm.xrm_bs.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "carousel_ads")
@Data
public class CarouselAd {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private String linkUrl;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean enabled = true;
}