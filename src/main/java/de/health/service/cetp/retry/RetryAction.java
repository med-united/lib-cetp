package de.health.service.cetp.retry;

public interface RetryAction<T> {

    T execute() throws Exception;
}
