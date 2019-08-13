package com.rahul.security.cam.conversion.hour.handler;

import com.rahul.security.cam.conversion.utils.ConversionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

@Component
@Slf4j
@JobScope
public class HourAggregator implements StepExecutionAggregator {
    @Value("#{jobExecution}")
    private JobExecution jobExecution;

    @Autowired
    private ConversionUtil conversionUtil;

    @Override
    public void aggregate(StepExecution result, Collection<StepExecution> executions) {
        if (BatchStatus.COMPLETED.equals(result.getStatus())) {
            boolean allCompleted = executions.stream().allMatch(execution -> BatchStatus.COMPLETED.equals(execution.getStatus()));
            if (allCompleted) {
                log.info("All Hours Completed Creating Text file");
                Path localDest = requireNonNull((Path) jobExecution.getExecutionContext().get("localDest"));
                Path finalTxt = localDest.resolve("Final.txt");
                if (Files.exists(finalTxt)) {
                    log.warn("Final.txt already exists {}, Skipping", finalTxt);
                } else {
                    log.info("Started Creating file list for merge");
                    generateFileListFile(localDest, finalTxt);
                }
                jobExecution.getExecutionContext().put("FinalTxt", finalTxt);
            }
        }
    }

    private void generateFileListFile(Path fileListSource, Path finalTxt) {
        String newLine = System.getProperty("line.separator");
        try {
            conversionUtil.onOrderedStream(conversionUtil.childrenDirs(fileListSource), dir -> {
                try {
                    conversionUtil.onOrderedStream(conversionUtil.childrenFiles(dir), (file) -> {
                        try {
                            Files.write(finalTxt, ("file '" + file.toAbsolutePath().toString() + "'" + newLine).getBytes(), StandardOpenOption.APPEND,
                                    StandardOpenOption.CREATE);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
