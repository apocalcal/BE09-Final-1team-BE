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
                        .requestMatchers(HttpMethod.GET,  "/api/newsletter/{newsletterId}/content").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/newsletter/{newsletterId}/html").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/newsletter/{newsletterId}/preview").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/newsletter/delivery/send-now").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/newsletter/delivery/schedule").permitAll()
                        .requestMatchers(HttpMethod.PUT,  "/api/newsletter/delivery/{deliveryId}/cancel").permitAll()
                        .requestMatchers(HttpMethod.PUT,  "/api/newsletter/delivery/{deliveryId}/retry").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/newsletter/subscription/{id}").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/newsletter/subscription/{id}").permitAll()
                        .requestMatchers(HttpMethod.PUT,  "/api/newsletter/subscription/{subscriptionId}/status").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/trending-keywords").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/category/{category}/trending-keywords").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/newsletter/category/{category}/articles").permitAll()
                        .requestMatchers(HttpMethod.GET, "/newsletter/trending-keywords").permitAll()
                        .requestMatchers(HttpMethod.GET, "/newsletter/category/{category}/trending-keywords").permitAll()

                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults()); // using Basic for dev
        return http.build();
    }

}

