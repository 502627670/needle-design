package org.needleframe.core;

import org.needleframe.context.AppContextService;
import org.needleframe.core.web.response.formatter.DataFormatter;
import org.needleframe.core.web.response.formatter.DefaultDataFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties
public class WebConfig implements WebMvcConfigurer {
	
	@Autowired
	private MessageSource messageSource;
	
	private WebContextService webContextService = new WebContextService();
	
	@Bean
	@ConditionalOnMissingBean(DataFormatter.class)
	public DataFormatter dataFormatter() {
		return new DefaultDataFormatter(messageSource);
	}
	
	@Bean
	@ConditionalOnMissingBean(AppContextService.class)
	public AppContextService appContextService() {
		return webContextService;
	}
	
	@Bean
    public Jackson2ObjectMapperBuilderCustomizer customJackson() {
        return new Jackson2ObjectMapperBuilderCustomizer() {
            public void customize(Jackson2ObjectMapperBuilder builder) {
            	builder.serializationInclusion(Include.NON_NULL);
                builder.serializationInclusion(Include.NON_EMPTY);
                builder.featuresToDisable(
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        SerializationFeature.FAIL_ON_EMPTY_BEANS,
                        SerializationFeature.FAIL_ON_SELF_REFERENCES,
                        SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS,
                        SerializationFeature.FLUSH_AFTER_WRITE_VALUE,
                        DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
                builder.featuresToEnable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
//                ObjectMapper om = builder.build();
//                om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
//        		om.setSerializationInclusion(Include.NON_NULL);
        		// builder.propertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
            }
        };
    }
	
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
			.allowedOrigins("*")
			.allowCredentials(true)
			.allowedMethods("GET", "POST", "DELETE", "PUT", "PATCH")
			.maxAge(3600);
	}
	
}
