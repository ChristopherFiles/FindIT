package com.findit.campus.config;

import com.findit.campus.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.util.Locale;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/css/**", "/login", "/register", "/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/reports/new", "/laporan/buat").authenticated()
                        .requestMatchers(HttpMethod.GET, "/reports", "/reports/*", "/laporan/*").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/", false)
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/"))
                .sessionManagement(session -> session
                        .maximumSessions(-1)
                        .expiredUrl("/login?expired")
                        .sessionRegistry(sessionRegistry))
                .build();
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return email -> users.findByEmail(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Akun tidak ditemukan."));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
