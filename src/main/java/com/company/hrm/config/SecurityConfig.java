package com.company.hrm.config;

import com.company.hrm.service.HrmUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final HrmUserDetailsService userDetailsService;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .userDetailsService(userDetailsService)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/css/**", "/js/**", "/webjars/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/employees/new", "/employees/*/edit").hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.POST, "/employees/new", "/employees/*/edit", "/employees/*/resign", "/employees/*/contracts")
                        .hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.POST, "/leaves/types/**").hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.POST, "/leaves/*/approve", "/leaves/*/reject")
                        .hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/payroll", "/payroll/*/generate", "/payroll/*/confirm",
                            "/payroll/*/pay", "/payroll/payslips/*").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/payroll/my", "/payroll/payslips/**")
                        .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        .requestMatchers("/payroll", "/payroll/*").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/employees/**").hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers("/contracts/**", "/catalog/**").hasAnyRole("ADMIN", "HR")
                        .requestMatchers("/payroll", "/payroll/**").hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .exceptionHandling(exception -> exception.accessDeniedPage("/error/403"))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}