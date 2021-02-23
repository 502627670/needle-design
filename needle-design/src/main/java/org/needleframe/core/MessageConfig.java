//package org.needleframe.core;
//
//import java.time.Duration;
//import java.util.Locale;
//
//import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
//import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.MessageSource;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.support.ResourceBundleMessageSource;
//import org.springframework.util.StringUtils;
//import org.springframework.web.servlet.LocaleResolver;
//import org.springframework.web.servlet.i18n.CookieLocaleResolver;
//
//@Configuration
//@EnableConfigurationProperties
//public class MessageConfig extends MessageSourceAutoConfiguration {
//		
//	public final static String MESSAGE_SOURCE = "i18n/messages";
//	
//	public final static String LANGUAGE = "language";
//	
//	@Override
//	public MessageSourceProperties messageSourceProperties() {
//		MessageSourceProperties messageSourceProperties = super.messageSourceProperties();
//		String basename = messageSourceProperties.getBasename();
//		basename = String.join(",", basename, MESSAGE_SOURCE);
//		messageSourceProperties.setBasename(basename);
//		return messageSourceProperties;
//	}
//	
//	@Bean
//	public MessageSource messageSource() {
//		MessageSourceProperties properties = messageSourceProperties();
//		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
//		if (StringUtils.hasText(properties.getBasename())) {
//			messageSource.setBasenames(StringUtils.commaDelimitedListToStringArray(
//					StringUtils.trimAllWhitespace(properties.getBasename())));
//		}
//		if (properties.getEncoding() != null) {
//			messageSource.setDefaultEncoding(properties.getEncoding().name());
//		}
//		messageSource.setFallbackToSystemLocale(properties.isFallbackToSystemLocale());
//		Duration cacheDuration = properties.getCacheDuration();
//		if (cacheDuration != null) {
//			messageSource.setCacheMillis(cacheDuration.toMillis());
//		}
//		messageSource.setAlwaysUseMessageFormat(properties.isAlwaysUseMessageFormat());
//		messageSource.setUseCodeAsDefaultMessage(properties.isUseCodeAsDefaultMessage());
//		return messageSource;
//	}
//	
//	@Bean
//	public LocaleResolver localeResolver() {
//		CookieLocaleResolver localeResolver = new CookieLocaleResolver();
//		localeResolver.setCookieName(LANGUAGE);
//		localeResolver.setCookieMaxAge(365 * 1 * 24 * 60 * 60);
//		localeResolver.setDefaultLocale(Locale.CHINA);
//	    return localeResolver;
//	}
//}
