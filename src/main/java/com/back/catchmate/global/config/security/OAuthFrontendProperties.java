package com.back.catchmate.global.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "oauth.frontend")
public class OAuthFrontendProperties {
    private String successRedirect;
    private String signupRedirect;
}
