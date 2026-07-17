package com.crescendo.security.alerts;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class GeoIpService {
    private static final Logger log = LoggerFactory.getLogger(GeoIpService.class);

    private final String dbPath;
    private final String licenseKey;
    private final boolean autoUpdateEnabled;
    private final AtomicReference<DatabaseReader> reader = new AtomicReference<>();

    public GeoIpService(
            @Value("${app.geoip.db-path:}") String dbPath,
            @Value("${app.geoip.license-key:}") String licenseKey,
            @Value("${app.geoip.auto-update.enabled:false}") boolean autoUpdateEnabled) {
        this.dbPath = dbPath;
        this.licenseKey = licenseKey;
        this.autoUpdateEnabled = autoUpdateEnabled;
    }

    @PostConstruct
    public void init() {
        if (dbPath == null || dbPath.isBlank()) {
            log.info("GeoIP database path not configured. Country-level login alerts will be disabled.");
            return;
        }

        File dbFile = new File(dbPath);

        // Auto-download on startup if missing and key is provided
        if (!dbFile.exists() && autoUpdateEnabled && licenseKey != null && !licenseKey.isBlank() && !licenseKey.equals("REPLACE_ME")) {
            log.info("GeoIP database missing on startup. Attempting initial download...");
            downloadAndExtractDatabase();
        }

        loadDatabase();
    }

    private void loadDatabase() {
        File dbFile = new File(dbPath);
        if (!dbFile.exists() || !dbFile.canRead()) {
            log.warn("GeoIP database file not found or unreadable at '{}'. Country-level alerts disabled.", dbPath);
            return;
        }

        try {
            DatabaseReader newReader = new DatabaseReader.Builder(dbFile).build();
            DatabaseReader oldReader = reader.getAndSet(newReader);
            log.info("GeoIP Database loaded successfully from '{}'", dbPath);

            if (oldReader != null) {
                oldReader.close();
            }
        } catch (IOException e) {
            log.error("Failed to initialize GeoIP DatabaseReader", e);
        }
    }

    /**
     * Run every Wednesday at 2 AM (MaxMind updates are typically released on Tuesdays).
     */
    @Scheduled(cron = "0 0 2 * * WED")
    public void scheduledUpdate() {
        if (autoUpdateEnabled && licenseKey != null && !licenseKey.isBlank() && !licenseKey.equals("REPLACE_ME")) {
            log.info("Starting scheduled GeoIP database update...");
            if (downloadAndExtractDatabase()) {
                loadDatabase(); // Hot reload the new database file
            }
        }
    }

    private boolean downloadAndExtractDatabase() {
        String downloadUrl = "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-Country&license_key=" + licenseKey + "&suffix=tar.gz";
        File targetFile = new File(dbPath);
        File parentDir = targetFile.getParentFile();

        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try {
            URL url = new URL(downloadUrl);
            try (InputStream in = new BufferedInputStream(url.openStream());
                 GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
                 TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn)) {

                TarArchiveEntry entry;
                while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
                    if (entry.getName().endsWith(".mmdb")) {
                        // Download to a temporary file first
                        Path tempFile = Files.createTempFile("geoip", ".mmdb");
                        Files.copy(tarIn, tempFile, StandardCopyOption.REPLACE_EXISTING);

                        // Atomically move it into place
                        Files.move(tempFile, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        log.info("Successfully downloaded and extracted new GeoIP database to {}", dbPath);
                        return true;
                    }
                }
            }
            log.error("Downloaded archive did not contain a .mmdb file.");
            return false;
        } catch (Exception e) {
            log.error("Failed to download or extract GeoIP database", e);
            return false;
        }
    }

    @PreDestroy
    public void cleanup() {
        DatabaseReader currentReader = reader.get();
        if (currentReader != null) {
            try {
                currentReader.close();
            } catch (IOException e) {
                log.error("Error closing GeoIP DatabaseReader", e);
            }
        }
    }

    /**
     * Looks up the country ISO code for an IP address.
     * Returns Optional.empty() if the IP is internal, unresolvable, or if the database is disabled.
     */
    @SuppressWarnings({"deprecation", "removal"})
    public Optional<String> lookupCountry(String ipAddress) {
        DatabaseReader currentReader = reader.get();
        if (currentReader == null || ipAddress == null || ipAddress.isBlank()) {
            return Optional.empty();
        }

        try {
            InetAddress ip = InetAddress.getByName(ipAddress);
            CountryResponse response = currentReader.country(ip);
            if (response != null && response.getCountry() != null) {
                return Optional.ofNullable(response.getCountry().getIsoCode());
            }
        } catch (IOException | GeoIp2Exception e) {
            // Address not found in database or invalid format. Normal for private IPs.
            log.debug("GeoIP lookup failed for IP: {}", ipAddress, e);
        }
        return Optional.empty();
    }
}
