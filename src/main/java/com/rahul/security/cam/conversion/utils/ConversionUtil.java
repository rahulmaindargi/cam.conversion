package com.rahul.security.cam.conversion.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
@Slf4j
public class ConversionUtil {

    public void onOrderedStream(DirectoryStream<Path> directoryStream, Consumer<Path> consumer) {
        List<Path> paths = new ArrayList<>();
        directoryStream.forEach(paths::add);
        paths.stream().sorted().forEachOrdered(consumer);
    }

    public DirectoryStream<Path> childrenDirs(Path parent) throws IOException {
        return Files.newDirectoryStream(parent, path -> path.toFile().isDirectory());
    }

    public DirectoryStream<Path> childrenFiles(Path parent) throws IOException {
        return Files.newDirectoryStream(parent, path -> path.toFile().isFile());
    }

}
