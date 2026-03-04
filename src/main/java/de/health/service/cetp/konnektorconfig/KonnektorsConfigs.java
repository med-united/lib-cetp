package de.health.service.cetp.konnektorconfig;

import de.health.service.cetp.config.KonnektorConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KonnektorsConfigs {

    private final Map<String, KonnektorConfig> konnektorsConfigs;

    public KonnektorsConfigs(Map<String, KonnektorConfig> konnektorsConfigs) {
        this.konnektorsConfigs = konnektorsConfigs;
    }

    public Set<String> getKonnektors() {
        return Collections.unmodifiableSet(konnektorsConfigs.keySet());
    }
    
    public Collection<KonnektorConfig> getConfigs() {
        return Collections.unmodifiableCollection(konnektorsConfigs.values());
    }

    public Map<String, KonnektorConfig> get() {
        return Collections.unmodifiableMap(konnektorsConfigs);
    }

    // konnektorsConfigs KEY: "konnektorHost<sep>workplaceId"
    public List<KonnektorConfig> filterConfigs(String host, String workplaceId) {
        return konnektorsConfigs.entrySet().stream()
            .filter(entry -> host == null || entry.getKey().startsWith(host))
            .filter(entry -> workplaceId == null || entry.getKey().endsWith(workplaceId))
            .map(Map.Entry::getValue).toList();
    }
}