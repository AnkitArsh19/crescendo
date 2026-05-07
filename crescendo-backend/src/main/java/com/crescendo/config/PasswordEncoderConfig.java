package com.crescendo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/// Standalone config for the password encoder bean.
/// Separated from SecurityConfig to avoid circular dependencies:
/// SecurityConfig → OAuth2LoginSuccessHandler → AuthenticationService → BCryptPasswordEncoder.
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
