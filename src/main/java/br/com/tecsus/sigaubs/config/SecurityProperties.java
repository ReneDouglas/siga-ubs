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

    public Session getSession() {
        return session;
    }

    public Password getPassword() {
        return password;
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
        @Min(1)
        private int maximumConcurrentSessions = 3;

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

        public int getMaximumConcurrentSessions() {
            return maximumConcurrentSessions;
        }

        public void setMaximumConcurrentSessions(int maximumConcurrentSessions) {
            this.maximumConcurrentSessions = maximumConcurrentSessions;
        }
    }

    public static class Password {
        public static final int MINIMUM_SUPPORTED_LENGTH = 8;
        public static final int MAXIMUM_SUPPORTED_LENGTH = 64;
        private static final int MINIMUM_BCRYPT_STRENGTH = 4;
        private static final int MAXIMUM_BCRYPT_STRENGTH = 16;
        private static final int DEFAULT_BCRYPT_STRENGTH = 12;

        @Min(MINIMUM_SUPPORTED_LENGTH)
        @Max(MAXIMUM_SUPPORTED_LENGTH)
        private int minimumLength = MINIMUM_SUPPORTED_LENGTH;
        @Min(MINIMUM_SUPPORTED_LENGTH)
        @Max(MAXIMUM_SUPPORTED_LENGTH)
        private int maximumLength = MAXIMUM_SUPPORTED_LENGTH;
        @Min(MINIMUM_BCRYPT_STRENGTH)
        @Max(MAXIMUM_BCRYPT_STRENGTH)
        private int bcryptStrength = DEFAULT_BCRYPT_STRENGTH;

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

}
