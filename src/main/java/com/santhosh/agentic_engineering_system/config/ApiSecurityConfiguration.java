package com.santhosh.agentic_engineering_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ApiSecurityConfiguration {
    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/api/v1/engineering-workflows/*/tasks/*/approval").hasRole("APPROVER")
                        .requestMatchers("/api/v1/engineering-workflows/*/governance/safe-stop").hasRole("OPERATOR")
                        .requestMatchers("/api/v1/engineering-workflows/*/clarification").hasRole("OPERATOR")
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/engineering-workflows").hasRole("OPERATOR")
                        .requestMatchers("/actuator/prometheus").hasAnyRole("OPERATOR", "APPROVER")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults()).build();
    }

    @Bean PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService users(ApiSecurityProperties properties, PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername(properties.operatorUsername())
                        .password(encoder.encode(properties.operatorPassword())).roles("OPERATOR").build(),
                User.withUsername(properties.approverUsername())
                        .password(encoder.encode(properties.approverPassword())).roles("APPROVER").build());
    }
}
