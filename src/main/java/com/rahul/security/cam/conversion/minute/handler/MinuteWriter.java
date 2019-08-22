package com.rahul.security.cam.conversion.minute.handler;

import com.rahul.security.cam.conversion.utils.Params;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Component
@StepScope
@Slf4j
public class MinuteWriter implements ItemWriter<Path> {

    @Value("#{stepExecution}")
    private StepExecution stepExecution;

    @Autowired
    private Params params;

    @Override
    public void write(List<? extends Path> items) {
        items.forEach(item -> log.trace("{} write", item));
        Path localSource = requireNonNull((Path) stepExecution.getJobExecution().getExecutionContext().get("localSource"));
        Path localDest = requireNonNull((Path) stepExecution.getJobExecution().getExecutionContext().get("localDest"));
        //Files.createDirectories(Paths.get("e:", name));
        items.forEach(item -> {
            Path destFile = localDest.resolve(localSource.relativize(item));
            log.debug("{} -> {}", item, destFile);
            try {
                Files.createDirectories(destFile.getParent());
                convert(item.toString(), destFile.toString());
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    private void convert(String inputFile, String outFile) throws IOException {
        Path ffmpegBin = params.getFfmpegBin();

        Path ffmpegPath = ffmpegBin.resolve("ffmpeg.exe");
        Path ffProbePath = ffmpegBin.resolve("ffprobe.exe");

        log.debug("Started converting small file {}", inputFile);
        //long startTime = System.currentTimeMillis();
        FFmpeg ffmpeg = new FFmpeg(ffmpegPath.toString());
        FFprobe ffprobe = new FFprobe(ffProbePath.toString());
        final long totalTime = Math.round(ffprobe.probe(inputFile).getFormat().duration);
        FFmpegBuilder done = new FFmpegBuilder().setInput(inputFile)
                .setVerbosity(FFmpegBuilder.Verbosity.ERROR)
                .overrideOutputFiles(true)
                .addOutput(outFile)
                .addExtraArgs("-crf", "35")
                .setVideoCodec("libx264")
                .done();
        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        // Using the FFmpegProbeResult determine the duration of the input
        // final double duration_ns = in.getFormat().duration * TimeUnit.SECONDS.toNanos(1);
        executor.createJob(done).run();

        long outFileTime = Math.round(ffprobe.probe(outFile).getFormat().duration);
        long diff = Math.abs(outFileTime - totalTime);
        if (diff >= 2) {
            log.error("outFileTime {}  totalTime {}", outFileTime, totalTime);
            throw new RuntimeException("Invalid Conversion outputFileTime " + outFileTime + " Input fileTime " + totalTime);
        }
    }
}
