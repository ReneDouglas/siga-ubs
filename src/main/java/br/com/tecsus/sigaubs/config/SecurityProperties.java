package br.com.tecsus.sigaubs.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "sigaubs.security")
public class SecurityProperties {

    @Valid
    private final Session session = new Session();

    @Valid
    private final Password password = new Password();

    @Valid
    private final Input input = new Input();

    public Session getSession() {
        return session;
    }

    public Password getPassword() {
        return password;
    }

    public Input getInput() {
        return input;
    }

    public enum SessionLocationSource {
        DISABLED,
        LOCAL,
        TRUSTED_PROXY
    }

    public static class Session {
        @NotNull
        private Duration tenantIdle = Duration.ofHours(12);
        @NotNull
        private Duration adminIdle = Duration.ofHours(2);
        @NotNull
        private Duration tenantAbsolute = Duration.ofHours(72);
        @NotNull
        private Duration adminAbsolute = Duration.ofHours(12);
        @NotNull
        private Duration reauthentication = Duration.ofMinutes(5);
        @NotNull
        private SessionLocationSource locationSource = SessionLocationSource.DISABLED;

        public Duration getTenantIdle() {
            return tenantIdle;
        }

        public void setTenantIdle(Duration tenantIdle) {
            this.tenantIdle = tenantIdle;
        }

        public Duration getAdminIdle() {
            return adminIdle;
        }

        public void setAdminIdle(Duration adminIdle) {
            this.adminIdle = adminIdle;
        }

        public Duration getTenantAbsolute() {
            return tenantAbsolute;
        }

        public void setTenantAbsolute(Duration tenantAbsolute) {
            this.tenantAbsolute = tenantAbsolute;
        }

        public Duration getAdminAbsolute() {
            return adminAbsolute;
        }

        public void setAdminAbsolute(Duration adminAbsolute) {
            this.adminAbsolute = adminAbsolute;
        }

        public Duration getReauthentication() {
            return reauthentication;
        }

        public void setReauthentication(Duration reauthentication) {
            this.reauthentication = reauthentication;
        }

        public SessionLocationSource getLocationSource() {
            return locationSource;
        }

        public void setLocationSource(SessionLocationSource locationSource) {
            this.locationSource = locationSource;
        }
    }

    public static class Password {
        @Min(8)
        private int minimumLength = 8;
        @Max(64)
        private int maximumLength = 64;
        @Min(4)
        @Max(16)
        private int bcryptStrength = 12;

        public int getMinimumLength() {
            return minimumLength;
        }

        public void setMinimumLength(int minimumLength) {
            this.minimumLength = minimumLength;
        }

        public int getMaximumLength() {
            return maximumLength;
        }

        public void setMaximumLength(int maximumLength) {
            this.maximumLength = maximumLength;
        }

        public int getBcryptStrength() {
            return bcryptStrength;
        }

        public void setBcryptStrength(int bcryptStrength) {
            this.bcryptStrength = bcryptStrength;
        }
    }

    public static class Input {
        @Min(1)
        @Max(100)
        private int maximumPageSize = 100;
        @Min(1)
        @Max(100)
        private int maximumSlotBatchSize = 100;
        @Min(100)
        @Max(10_000)
        private int maximumObservationLength = 2_000;

        public int getMaximumPageSize() {
            return maximumPageSize;
        }

        public void setMaximumPageSize(int maximumPageSize) {
            this.maximumPageSize = maximumPageSize;
        }

        public int getMaximumSlotBatchSize() {
            return maximumSlotBatchSize;
        }

        public void setMaximumSlotBatchSize(int maximumSlotBatchSize) {
            this.maximumSlotBatchSize = maximumSlotBatchSize;
        }

        public int getMaximumObservationLength() {
            return maximumObservationLength;
        }

        public void setMaximumObservationLength(int maximumObservationLength) {
            this.maximumObservationLength = maximumObservationLength;
        }
    }
}
