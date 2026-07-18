package cn.liyu.hospital.common.security;

import cn.liyu.hospital.common.api.RestfulAccessDeniedHandler;
import cn.liyu.hospital.common.api.RestfulAuthenticationEntryPoint;
import cn.liyu.hospital.config.IgnoreUrlsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置类（现代化：使用 SecurityFilterChain 替代 WebSecurityConfigurerAdapter）
 *
 * @author 医秒通
 */

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired(required = false)
    private IDynamicSecurityService dynamicSecurityService;

    @Autowired
    private IgnoreUrlsConfig ignoreUrlsConfig;

    @Autowired(required = false)
    private DynamicSecurityFilter dynamicSecurityFilter;

    /**
     * 配置 SecurityFilterChain（替代旧版 WebSecurityConfigurerAdapter）
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
                                                    RestfulAccessDeniedHandler restfulAccessDeniedHandler,
                                                    RestfulAuthenticationEntryPoint restAuthenticationEntryPoint,
                                                    JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter) throws Exception {
        // 不需要保护的资源路径允许访问
        httpSecurity.authorizeRequests(registry -> {
            for (String url : ignoreUrlsConfig.getUrls()) {
                // 支持方法级别白名单，格式：GET:/path 或 POST:/path
                // 不带前缀则所有方法放行（兼容旧配置）
                int colonIdx = url.indexOf(':');
                if (colonIdx > 0 && colonIdx < 10) {
                    String method = url.substring(0, colonIdx);
                    String path = url.substring(colonIdx + 1);
                    registry.antMatchers(HttpMethod.resolve(method), path).permitAll();
                } else {
                    registry.antMatchers(url).permitAll();
                }
            }
            // 允许跨域请求的OPTIONS请求
            registry.antMatchers(HttpMethod.OPTIONS).permitAll();
            // 任何请求需要身份认证
            registry.anyRequest().authenticated();
        });

        // 关闭跨站请求防护及不使用session
        httpSecurity.csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        // 自定义权限拒绝处理类
        httpSecurity.exceptionHandling()
                .accessDeniedHandler(restfulAccessDeniedHandler)
                .authenticationEntryPoint(restAuthenticationEntryPoint);

        // 自定义JWT过滤器
        httpSecurity.addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

        // 有动态权限配置时添加动态权限校验过滤器
        if (dynamicSecurityService != null && dynamicSecurityFilter != null) {
            httpSecurity.addFilterBefore(dynamicSecurityFilter, FilterSecurityInterceptor.class);
        }

        return httpSecurity.build();
    }

    /**
     * 暴露 AuthenticationManager（替代旧版 authenticationManagerBean()）
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter() {
        return new JwtAuthenticationTokenFilter();
    }

    @Bean
    public RestfulAccessDeniedHandler restfulAccessDeniedHandler() {
        return new RestfulAccessDeniedHandler();
    }

    @Bean
    public RestfulAuthenticationEntryPoint restAuthenticationEntryPoint() {
        return new RestfulAuthenticationEntryPoint();
    }

    @Bean
    public JwtTokenUtil jwtTokenUtil() {
        return new JwtTokenUtil();
    }

    @ConditionalOnBean(name = "dynamicSecurityService")
    @Bean
    public DynamicAccessDecisionManager dynamicAccessDecisionManager() {
        return new DynamicAccessDecisionManager();
    }

    @ConditionalOnBean(name = "dynamicSecurityService")
    @Bean
    public DynamicSecurityFilter dynamicSecurityFilter() {
        return new DynamicSecurityFilter();
    }

    @Bean
    public DynamicSecurityMetadataSource dynamicSecurityMetadataSource() {
        return new DynamicSecurityMetadataSource();
    }
}
