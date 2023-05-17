package com.security.kerberos.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.kerberos.authentication.KerberosAuthenticationProvider;
import org.springframework.security.kerberos.authentication.KerberosServiceAuthenticationProvider;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosClient;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
import org.springframework.security.kerberos.web.authentication.SpnegoEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.Assert;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${app.service-principal}")
    private String servicePrincipal;

    @Value("${app.keytab-location}")
    private String keytabFilePath;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf()
                .disable()
                .exceptionHandling()
                .accessDeniedHandler(customAccessDeniedHandler())
                .authenticationEntryPoint(spnegoEntryPoint()) // 보호된 리소스 접근 시 동작
                .authenticationEntryPoint(customAuthenticationEntryPoint())
                .and()
                .authorizeRequests().antMatchers("/assets/**").permitAll()
                .and()
                .authorizeRequests().anyRequest().authenticated()
                .and()
                .formLogin().loginPage("/login")
                .failureHandler(customAuthenticationFailureHandler())
                .successHandler(loginSuccessHandler())
                .permitAll()
                .and()
                .authenticationProvider(kerberosAuthenticationProvider()) // 인증 처리 수행, 성공시 Authentication 반환
                .authenticationProvider(kerberosServiceAuthenticationProvider())
                .addFilterBefore(
                        spnegoAuthenticationProcessingFilter(new ProviderManager(kerberosServiceAuthenticationProvider())),
                        BasicAuthenticationFilter.class);
        return http.build();
    }

    @Bean // BadCredentialsException 처리를 위한 handler
    public CustomAuthenticationFailureHandler customAuthenticationFailureHandler() {
        return new CustomAuthenticationFailureHandler();
    }

    @Bean // 로그인 성공 처리 이후 동작을 수행하는 handler
    public LoginSuccessHandler loginSuccessHandler(){
        return new LoginSuccessHandler();
    }

    @Bean // AccessDeniedException 처리를 위한 handler
    public CustomAccessDeniedHandler customAccessDeniedHandler(){
        return new CustomAccessDeniedHandler();
    }

    @Bean // 인증 필요 페이지 접근 시 kerberos ticket 요청을 보내는 entry point
    public SpnegoEntryPoint spnegoEntryPoint() {
        return new SpnegoEntryPoint("/login");
    }

    @Bean // spnego entry point 이후에도 인증 안됐을때 처리
    public CustomAuthenticationEntryPoint customAuthenticationEntryPoint(){
        return new CustomAuthenticationEntryPoint();
    }

    // 사용자에게서 받은 SPNEGO 헤더 처리를 위한 filter
    public SpnegoAuthenticationProcessingFilter spnegoAuthenticationProcessingFilter(
            AuthenticationManager authenticationManager) {
        SpnegoAuthenticationProcessingFilter filter = new SpnegoAuthenticationProcessingFilter();
        filter.setAuthenticationManager(authenticationManager);
        filter.setFailureHandler(customAuthenticationFailureHandler());
        return filter;
    }

    @Bean
    // 사용자의 Kerberos 티켓을 사용하여 사용자를 인증 및 인가
    // 실제 사용자의 인증이 수행되는 부분
    public KerberosAuthenticationProvider kerberosAuthenticationProvider() {
        KerberosAuthenticationProvider provider = new KerberosAuthenticationProvider();
        SunJaasKerberosClient client = new SunJaasKerberosClient();
        client.setDebug(true);
        provider.setKerberosClient(client);
        provider.setUserDetailsService(testUserDetailsService());
        return provider;
    }

    @Bean
    // 실행중인 서비스, 서버가 인증을 처리할 수 있는지 검증
    // 해당 서비스를 spn이 등록된 계정으로 실행한다면 keytab 없이 검증 가능
    // 해당 과정에서 서비스 티켓이 발급되고 사용자 인증 시 서비스 티켓과 함께 인증 요청을 한다.
    public KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider() {
        KerberosServiceAuthenticationProvider provider = new KerberosServiceAuthenticationProvider();
        provider.setTicketValidator(sunJaasKerberosTicketValidator());
        provider.setUserDetailsService(testUserDetailsService());
        return provider;
    }

    @Bean // 사용자로부터 받은 티켓 자체에 대한 유효성 검증
    public SunJaasKerberosTicketValidator sunJaasKerberosTicketValidator() {
        SunJaasKerberosTicketValidator ticketValidator =
                new SunJaasKerberosTicketValidator();
        ticketValidator.setServicePrincipal(servicePrincipal);

        FileSystemResource fs = new FileSystemResource(keytabFilePath);
        log.info("Kerberos KEYTAB file path:" + fs.getFilename() + " principal: " + servicePrincipal + "file exist: " + fs.exists());
        Assert.notNull(fs.exists(), "*.keytab key exist.");
        ticketValidator.setKeyTabLocation(fs);
        ticketValidator.setDebug(true);
        return ticketValidator;
    }

    @Bean
    public TestUserDetailsService testUserDetailsService() {
        return new TestUserDetailsService();
    }

}