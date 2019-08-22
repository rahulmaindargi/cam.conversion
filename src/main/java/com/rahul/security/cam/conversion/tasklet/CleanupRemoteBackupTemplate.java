package com.rahul.security.cam.conversion.tasklet;

import com.rahul.security.cam.conversion.utils.FileUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

@Slf4j
@Component
public class CleanupRemoteBackupTemplate implements ConverterTasklet {
    @Autowired
    FileUtility fileUtility;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("Cleanup RemoteBackup execute");

        Path naasBackup =
                requireNonNull((Path) chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get(
                        "naasBackup"));
        try {
            cleanUpNaasBackup(naasBackup);
        } catch (Throwable t) {
            log.error("Error in Remote Cleanup", t);
            // We can continue processing.
        }
        return RepeatStatus.FINISHED;
    }


    private void cleanUpNaasBackup(Path naasBackup) throws IOException {
        fileUtility.deleteIfExists(naasBackup);
    }
}
