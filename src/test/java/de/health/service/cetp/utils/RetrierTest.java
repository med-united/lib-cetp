package de.health.service.cetp.utils;

import de.health.service.cetp.retry.Retrier;
import lombok.ToString;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RetrierTest {

    @ToString(of = {"value"})
    private static class Result {
        String value;

        public Result(String value) {
            this.value = value;
        }
    }

    private static class Tester {
        private final Result result;

        public Tester(Result result) {
            this.result = result;
        }

        Result testFunc() {
            if (result.value.contains("insurantid")) {
                throw new IllegalStateException(result.value);
            }
            return result;
        }
    }

    @Test
    public void testFuncRetriesCorrectlyWithinRetryPeriod() {
        String badValue = "badValue";
        Tester t = spy(new Tester(new Result(badValue)));

        int repeat = 3;
        Result result = null;
        Exception ex = null;
        try {
            result = Retrier.callAndRetryEx(
                List.of(1000),
                repeat * 1000 + 500,
                true,
                Set.of(),
                t::testFunc,
                () -> false,
                res -> !res.value.equals(badValue)
            );
        } catch (Exception e) {
            ex = e;
        }
        assertNull(ex);
        assertNotNull(result);
        verify(t, times(repeat + 1)).testFunc();
    }

    @Test
    public void testFuncDidntRetryDueToImmediateCondition() {
        String norFound = "x-insurantid is not found";
        Tester t = spy(new Tester(new Result(norFound)));

        int repeat = 30;
        Result result = null;
        Exception ex = null;
        try {
            result = Retrier.callAndRetryEx(
                List.of(1000),
                repeat * 1000 + 500,
                true,
                Set.of("not found"),
                t::testFunc,
                () -> false,
                Objects::nonNull
            );
        } catch (Exception e) {
            ex = e;
        }
        assertNotNull(ex);
        assertNull(result);
        verify(t, times(1)).testFunc();
    }
}
