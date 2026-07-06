package com.mmetzner.vmh.auth.infrastructure.security;

import java.io.IOException;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmetzner.vmh.shared.common.ApiMessages;
import com.mmetzner.vmh.shared.exception.ApiErrorCode;
import com.mmetzner.vmh.shared.presentation.dto.error.ApiErrorResponse;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final CorsProperties corsProperties;

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(
                        corsConfigurationSource()
                ))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        ))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v1/auth/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                (request, response, exception) ->
                                        writeErrorResponse(
                                                response,
                                                HttpStatus.UNAUTHORIZED,
                                                ApiErrorCode.UNAUTHENTICATED,
                                                ApiMessages.Auth.UNAUTHENTICATED,
                                                request.getRequestURI()
                                        )
                        )
                        .accessDeniedHandler(
                                (request, response, exception) ->
                                        writeErrorResponse(
                                                response,
                                                HttpStatus.FORBIDDEN,
                                                ApiErrorCode.ACCESS_DENIED,
                                                ApiMessages.Auth.ACCESS_DENIED,
                                                request.getRequestURI()
                                        )
                        ))
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                corsProperties.allowedOrigins()
        );

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type"
        ));

        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/v1/**",
                configuration
        );

        return source;
    }

    private void writeErrorResponse(
            HttpServletResponse response,
            HttpStatus status,
            ApiErrorCode code,
            String message,
            String path
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(
                response.getWriter(),
                ApiErrorResponse.withoutFieldErrors(
                        status.value(),
                        status.getReasonPhrase(),
                        code.name(),
                        message,
                        path
                )
        );
    }
}