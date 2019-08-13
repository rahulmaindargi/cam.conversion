package com.rahul.security.cam.conversion.listener;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Date;

public class AbstractPerformanceListener {
    protected void logPerformance(Date startTime, Date endTime, Logger log, String stepName) {
        if (endTime == null) {
            endTime = new Date();
        }
        long diff = endTime.getTime() - startTime.getTime();
        long diffMilliSeconds = diff % 1000;
        long diffSeconds = diff / 1000 % 60;
        long diffMinutes = diff / (60 * 1000) % 60;
        long diffHours = diff / (60 * 60 * 1000) % 24;
        long diffDays = diff / (24 * 60 * 60 * 1000);
        ArrayList<Object> list = new ArrayList<>();
        list.add(stepName);
        StringBuilder message = new StringBuilder("{} FINISHED! ");
        appendIfNeeded(message, list, diffDays, "days");
        appendIfNeeded(message, list, diffHours, "hours");
        appendIfNeeded(message, list, diffMinutes, "minutes");
        appendIfNeeded(message, list, diffSeconds, "seconds");
        appendIfNeeded(message, list, diffMilliSeconds, "milli seconds");
        log.info(message.toString(), list.toArray());
    }

    private void appendIfNeeded(StringBuilder message, ArrayList<Object> list, long diff, String type) {
        if (diff != 0) {
            message.append(" {} ").append(type);
            list.add(diff);
        }
    }
}
