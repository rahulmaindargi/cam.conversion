package com.rahul.security.cam.conversion.config;

import com.rahul.security.cam.conversion.filecopy.CopyProcessor;
import com.rahul.security.cam.conversion.filecopy.CopyReader;
import com.rahul.security.cam.conversion.filecopy.CopyWriter;
import com.rahul.security.cam.conversion.hour.handler.HourAggregator;
import com.rahul.security.cam.conversion.listener.CopyFileRetryListener;
import com.rahul.security.cam.conversion.listener.JobCompletionListener;
import com.rahul.security.cam.conversion.listener.MinuteFileRetryListener;
import com.rahul.security.cam.conversion.minute.handler.MinuteProcessor;
import com.rahul.security.cam.conversion.minute.handler.MinuteReader;
import com.rahul.security.cam.conversion.minute.handler.MinuteWriter;
import com.rahul.security.cam.conversion.partitioner.HourPartitioner;
import com.rahul.security.cam.conversion.partitioner.MinutePartitioner;
import com.rahul.security.cam.conversion.tasklet.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.nio.file.Path;

@Configuration
@EnableBatchProcessing
@Slf4j
//@EnableScheduling
public class Config {

    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    @Autowired
    private StepBuilderFactory stepBuilderFactory;
    @Autowired
    private RemoteMoveTemplate remoteMoveTemplate;
    @Autowired
    private StepExecutionListener stepListener;
    @Autowired
    private CopyToLocalTemplate copyToLocalTemplate;
    @Autowired
    private MinutePartitioner minutePartitioner;


    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(3);
        taskExecutor.setMaxPoolSize(3);
        taskExecutor.setThreadNamePrefix("my thread");
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Bean
    public Step hourPartition(Step convertHourStep, HourAggregator hourAggregator, HourPartitioner hourPartitioner) {
        return stepBuilderFactory.get("hourPartitionStep")
                .partitioner("hourStep", hourPartitioner)
                .step(convertHourStep)
                .aggregator(hourAggregator)
                .build();
    }

    @Bean
    public Step convertHourStep(Step convertMinuteStep) {
        return stepBuilderFactory.get("minutePartitionStep")
                .partitioner("minuteStep", minutePartitioner)
                .step(convertMinuteStep)
                .taskExecutor(taskExecutor())
                .gridSize(3)
                .listener(stepListener)
                .build();
    }

    @Bean
    public Step convertMinuteStep(MinuteFileRetryListener minuteFileRetryListener, MinuteProcessor minuteProcessor,
                                  MinuteWriter minuteWriter,
                                  MinuteReader minuteReader) {
        return stepBuilderFactory.get("minuteConversion").<Path, Path>chunk(1)
                .faultTolerant().retryPolicy(new AlwaysRetryPolicy()).retry(Throwable.class).listener(minuteFileRetryListener)
                .reader(minuteReader).processor(minuteProcessor).writer(minuteWriter).build();
    }

    @Bean
    public Job dayConversionJob(JobCompletionListener jobListner, Step hourPartition, Step finalMerge, Step moveOutput,
                                Flow parallelSteps) {
        return jobBuilderFactory.get("DayConversionJob").incrementer(new RunIdIncrementer()).listener(jobListner).flow(remoteMove())
                .next(parallelSteps)
                .next(hourPartition).next(finalMerge).next(moveOutput).build().build();
    }

    @Bean
    public Flow parallelSteps(Flow copyToLocalChunk, Flow cleanupFlow) {
        return new FlowBuilder<Flow>("parallelSteps").split(new SimpleAsyncTaskExecutor()).add(copyToLocalChunk, cleanupFlow).build();
    }

    @Bean
    public Flow cleanupFlow(CleanupStepTemplate cleanupStepTemplate) {
        return new FlowBuilder<Flow>("cleanupFlow").start(stepBuilderFactory.get("Cleanup").listener(stepListener)
                .tasklet(cleanupStepTemplate).build()).build();
    }

    @Bean
    public Step moveOutput(OutputMoveTemplate outputMoveTemplate) {
        return stepBuilderFactory.get("Move Output Step").listener(stepListener).tasklet(outputMoveTemplate).build();
    }

    @Bean
    public Step finalMerge(FinalMergeTemplate finalMergeTemplate) {
        return stepBuilderFactory.get("FinalMerge").listener(stepListener).tasklet(finalMergeTemplate).build();
    }

    @Bean
    public Flow copyToLocalChunk(CopyReader reader, CopyProcessor processor, CopyWriter writer,
                                 CopyFileRetryListener copyFileRetryListener, Step cleanupRemoteBackupStep) {
        return new FlowBuilder<Flow>("copyLocalChunk")
                .start(stepBuilderFactory.get("Copy to Local Step Chunk").listener(stepListener).<Path, Path>chunk(1).faultTolerant()
                        .retryPolicy(new AlwaysRetryPolicy()).retry(Throwable.class).listener(copyFileRetryListener).reader(reader).processor(processor)
                        .writer(writer).build()).next(cleanupRemoteBackupStep).build();
    }

    @Bean
    public Step cleanupRemoteBackupStep(CleanupRemoteBackupTemplate cleanupRemoteBackupTemplate) {
        return stepBuilderFactory.get("Cleanup Backup Step").listener(stepListener).tasklet(cleanupRemoteBackupTemplate).build();
    }

    @Bean
    public Step copyToLocal() {
        return stepBuilderFactory.get("Copy to Local Step").listener(stepListener).tasklet(copyToLocalTemplate).build();
    }

    @Bean
    public Step remoteMove() {
        return stepBuilderFactory.get("Remote Move Step").listener(stepListener).tasklet(remoteMoveTemplate).build();
    }
}
