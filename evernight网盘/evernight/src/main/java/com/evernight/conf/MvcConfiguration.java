package com.evernight.conf;


import com.evernight.controller.UserInfoCheckController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * MVC配置类
 */
@Configuration
public class MvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private UserInfoCheckController userInfoCheckController;


    /**
     * 添加参数解析器
     *
     * @param resolvers 参数解析器列表
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userInfoCheckController);
    }

    /**
     * 配置 Spring MVC 使用的 HttpMessageConverter
     * <p>
     * 显式配置一个不带 DefaultTyping 的 ObjectMapper，
     * 避免 Redis 序列化用的 ObjectMapper（含 @class 类型信息）被 Spring MVC 复用，
     * 导致 HTTP 请求体反序列化时要求提供 @class 属性
     *
     * @param converters HttpMessageConverter 列表
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);
        converters.add(0, converter);
    }

    /**
     * 配置跨域请求映射规则
     * <p>
     * 允许所有路径的跨域请求，支持所有来源、指定HTTP方法和所有请求头
     *
     * @param registry CORS注册器对象
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")             // 对所有路径生效
                .allowedOriginPatterns("*")              // 使用allowedOriginPatterns替代allowedOrigins以支持通配符与credentials
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 允许的HTTP方法
                .allowedHeaders("*")                     // 允许的请求头
                .allowCredentials(true);                 // 允许携带认证信息(cookies等)

    }

}
