package com.rahul.security.cam.conversion.partitioner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@Component
@Slf4j
@StepScope
public class MinutePartitioner implements Partitioner {
    private static final String PARTITION_KEY = "MinutePartition";
    @Value("#{stepExecution}")
    private StepExecution stepExecution;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> map = new HashMap<>(gridSize);
        Path hourSource = requireNonNull((Path) stepExecution.getExecutionContext().get("hourSource"));
        try {
            Files.newDirectoryStream(hourSource, path -> path.toFile().isFile()).forEach(minuteFile -> {
                ExecutionContext context = new ExecutionContext();
                context.put("minuteFile", minuteFile);
                map.put(PARTITION_KEY + " " + minuteFile.getFileName().toString(), context);
            });
        } catch (IOException e) {
           throw new RuntimeException(e);
        }
        return map;
    }
}
