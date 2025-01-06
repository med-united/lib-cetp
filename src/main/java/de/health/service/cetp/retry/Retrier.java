package de.health.service.cetp.retry;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Retrier {

    private static final Logger log = Logger.getLogger(Retrier.class.getName());

    private Retrier() {
    }

    private static class SafeInfo<T> {
        T result;
        Exception exception;

        public SafeInfo(T result, Exception exception) {
            this.result = result;
            this.exception = exception;
        }
    }

    public static <T> T callAndRetry(
        List<Integer> retryMillis,
        int retryPeriodMs,
        RetryAction<T> action,
        Predicate<T> predicate
    ) {
        try {
            return callAndRetryEx(retryMillis, retryPeriodMs, false, action, predicate);
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> T callAndRetryEx(
        List<Integer> retryMillis,
        int retryPeriodMs,
        boolean keepException,
        RetryAction<T> action,
        Predicate<T> predicate
    ) throws Exception {
        SafeInfo<T> safeInfo = safeExecute(action);
        if (safeInfo.result != null && predicate.test(safeInfo.result)) {
            return safeInfo.result;
        }
        List<Integer> retries = retryMillis.stream().filter(Objects::nonNull).sorted().toList();
        if (!retries.isEmpty()) {
            int k = 0;
            long start = System.currentTimeMillis();
            while (safeInfo.result == null || !predicate.test(safeInfo.result)) {
                Integer timeoutMs = retries.get(k++);
                if (k >= retries.size()) {
                    k = retries.size() - 1;
                }
                long delta = System.currentTimeMillis() - start;
                if (delta + timeoutMs > retryPeriodMs) {
                    break;
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(timeoutMs);
                } catch (InterruptedException ignored) {
                }
                safeInfo = safeExecute(action);
            }
        }
        if (keepException && safeInfo.result == null && safeInfo.exception != null) {
            throw safeInfo.exception;
        } else {
            return safeInfo.result;
        }
    }

    private static <T> SafeInfo<T> safeExecute(RetryAction<T> action) {
        try {
            return new SafeInfo<>(action.execute(), null);
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error while executing retryable action", e);
            return new SafeInfo<>(null, e);
        } catch (Throwable t) {
            // Log in case that happens in an unchecked executor task
            // to not miss something like an OutOfMemoryError
            log.log(Level.SEVERE, "Caught throwable!!! Panic!", t);
            throw t;
        }
    }
}
