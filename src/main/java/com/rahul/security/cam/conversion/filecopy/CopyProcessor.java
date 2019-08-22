package com.rahul.security.cam.conversion.filecopy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@StepScope
@Slf4j
public class CopyProcessor implements ItemProcessor<Path, Path> {

/*
    @Value("#{jobExecution}")
    private JobExecution jobExecution;
    @Autowired
    private Params params;
*/

    @Override
    public Path process(Path item) {
        return item;
    }
}
