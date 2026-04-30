package com.rpa.manage.config;

import com.rpa.manage.security.JwtAuthenticationFilter;
import com.rpa.manage.security.RestAccessDeniedHandler;
import com.rpa.manage.security.RestAuthenticationEntryPoint;
import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 安全配置类。
 *
 * <p>这里集中声明整个项目的安全规则，例如：
 * - 哪些接口不需要登录；
 * - 是否启用 Session；
 * - JWT 过滤器插到哪里；
 * - 未登录和无权限时返回什么风格的响应；
 * - 前后端联调时 CORS 怎么放开。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 白名单接口。
     *
     * <p>这些地址不需要携带 token 就能访问，通常包括登录接口、Swagger 文档、
     * 上传文件的静态访问路径等。
     */
    private static final String[] WHITE_LIST = {
            "/api/v1/auth/login",
            "/test-ui.html",
            "/uploads/**",
            "/error",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    /**
     * 配置 Spring Security 过滤器链。
     *
     * <p>当前项目采用的是“无状态 JWT”方案，所以这里会关闭表单登录、HTTP Basic 和 Session，
     * 让每次请求都依赖请求头里的 token 完成认证。
     *
     * @param http Spring Security 的 HTTP 配置入口
     * @param jwtAuthenticationFilter JWT 过滤器
     * @param restAuthenticationEntryPoint 未登录处理器
     * @param restAccessDeniedHandler 无权限处理器
     * @return 安全过滤器链
     * @throws Exception 配置过程中可能抛出的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler
    ) throws Exception {
        http
                // 前后端分离项目常见配置：统一放开跨域，再交给具体接口去做鉴权。
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                // 使用 JWT 时，服务端不保存传统 Session。
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(WHITE_LIST).permitAll()
                        .anyRequest().authenticated()
                )
                // 让 JWT 过滤器先于用户名密码过滤器执行，尽早把登录用户放进上下文。
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 声明密码加密器。
     *
     * <p>BCrypt 是 Spring Security 里很常见的密码散列算法，
     * 适合保存用户密码，不应使用明文或可逆加密。
     *
     * @return BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置跨域规则。
     *
     * <p>当前配置比较宽松，便于前后端联调：
     * 允许任意来源、常见 HTTP 方法、任意请求头。
     * 如果将来上线到生产环境，通常需要再收紧来源范围。
     *
     * @return CORS 配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
