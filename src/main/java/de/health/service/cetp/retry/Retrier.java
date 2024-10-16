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

    public static <T> T callAndRetry(
        List<Integer> retryMillis,
        int retryPeriodMs,
        RetryAction<T> action,
        Predicate<T> predicate
    ) {
        T result = safeExecute(action);
        if (result != null && predicate.test(result)) {
            return result;
        }
        List<Integer> retries = retryMillis.stream().filter(Objects::nonNull).sorted().toList();
        if (!retries.isEmpty()) {
            int k = 0;
            long start = System.currentTimeMillis();
            while (result == null || !predicate.test(result)) {
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
                result = safeExecute(action);
            }
        }
        return result;
    }

    private static <T> T safeExecute(RetryAction<T> action) {
        try {
            return action.execute();
        } catch (Exception t) {
            log.log(Level.SEVERE, "Error while executing retryable action", t);
        } catch (Throwable t) {
            // Log in case that happens in an unchecked executor task
            // to not miss something like an OutOfMemoryError
            log.log(Level.SEVERE, "Caught throwable!!! Panic!", t);
            throw t;
        }
        return null;
    }
}
