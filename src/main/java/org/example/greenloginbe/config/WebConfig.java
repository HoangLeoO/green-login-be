package org.example.greenloginbe.config;

import org.example.greenloginbe.enums.OrderStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, OrderStatus>() {
            @Override
            public OrderStatus convert(String source) {
                return OrderStatus.fromValue(source);
            }
        });
    }
}
