package de.health.service.cetp.konnektorconfig;

import de.servicehealth.epa4all.config.KonnektorConfig;
import de.servicehealth.epa4all.config.api.ISubscriptionConfig;
import de.servicehealth.epa4all.config.api.UserRuntimeConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Setter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static de.health.service.cetp.SubscriptionManager.FAILED;

@SuppressWarnings("CdiInjectionPointsInspection")
@ApplicationScoped
public class FSConfigService implements KonnektorConfigService {

    private final static Logger log = Logger.getLogger(FSConfigService.class.getName());

    public static final String PROPERTIES_EXT = ".properties";

    @Setter
    @ConfigProperty(name = "ere.per.konnektor.config.folder")
    String configFolder;

    ISubscriptionConfig subscriptionConfig;
    UserRuntimeConfig userRuntimeConfig;

    @Inject
    public FSConfigService(
        ISubscriptionConfig subscriptionConfig,
        UserRuntimeConfig userRuntimeConfig
    ) {
        this.subscriptionConfig = subscriptionConfig;
        this.userRuntimeConfig = userRuntimeConfig;
    }

    @Override
    public Map<String, KonnektorConfig> loadConfigs() {
        List<KonnektorConfig> configs = new ArrayList<>();
        var konnektorConfigFolder = new File(configFolder);
        if (konnektorConfigFolder.exists()) {
            configs = readFromPath(konnektorConfigFolder.getAbsolutePath());
        }
        if (configs.isEmpty()) {
            configs.add(
                new KonnektorConfig(
                    konnektorConfigFolder,
                    subscriptionConfig.getCetpServerDefaultPort(),
                    userRuntimeConfig.getConfigurations(),
                    URI.create(
                        subscriptionConfig.getCardLinkServer()
                            .orElse("wss://cardlink.service-health.de:8444/websocket/80276003650110006580-20230112")
                    )
                )
            );
        }
        return configs.stream().collect(Collectors.toMap(this::getKonnectorKey, config -> config));
    }

    private String getKonnectorKey(KonnektorConfig config) {
        String konnectorHost = config.getHost();
        URI connectorBaseURI = URI.create(userRuntimeConfig.getConnectorBaseURL());
        String host = konnectorHost == null ? connectorBaseURI.getHost() : konnectorHost;
        return String.format("%d_%s", config.getCetpPort(), host);
    }

    public List<KonnektorConfig> readFromPath(String path) {
        File folderFile = new File(path);
        if (folderFile.exists() && folderFile.isDirectory()) {
            File[] files = folderFile.listFiles();
            if (files != null) {
                return Arrays.stream(files)
                    .filter(File::isDirectory)
                    .map(this::fromFolder)
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(KonnektorConfig::getCetpPort))
                    .collect(Collectors.toList());
            }
        }
        return List.of();
    }

    private KonnektorConfig fromFolder(File folder) {
        Optional<File> userPropertiesOpt = Arrays.stream(folder.listFiles())
            .filter(f -> f.getName().endsWith(PROPERTIES_EXT))
            .max(Comparator.comparingLong(File::lastModified));

        Optional<File> subscriptionFileOpt = Arrays.stream(folder.listFiles())
            .filter(f -> !f.getName().startsWith(FAILED) && !f.getName().endsWith(PROPERTIES_EXT))
            .max(Comparator.comparingLong(File::lastModified));

        if (userPropertiesOpt.isPresent()) {
            File actualSubscription = userPropertiesOpt.get();
            if (actualSubscription.exists()) {
                String subscriptionId = subscriptionFileOpt.map(File::getName).orElse(null);
                OffsetDateTime subscriptionTime = subscriptionFileOpt
                    .map(f -> OffsetDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault()))
                    .orElse(OffsetDateTime.now().minusDays(30)); // force subscribe if no subscription is found
                try (var fis = new FileInputStream(actualSubscription)) {
                    Properties properties = new Properties();
                    properties.load(fis);
                    KonnektorConfig konnektorConfig = new KonnektorConfig();
                    konnektorConfig.setCetpPort(Integer.parseInt(folder.getName()));
                    konnektorConfig.setUserConfigurations(new KCUserConfigurations(properties));
                    konnektorConfig.setCardlinkEndpoint(new URI(properties.getProperty("cardlinkServerURL")));
                    konnektorConfig.setSubscriptionId(subscriptionId);
                    konnektorConfig.setSubscriptionTime(subscriptionTime);
                    konnektorConfig.setFolder(folder);
                    return konnektorConfig;
                } catch (URISyntaxException | IOException e) {
                    String msg = String.format(
                        "Could not read konnektor config: folder=%s, subscriptionId=%s", folder.getName(), subscriptionId
                    );
                    log.log(Level.WARNING, msg, e);
                }
            }
        }
        return null;
    }

    @Override
    public void trackSubscriptionFile(KonnektorConfig konnektorConfig, String subscriptionId, String error) {
        try {
            writeFile(konnektorConfig.getFolder().getAbsolutePath() + "/" + subscriptionId, error);
            cleanUpSubscriptionFiles(konnektorConfig, subscriptionId);
            konnektorConfig.setSubscriptionId(subscriptionId);
            konnektorConfig.setSubscriptionTime(OffsetDateTime.now());
        } catch (IOException e) {
            String msg = String.format(
                "Error while recreating subscription properties in folder: %s",
                konnektorConfig.getFolder().getAbsolutePath()
            );
            log.log(Level.SEVERE, msg, e);
        }
    }

    @Override
    public void cleanUpSubscriptionFiles(KonnektorConfig konnektorConfig, String subscriptionId) {
        Arrays.stream(konnektorConfig.getFolder().listFiles())
            .filter(file -> !file.getName().equals(subscriptionId) && !file.getName().endsWith(PROPERTIES_EXT))
            .forEach(file -> {
                boolean deleted = file.delete();
                if (!deleted) {
                    String msg = String.format("Unable to delete previous subscription file: %s", file.getName());
                    log.log(Level.SEVERE, msg);
                    file.renameTo(new File(String.format("%s_DELETING", file.getAbsolutePath())));
                }
            });
    }

    private static void writeFile(String absolutePath, String content) throws IOException {
        try (FileOutputStream os = new FileOutputStream(absolutePath)) {
            if (content != null) {
                os.write(content.getBytes());
            }
            os.flush();
        }
    }
}
