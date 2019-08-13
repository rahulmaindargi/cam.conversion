package com.rahul.security.cam.conversion.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;

import static java.nio.file.FileVisitResult.CONTINUE;

@Service
@Slf4j
public class FileUtility {
    @Async
    public void deleteIfExists(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).filter(File::exists).forEach(File::delete);
        } else {
            Files.deleteIfExists(path);
        }
    }

    public void copyDirRecursive(Path source, Path destination) throws IOException {
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
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.copy(file, destination.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void filterAndMoveDirectoryContains(Path source, Path destination, CheckedPredicate<Path> predicate) throws IOException {

        Files.newDirectoryStream(source, predicate::test).forEach(file -> {
            try {
                Path destDir = destination.resolve(source.relativize(file));
                if (Files.isDirectory(file)) {
                    if (Files.notExists(destDir)) {
                        Files.createDirectories(destDir);
                    }
                }
                Files.move(file, destDir, StandardCopyOption.REPLACE_EXISTING);
            } catch (DirectoryNotEmptyException dne) {
                log.warn("Directory not empty", dne);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public boolean isPathModifiedBefore(Path path, ZonedDateTime startOfDay, ZoneId zoneId) throws IOException {
        return Files.getLastModifiedTime(path).toInstant().atZone(zoneId).isBefore(startOfDay);
    }
}
