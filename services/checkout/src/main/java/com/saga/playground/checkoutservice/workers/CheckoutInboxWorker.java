package com.saga.playground.checkoutservice.workers;

import com.saga.playground.checkoutservice.constants.CronIntervalConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckoutInboxWorker {

    // @Scheduled(cron = CronIntervalConstant.EVERY_01_MINUTE)
    // @Async
    // public void pullMessage() {
    //     log.info("SCHEDULED_TASK start fetchCrmAvailableCommunitiesForBooking");
    // }

}
