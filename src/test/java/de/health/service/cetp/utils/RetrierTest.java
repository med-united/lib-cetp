package de.health.service.cetp.utils;

import de.health.service.cetp.retry.Retrier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RetrierTest {

    private static class Tester {
        boolean testFunc() {
            return false;
        }
    }

    @Test
    public void testFuncRetriesCorrectlyWithinRetryPeriod() {
        Tester t = mock(Tester.class);
        when(t.testFunc()).thenThrow(new IllegalStateException("x-insurantid is not found"));

        int repeat = 3;
        Boolean result = null;
        Exception ex = null;
        try {
            result = Retrier.callAndRetryEx(
                List.of(1000),
                repeat * 1000 + 500,
                true,
                Set.of(),
                t::testFunc,
                () -> false,
                bool -> bool != null && bool
            );
        } catch (Exception e) {
            ex = e;
        }
        assertNotNull(ex);
        assertNull(result);
        verify(t, times(repeat + 1)).testFunc();
    }

    @Test
    public void testFuncDidntRetryDueToImmediateCondition() {
        Tester t = mock(Tester.class);
        when(t.testFunc()).thenThrow(new IllegalStateException("x-insurantid is not found"));

        int repeat = 30;
        Boolean result = null;
        Exception ex = null;
        try {
            result = Retrier.callAndRetryEx(
                List.of(1000),
                repeat * 1000 + 500,
                true,
                Set.of("not found"),
                t::testFunc,
                () -> false,
                bool -> bool != null && bool
            );
        } catch (Exception e) {
            ex = e;
        }
        assertNotNull(ex);
        assertNull(result);
        verify(t, times(1)).testFunc();
    }
}
