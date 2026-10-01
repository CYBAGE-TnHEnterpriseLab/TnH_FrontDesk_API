package com.pms.common.config;

import com.pms.common.security.CurrentUserProvider;
import com.pms.common.security.JwtAuthenticationFilter;
import com.pms.common.security.RequestCurrentUserProvider;
import com.pms.common.utils.AccessTokenVerifier;
import com.pms.common.utils.JwtTokenService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(name = "security.jwt.secret")
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityAutoConfiguration {

    @Bean
    public JwtTokenService jwtTokenService(JwtProperties properties) {
        return new JwtTokenService(properties);
    }

    @Bean
    public AccessTokenVerifier accessTokenVerifier(JwtProperties properties) {
        return new AccessTokenVerifier(properties);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(AccessTokenVerifier accessTokenVerifier, JwtProperties properties) {
        return new JwtAuthenticationFilter(accessTokenVerifier, properties);
    }

    @Bean
    public CurrentUserProvider currentUserProvider() {
        return new RequestCurrentUserProvider();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(JwtAuthenticationFilter jwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }
}
