
package com.ambulance.dispatch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    public WebConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(loginInterceptor)
                .addPathPatterns(
                        "/ambulance",
                        "/ambulance/**",
                        "/emergency",
                        "/emergency/**",
                        "/zone",
                        "/zone/**",
                        "/distance",
                        "/distance/**",
                        "/report",
                        "/report/**"
                );
    }
}