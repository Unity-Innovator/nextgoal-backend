package com.example.genz.Service.SavingPlanService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SavingPlanScheduler {

    private final SavingPlanService savingPlanService;

    @Scheduled(cron = "0 0 1 * * *")
    public void processRecurringContributions() {
        int processed = savingPlanService.processDueContributions();
        if (processed > 0) {
            log.info("Processed {} recurring contribution plan(s)", processed);
        }
    }
}
