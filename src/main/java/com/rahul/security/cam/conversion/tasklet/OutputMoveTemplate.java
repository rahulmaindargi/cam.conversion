package com.rahul.security.cam.conversion.tasklet;

import com.rahul.security.cam.conversion.utils.FileUtility;
import com.rahul.security.cam.conversion.utils.Params;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static java.util.Objects.requireNonNull;

@Slf4j
@Component
public class OutputMoveTemplate implements ConverterTasklet {
    @Autowired
    private Params params;

    @Autowired
    private FileUtility fileUtility;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("Output Move execute");
        ExecutionContext jobExecutionContext = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        try {
            Path localDestFile = requireNonNull((Path) jobExecutionContext.get("localDestFile"));
            Files.move(localDestFile, params.getBasePath().resolve(localDestFile.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (Throwable t) {
            return RepeatStatus.CONTINUABLE;
        }
        try {
            Path localSource = (Path) jobExecutionContext.get("localSource");
            Path localDest = (Path) jobExecutionContext.get("localDest");
            fileUtility.deleteIfExists(localSource);
            fileUtility.deleteIfExists(localDest);
        } catch (Exception e) {
            log.error("Local Clean up failed.",e);
        }
        return RepeatStatus.FINISHED;
    }
}
