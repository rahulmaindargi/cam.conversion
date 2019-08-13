package com.rahul.security.cam.conversion.tasklet;

import com.rahul.security.cam.conversion.utils.Params;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

@Slf4j
@Component
public class FinalMergeTemplate implements ConverterTasklet {

    @Autowired
    private Params params;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("FinalMergeTemplate execute");
        try {
            ExecutionContext jobExecutionContext = chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
            Path finalTxt = requireNonNull((Path) jobExecutionContext.get("FinalTxt"));
            Path localDestFile = requireNonNull((Path) jobExecutionContext.get("localDestFile"));
            convertConcat(finalTxt, localDestFile);
        } catch (Throwable e) {
            log.error("Error while generating outfile. Retrying", e);
            return RepeatStatus.CONTINUABLE;
        }
        return RepeatStatus.FINISHED;
    }

    private void convertConcat(Path inputTxtFile, Path outputFile) {
        try {
            Path ffmpegBin = params.getFfmpegBin();
            Path ffmpegPath = ffmpegBin.resolve("ffmpeg.exe");
            Path ffProbePath = ffmpegBin.resolve("ffprobe.exe");
            FFmpeg ffmpeg = new FFmpeg(ffmpegPath.toString());
            FFprobe ffprobe = new FFprobe(ffProbePath.toString());
            final long oneFileTime = Files.lines(inputTxtFile)
                    .map(file -> file.substring(6, file.length() - 1)).findFirst().map(file -> {
                        try {
                            return Math.round(ffprobe.probe(file).getFormat().duration);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }).orElse(0L);
            final long totalTime = Files.lines(inputTxtFile).count() * oneFileTime;
            FFmpegBuilder done = new FFmpegBuilder().setInput(inputTxtFile.toString())
                    .setVerbosity(FFmpegBuilder.Verbosity.ERROR)
                    .addExtraArgs("-safe", "0")
                    .setFormat("concat")
                    .overrideOutputFiles(true)
                    .addOutput(outputFile.toString())
                    // .setVideoCodec("libx264")
                    //.setAudioCodec("libfdk_aac")
                    //.setTargetSize(8589934592l)
                    .addExtraArgs("-c", "copy")
                    //.disableAudio()
                    .done();

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            executor.createJob(done).run();
            long outFileTime = Math.round(ffprobe.probe(outputFile.toString()).getFormat().duration);
            long diff = Math.abs(outFileTime - totalTime);
            if (diff >= 2 && totalTime > outFileTime) {
                log.error("outFileTime {}  totalTime {}", outFileTime, totalTime);
                throw new RuntimeException("Concat failed outFileTime " + outFileTime + "  totalTime " + totalTime);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
