package com.rahul.security.cam.conversion.filecopy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.FileVisitResult.CONTINUE;

@Component
@StepScope
@Slf4j
public class CopyReader implements ItemReader<Path> {
    private List<Path> fileList;

    @Value("#{stepExecution}")
    private StepExecution stepExecution;
    /*@Value("#{jobExecutionContext}")
    private ExecutionContext executionContext;*/
    @Value("#{jobExecutionContext['magicName']}")
    private String magicName;
    @Value("#{jobExecutionContext['naasBackup']}")
    private Path naasBackup;

    @Override
    public Path read() {
        if (!fileList.isEmpty()) {
            return fileList.remove(0);
        }
        return null;
    }

    @PostConstruct
    public void init() throws Exception {
        ExecutionContext executionContext = stepExecution.getJobExecution().getExecutionContext();
        //String magicName = (String) executionContext.get("magicName");
        //  Path naasBackup = requireNonNull((Path) executionContext.get("naasBackup"));
        Path localSource = Paths.get("e:", magicName + "_Source");
        fileList = getFileList(naasBackup, localSource);
        executionContext.put("localSource", localSource);
        executionContext.put("localDest", Paths.get("e:", magicName));
        executionContext.put("localDestFile", Paths.get("e:", magicName, magicName + ".mp4"));
    }

    private List<Path> getFileList(Path source, Path destination) throws IOException {
        ArrayList<Path> sourceFiles = new ArrayList<>();
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = destination.resolve(source.relativize(dir));
                try {
                    Files.copy(dir, targetDir);
                } catch (FileAlreadyExistsException faee) {
                    if (!Files.isDirectory(targetDir)) {
                        throw faee;
                    }
                }
                return CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                sourceFiles.add(file);
                //Files.copy(file, destination.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
        return sourceFiles;
    }
}
