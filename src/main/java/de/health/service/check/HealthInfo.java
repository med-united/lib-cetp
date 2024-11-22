package de.health.service.check;

import java.util.List;

public record HealthInfo(String status, List<CheckInfo> checks) {
}
