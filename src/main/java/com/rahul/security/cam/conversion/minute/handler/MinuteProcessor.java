package com.rahul.security.cam.conversion.minute.handler;

import com.rahul.security.cam.conversion.utils.Params;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

@Component
@StepScope
@Slf4j
public class MinuteProcessor implements ItemProcessor<Path, Path> {

    @Value("#{stepExecution}")
    private StepExecution stepExecution;
    @Autowired
    private Params params;

    @Override
    public Path process(Path item) {
        Path localSource = requireNonNull((Path) stepExecution.getJobExecution().getExecutionContext().get("localSource"));
        Path localDest = requireNonNull((Path) stepExecution.getJobExecution().getExecutionContext().get("localDest"));
        Path destFile = localDest.resolve(localSource.relativize(item));
        if (Files.exists(destFile)) {
            try {
                Path ffProbePath = params.getFfmpegBin().resolve("ffprobe.exe");
                FFprobe ffprobe = new FFprobe(ffProbePath.toString());
                long inputTime = Math.round(ffprobe.probe(item.toString()).getFormat().duration);
                long outputTime = Math.round(ffprobe.probe(destFile.toString()).getFormat().duration);
                if (Math.abs(inputTime - outputTime) < 2) {
                    log.trace("{} Already converted", item);
                    return null; //SKIP
                }
            } catch (Exception e) {
                log.warn("Error checking if file completed successfully {} Continuing with conversion", destFile);
            }
        }
        return item;
    }
}
