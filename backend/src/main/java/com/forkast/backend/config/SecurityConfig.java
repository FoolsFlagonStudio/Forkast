package com.forkast.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.forkast.backend.auth.JwtAuthenticationFilter;
import com.forkast.backend.user.UserRepository;

import com.forkast.backend.auth.JwtAuthenticationFilter;
import com.forkast.backend.user.UserRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        /** Auth routes anyone can call without an access token. */
        private static final String[] PUBLIC_POST_ROUTES = {
                        "/api/auth/signup",
                        "/api/auth/verify-email",
                        "/api/auth/resend-verification",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password"
        };

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http,
                        SecurityErrorHandler securityErrorHandler,
                        JwtDecoder jwtDecoder,
                        UserRepository userRepository) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .httpBasic(basic -> basic.disable())
                                .formLogin(form -> form.disable())
                                .logout(logout -> logout.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/dietary-labels").permitAll()
                                                .requestMatchers(HttpMethod.POST, PUBLIC_POST_ROUTES).permitAll()
                                                .requestMatchers("/error").permitAll()
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(securityErrorHandler)
                                                .accessDeniedHandler(securityErrorHandler))
                                .addFilterBefore(new JwtAuthenticationFilter(jwtDecoder, userRepository),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }
}