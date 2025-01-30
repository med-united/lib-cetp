package de.health.service.cetp.retry;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
            return callAndRetryEx(retryMillis, retryPeriodMs, false, Set.of(), action, () -> false, predicate);
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> T callAndRetryEx(
        List<Integer> retryMillis,
        int retryPeriodMs,
        boolean keepException,
        Set<String> immediateSet,
        RetryAction<T> action,
        Supplier<Boolean> blocker,
        Predicate<T> stopCondition
    ) throws Exception {
        SafeInfo<T> safeInfo = safeExecute(action);
        if (stopByResultCondition(safeInfo, stopCondition)) {
            return safeInfo.result;
        }
        List<Integer> retries = retryMillis.stream().filter(Objects::nonNull).sorted().toList();
        if (!retries.isEmpty()) {
            int k = 0;
            long start = System.currentTimeMillis();
            while (!immediateReturn(immediateSet, safeInfo) && !stopByResultCondition(safeInfo, stopCondition)) {
                Integer timeoutMs = retries.get(k++);
                if (k >= retries.size()) {
                    k = retries.size() - 1;
                }
                long delta = System.currentTimeMillis() - start;
                if (delta + timeoutMs > retryPeriodMs) {
                    break;
                }
                sleepMs(timeoutMs);
                if (blocker.get() == null || !blocker.get()) {
                    safeInfo = safeExecute(action);
                }
            }
        }
        if (keepException && safeInfo.result == null && safeInfo.exception != null) {
            throw safeInfo.exception;
        } else {
            return safeInfo.result;
        }
    }

    private static <T> boolean stopByResultCondition(SafeInfo<T> safeInfo, Predicate<T> stopCondition) {
        return safeInfo.result != null && stopCondition.test(safeInfo.result);
    }

    private static <T> boolean immediateReturn(Set<String> immediate, SafeInfo<T> safeInfo) {
        if (safeInfo.exception == null || safeInfo.exception.getMessage() == null) {
            return false;
        }
        // todo find better solution, issue is lib-cetp can't import project modules so unified approach is needed
        return immediate.stream().anyMatch(safeInfo.exception.getMessage()::contains);
    }

    private static void sleepMs(int value) {
        try {
            TimeUnit.MILLISECONDS.sleep(value);
        } catch (InterruptedException ignored) {
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
