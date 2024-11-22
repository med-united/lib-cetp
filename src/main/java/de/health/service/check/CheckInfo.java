package de.health.service.check;

import java.util.Map;

public record CheckInfo(String name, String status, Map<String, String> data) {
}
