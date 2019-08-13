package com.rahul.security.cam.conversion.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class Params {
    @Value("#{T(java.nio.file.Paths).get('${basePath:\\\\192.168.0.3\\Public\\xiaomi_camera_videos}')}")
    private Path basePath;
    @Value("${recordingFolderName:7811dc169b8d}")
    private String recordingFolderName;
    @Value("#{ !('${date:}'.equals('')) ? T(java.time.LocalDate).parse('${date:}', T(java.time.format.DateTimeFormatter).ofPattern('MM-dd-yyyy')) " +
            ":T(java.time.LocalDate).now()}")
    private LocalDate asOfDate;

    @Value("#{T(java.nio.file.Paths).get('${ffmpegBin:E:\\ffmpeg\\bin}')}")
    private Path ffmpegBin;

    private ZoneId zoneId = ZoneId.systemDefault();

    public Path getBasePath() {
        return basePath;
    }

    public String getRecordingFolderName() {
        return recordingFolderName;
    }

    public LocalDate getAsOfDate() {
        //return LocalDate.parse("08-11-2019", DateTimeFormatter.ofPattern("MM-dd-yyyy"));
        return asOfDate;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public Path getFfmpegBin() {
        return ffmpegBin;
    }
}
