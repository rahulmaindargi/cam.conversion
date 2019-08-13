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

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class RemoteMoveTemplate implements ConverterTasklet {

    @Autowired
    FileUtility fileUtility;
    @Autowired
    private Params params;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("RemoteMoveTemplate execute");
        ExecutionContext jobExecutionContext = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        Path basePath = params.getBasePath();
        Path recordings = basePath.resolve(params.getRecordingFolderName());
        LocalDate asOfDate = params.getAsOfDate();
        ZoneId zoneId = params.getZoneId();
        String name = asOfDate.minusDays(1).format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));
        Path naasBackup = basePath.resolve(name);
        ZonedDateTime startOfDay = asOfDate.atStartOfDay(zoneId);
        fileUtility.filterAndMoveDirectoryContains(recordings, naasBackup,
                path -> path.toFile().isDirectory() && fileUtility.isPathModifiedBefore(path, startOfDay, zoneId));
        jobExecutionContext.put("naasBackup", naasBackup);
        jobExecutionContext.put("magicName", name);
        return RepeatStatus.FINISHED;
    }
}
