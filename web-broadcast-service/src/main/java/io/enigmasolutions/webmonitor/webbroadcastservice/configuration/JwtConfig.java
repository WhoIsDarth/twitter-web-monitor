package io.enigmasolutions.webmonitor.webbroadcastservice.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "security.jwt")
public class JwtConfig {

    private String Uri;
    private String header;
    private String prefix;
    private int expiration;
    private String secret;
}
