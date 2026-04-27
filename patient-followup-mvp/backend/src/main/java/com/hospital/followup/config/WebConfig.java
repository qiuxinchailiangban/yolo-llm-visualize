package com.hospital.followup.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties({
    CorsProperties.class,
    AdminAuthProperties.class,
    WechatMiniappProperties.class,
    RpaIntegrationProperties.class,
    AutomationWorkerProperties.class
})
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;
    private final AdminAuthInterceptor adminAuthInterceptor;
    private final WorkerAuthInterceptor workerAuthInterceptor;

    public WebConfig(
        CorsProperties corsProperties,
        AdminAuthInterceptor adminAuthInterceptor,
        WorkerAuthInterceptor workerAuthInterceptor
    ) {
        this.corsProperties = corsProperties;
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.workerAuthInterceptor = workerAuthInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(corsProperties.getAllowedOrigins().toArray(String[]::new))
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
            .addPathPatterns("/api/admin/**");
        registry.addInterceptor(workerAuthInterceptor)
            .addPathPatterns("/api/worker/**");
    }
}
