package com.rahul.security.cam.conversion.minute.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@StepScope
@Slf4j
public class MinuteReader implements ItemReader<Path> {
    @Value("#{stepExecutionContext[minuteFile]}")
    private Path minuteFile;


    @Override
    public Path read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        /*After returning minute file 2nd time should return null*/
        var result = minuteFile;
        if (minuteFile != null) {
            log.trace("{} Read", minuteFile.toString());
            minuteFile = null;
        }
        return result;
    }
}
