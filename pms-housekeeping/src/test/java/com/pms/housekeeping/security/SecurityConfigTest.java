package com.pms.housekeeping.security;

import com.pms.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    @Test
    void securityFilterChain_shouldBuildUsingConfiguredHttpSecurity() throws Exception {
        SecurityConfig config = new SecurityConfig();
        JwtAuthenticationFilter jwtAuthenticationFilter = mock(JwtAuthenticationFilter.class);
        HttpSecurity http = mock(HttpSecurity.class);
        DefaultSecurityFilterChain expectedChain = mock(DefaultSecurityFilterChain.class);

        when(http.csrf(any())).thenReturn(http);
        when(http.cors(any())).thenReturn(http);
        when(http.sessionManagement(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)).thenReturn(http);
        when(http.build()).thenReturn(expectedChain);

        SecurityFilterChain actual = config.securityFilterChain(http, jwtAuthenticationFilter);

        assertThat(actual).isSameAs(expectedChain);
        verify(http).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
