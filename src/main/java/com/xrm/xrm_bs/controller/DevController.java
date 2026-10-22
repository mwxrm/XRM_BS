package com.xrm.xrm_bs.controller;

import com.xrm.xrm_bs.service.TestDataService;
import com.xrm.xrm_bs.service.HotRankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@PreAuthorize("hasRole('ADMIN')")
//@Profile("dev")
//@Profile("default")
public class DevController {

    @Autowired
    private TestDataService testDataService;

    @Autowired
    private HotRankService hotRankService;

    @PostMapping("/generate-test-data")
    public ResponseEntity<Map<String, Object>> generateData() {
        Map<String, Object> result = testDataService.generateTestData();
        // 生成数据后立即更新热榜
        hotRankService.computeHotRanks();
        result.put("hotRankUpdated", true);
        return ResponseEntity.ok(result);
    }
}