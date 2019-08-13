package com.rahul.security.cam.conversion.partitioner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.configuration.annotation.JobScope;
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
@JobScope
public class HourPartitioner implements Partitioner {
    private static final String PARTITION_KEY = "HourPartition";
    @Value("#{jobExecution}")
    private JobExecution jobExecution;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> map = new HashMap<>(gridSize);
        Path localSource = requireNonNull((Path) jobExecution.getExecutionContext().get("localSource"));
        try {
            Files.newDirectoryStream(localSource, path -> path.toFile().isDirectory()).forEach(hourDir -> {
                ExecutionContext context = new ExecutionContext();
                context.put("hourSource", hourDir);
                map.put(PARTITION_KEY + " " + hourDir.getFileName().toString(), context);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return map;
    }
}
