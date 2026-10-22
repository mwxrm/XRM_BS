package com.xrm.xrm_bs.service;

import com.xrm.xrm_bs.entity.UserBehaviorLog;
import com.xrm.xrm_bs.repository.UserBehaviorLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserBehaviorLogService {

    private final UserBehaviorLogRepository behaviorLogRepository;

    /**
     * 记录用户行为（异步，不影响主流程）
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, Long itemId, String itemType, String action) {
        UserBehaviorLog log = new UserBehaviorLog();
        log.setUserId(userId);
        log.setItemId(itemId);
        log.setItemType(itemType);
        log.setAction(action);
        behaviorLogRepository.save(log);
    }
}