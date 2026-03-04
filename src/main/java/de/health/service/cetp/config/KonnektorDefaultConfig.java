package de.health.service.cetp.config;

import de.health.service.cetp.konnektorconfig.UserConfigurations;
import de.health.service.config.api.IUserConfigurations;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;

@Getter
@ApplicationScoped
public class KonnektorDefaultConfig {

    private static final Logger log = LoggerFactory.getLogger(KonnektorDefaultConfig.class.getName());

    @ConfigProperty(name = "konnektor.default.url")
    String url;

    @ConfigProperty(name = "konnektor.default.version")
    String version;

    @ConfigProperty(name = "konnektor.default.mandant-id")
    String mandantId;

    @ConfigProperty(name = "konnektor.default.workplace-id")
    String workplaceId;

    @ConfigProperty(name = "konnektor.default.client-system-id")
    String clientSystemId;

    @ConfigProperty(name = "konnektor.default.user-id")
    Optional<String> userId;

    @ConfigProperty(name = "konnektor.default.tvMode")
    String tvMode;

    @ConfigProperty(name = "konnektor.default.auth")
    Optional<KonnektorAuth> auth;

    @ConfigProperty(name = "konnektor.default.basic.auth.username")
    Optional<String> basicAuthUsername;

    @ConfigProperty(name = "konnektor.default.basic.auth.password")
    Optional<String> basicAuthPassword;

    @ConfigProperty(name = "konnektor.default.cert.auth.store.file")
    Optional<String> certAuthStoreFile;

    @ConfigProperty(name = "konnektor.default.cert.auth.store.file.password")
    Optional<String> certAuthStoreFilePassword;

    private String clientCertificate;

    public IUserConfigurations toUserConfigurations(String iccsn) {
        UserConfigurations userConfigurations = new UserConfigurations();
        userConfigurations.setAuth(auth.orElse(KonnektorAuth.CERTIFICATE).name());
        userConfigurations.setBasicAuthUsername(basicAuthUsername.orElse(null));
        userConfigurations.setBasicAuthPassword(basicAuthPassword.orElse(null));
        userConfigurations.setClientCertificate(resolveClientCertificate());
        userConfigurations.setClientCertificatePassword(getCertAuthStoreFilePassword().orElse(null));
        userConfigurations.setConnectorBaseURL(url);
        userConfigurations.setClientSystemId(clientSystemId);
        userConfigurations.setMandantId(mandantId);
        userConfigurations.setWorkplaceId(workplaceId);
        userConfigurations.setUserId(userId.orElse(null));
        userConfigurations.setVersion(version);
        userConfigurations.setTvMode(tvMode);
        userConfigurations.setIccsn(iccsn);

        return userConfigurations;
    }

    private String resolveClientCertificate() {
        if (clientCertificate == null) {
            Optional<String> certAuthStoreFileOpt = getCertAuthStoreFile();
            if (certAuthStoreFileOpt.isPresent()) {
                String certFilePath = certAuthStoreFileOpt.get();
                byte[] certBytes = new byte[0];
                try {
                    certBytes = Files.readAllBytes(Paths.get(certFilePath));
                } catch (IOException e) {
                    log.error("Unable to read certificate from " + certFilePath, e);
                }
                String prefix = "data:application/x-pkcs12;base64,";
                clientCertificate = prefix + Base64.getEncoder().encodeToString(certBytes).replace("\n", "");
            }
        }
        return clientCertificate;
    }
}