package com.rahul.security.cam.conversion.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;

@Component
@Slf4j
public class StepCompletionListener extends AbstractPerformanceListener implements StepExecutionListener {

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("{} Step Started", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
            Date startTime = stepExecution.getStartTime();
            Date endTime = stepExecution.getEndTime();
            logPerformance(startTime, endTime, log, stepExecution.getStepName()+" Step");
        }
        return stepExecution.getExitStatus();
    }
}
