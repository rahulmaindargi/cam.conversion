package com.rahul.security.cam.conversion.tasklet;

import com.rahul.security.cam.conversion.utils.FileUtility;
import com.rahul.security.cam.conversion.utils.Params;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;

import static java.util.Objects.requireNonNull;

@Slf4j
@Component
public class CopyToLocalTemplate implements ConverterTasklet {
    @Autowired
    FileUtility fileUtility;
    @Autowired
    private Params params;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("CopyToLocalTemplate execute");
        ExecutionContext jobExecutionContext = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
        String magicName = (String) jobExecutionContext.get("magicName");
        Path naasBackup = requireNonNull((Path) jobExecutionContext.get("naasBackup"));
        Path localSource = Paths.get("e:", magicName + "_Source");
        try {
            if (!Files.exists(naasBackup)) {
                Files.createDirectory(naasBackup);
            }
            fileUtility.copyDirRecursive(naasBackup, localSource);
        } catch (Throwable t) {
            return RepeatStatus.CONTINUABLE;
        }
        try {
            deleteOldFiles();
            cleanUpNaasBackup(naasBackup);
        } catch (Throwable t) {
            log.error("Error in Remote Cleanup", t);
            // We can continue processing.
        }
        jobExecutionContext.put("localSource", localSource);
        jobExecutionContext.put("localDest", Paths.get("e:", magicName));
        jobExecutionContext.put("localDestFile", Paths.get("e:", magicName, magicName + ".mp4"));
        return RepeatStatus.FINISHED;
    }

    private void deleteOldFiles() throws IOException {
        Files.newDirectoryStream(params.getBasePath(), this::isOneMonthOldMp4File).forEach(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.error("Error deleting file {}", path, e);
            }
        });
    }

    private boolean isOneMonthOldMp4File(Path path) throws IOException {
        ZoneId zoneId = params.getZoneId();
        LocalDate asOfDate = params.getAsOfDate();
        return Files.isRegularFile(path) && "mp4".equalsIgnoreCase(FilenameUtils.getExtension(path.getFileName().toString()))
                && Files.getLastModifiedTime(path).toInstant().atZone(zoneId).isBefore(asOfDate.minusMonths(1).atStartOfDay(zoneId));
    }

    private void cleanUpNaasBackup(Path naasBackup) throws IOException {
        fileUtility.deleteIfExists(naasBackup);
    }
}
