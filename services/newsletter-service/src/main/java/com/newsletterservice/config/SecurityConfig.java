package com.newsletterservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // SecurityConfig (newsletter-service, 8085)
    @Bean
    UserDetailsService users() {
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password("{noop}admin").roles("USER").build()
        );
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/newsletter/subscribe").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/subscriptions").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/newsletter/confirm").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/newsletter/*/content").permitAll() // 임시로 허용
                        .requestMatchers(HttpMethod.POST, "/api/newsletter/delivery/send-now").permitAll() // 발송 허용
                        .requestMatchers(HttpMethod.POST, "/api/newsletter/delivery/schedule").permitAll() // 예약 발송 허용
                        .requestMatchers(HttpMethod.PUT,  "/api/newsletter/delivery/*/cancel").permitAll() // 발송 취소 허용
                        .requestMatchers(HttpMethod.PUT,  "/api/newsletter/delivery/*/retry").permitAll() // 발송 재시도 허용
                        .requestMatchers(HttpMethod.GET,  "/api/newsletter/subscription/*").permitAll() // 구독 조회 허용
                        .requestMatchers(HttpMethod.DELETE, "/api/newsletter/subscription/*").permitAll() // 구독 해지 허용
                        .requestMatchers(HttpMethod.PUT,  "/api/newsletter/subscription/*/status").permitAll() // 구독 상태 변경 허용
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults()); // using Basic for dev
        return http.build();
    }

}

