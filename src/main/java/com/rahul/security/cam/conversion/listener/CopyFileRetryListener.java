package com.rahul.security.cam.conversion.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CopyFileRetryListener extends RetryListenerSupport {
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        log.warn("{}. Retrying copying file {} FailureMessage {}", context.getRetryCount(), context.getAttribute(RetryContext.STATE_KEY),
                throwable.getMessage());
    }

    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        if (context.getRetryCount() > 0 && throwable == null) {
            log.warn("Copy Successful for {} after {} attempt ", context.getAttribute(RetryContext.STATE_KEY), context.getRetryCount());
        }
    }
}
