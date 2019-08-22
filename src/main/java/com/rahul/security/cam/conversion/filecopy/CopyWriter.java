package com.rahul.security.cam.conversion.filecopy;

import com.rahul.security.cam.conversion.utils.Params;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Component
@StepScope
@Slf4j
public class CopyWriter implements ItemWriter<Path> {

    /*@Value("#{jobExecution}")
    private JobExecution jobExecution;
    @Value("#{jobExecutionContext}")
    private ExecutionContext executionContext;*/
    @Value("#{jobExecutionContext['localSource']}")
    private Path destination;
    @Value("#{jobExecutionContext['naasBackup']}")
    private Path source;
    @Autowired
    private Params params;

    @Override
    public void write(List<? extends Path> items) {
        // ExecutionContext executionContext = jobExecution.getExecutionContext();
        //  Path destination = requireNonNull((Path) executionContext.get("localSource"));
        // Path source = requireNonNull((Path) executionContext.get("naasBackup"));
        items.forEach(file -> {
            log.trace("{} write", file);
            try {
                Files.copy(file, destination.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error("Error while copying file {}", file, e);
            }
        });
    }
}
