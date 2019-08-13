package com.rahul.security.cam.conversion.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class JobCompletionListener extends AbstractPerformanceListener implements JobExecutionListener {
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("{} Job Started", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            Date startTime = jobExecution.getStartTime();
            Date endTime = jobExecution.getEndTime();
            logPerformance(startTime, endTime, log, jobExecution.getJobInstance().getJobName() + " Job");
            taskExecutor.shutdown();
        }
    }
}
