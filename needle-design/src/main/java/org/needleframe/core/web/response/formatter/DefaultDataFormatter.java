package org.needleframe.core.web.response.formatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.needleframe.core.model.ModuleProp.Feature;
import org.needleframe.core.model.ModuleProp.Select;
import org.needleframe.core.web.response.DataModule;
import org.needleframe.core.web.response.DataModuleBuilder.DataViewProp;
import org.needleframe.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.util.StringUtils;

public class DefaultDataFormatter implements DataFormatter {
	
	Logger logger = LoggerFactory.getLogger(DefaultDataFormatter.class);
	
	public final static String CHINESE = "[\u4e00-\u9fa5]";
	
	private MessageSource messageSource;
	
	public DefaultDataFormatter(MessageSource messageSource) {
		this.messageSource = messageSource;
	}
	
	public Locale getLocale() {
		return Locale.getDefault();
	}
	
	public void format(DataModule module) {
		Locale locale = getLocale();
		module.setShowName(getMessage(module.getShowName(), locale));
		
		module.getFilterProps().forEach(fp -> {
			String prop = String.join(".", module.getName(), fp.getProp());
			String name = String.join(".", module.getName(), fp.getName());
			String defaultMessage = messageSource.getMessage(name, new Object[0], fp.getName(), locale);
			String formatedName = this.messageSource.getMessage(prop, new Object[0], defaultMessage, locale);
			fp.setName(formatedName);
		});
		
		module.getChildren().forEach(child -> {
			child.setShowName(getMessage(child.getName(), locale));
		});
		
		module.getProps().forEach(mp -> {
			String name = String.join(".", module.getName(), mp.getProp());
			String defaultMessage = getMessage(mp.getName(), locale);
			String formatedName = this.messageSource.getMessage(name, new Object[0], defaultMessage, locale);
			mp.setName(formatedName);
			formatSelect(mp.getValues());
			
			Object defaultValue = mp.getDefaultValue();
			if(defaultValue != null && defaultValue instanceof String) {
				defaultValue = this.messageSource.getMessage((String) defaultValue, new Object[0], (String) defaultValue, locale);
				mp.setDefaultValue(defaultValue);
			}
		});
		
		module.getActions().forEach(action -> {
			String formatName = getMessage(action.getName(), locale);
			action.setName(formatName);
			action.getProps().forEach(actionProp -> {
				formatSelect(actionProp.getValues());
			});
			
			List<Object> values = new ArrayList<Object>();
			for(Object dv : action.getDisableValues()) {
				String disableValue = getMessage(dv.toString(), locale);
				values.add(disableValue);
			}
			action.setDisableValues(values);
		});
	}
	
	public void format(DataModule module, Page<Map<String,Object>> page) {
		this.format(module, page.getContent());
	}
	
	public void format(DataModule module, List<Map<String,Object>> dataList) {
		List<DataViewProp> viewProps = module.getViewProps();
		Map<String,DataViewProp> viewPropsMap = module.getViewProps().stream()
				.collect(Collectors.toMap(DataViewProp::getProp, v -> v, (x,y) -> x));
		Map<String,Formatter> formatters = getPropFormatters(module, viewProps);
		
		format(module);
		dataList.forEach(data -> {
			format(module, viewPropsMap, formatters, data);
		});
	}
	
	public void format(DataModule module, Map<String,Object> data) {
		List<DataViewProp> viewProps = module.getViewProps();
		Map<String,DataViewProp> viewPropsMap = module.getViewProps().stream()
				.collect(Collectors.toMap(DataViewProp::getProp, v -> v, (x, y) -> x));
		Map<String,Formatter> formatters = getPropFormatters(module, viewProps);
		
		format(module);
		format(module, viewPropsMap, formatters, data);
	}
	
	private void format(DataModule module, 
			Map<String,DataViewProp> viewPropsMap, 
			Map<String,Formatter> formatters,  
			Map<String,Object> data) {
		data.forEach((prop, value) -> {
			if(value != null) {
				DataViewProp viewProp = viewPropsMap.get(prop);
				if(value != null && viewProp != null) {
					if(viewProp.getDecoder() != null) {
						value = viewProp.getDecoder().decode(value);
					}
					if(viewProp.getFeature() != Feature.IMAGE && 
							viewProp.getFeature() != Feature.FILE) {
						viewProp.setIfMaxLength(getCharsLength(value.toString()));
					}
				}
				Formatter formatter = formatters.get(prop);
				if(formatter != null) {
					value = formatter.format(value);
				}
				data.put(prop, value);
			}
		});
	}
	
	private void formatSelect(List<Select> values) {
		values.forEach(select -> {
			select.setLabel(getMessage(select.getLabel(), getLocale()));
		});
	}
	
	private String getMessage(String code, Locale locale) {
		return messageSource.getMessage(code, new Object[0], code, locale);
	}
	
	private String formatViewProp(DataModule module, DataViewProp vp) {
		Locale locale = getLocale();
		
		String defaultLabel = String.join(".", module.getName(), vp.getName());
		defaultLabel = this.messageSource.getMessage(defaultLabel, new Object[0], null, locale);
		if(defaultLabel != null) {
			return defaultLabel;
		}
		
		String[] nameArray = vp.getName().split("\\;");
		String first = nameArray[0].split("\\.")[0];
		defaultLabel = String.join(".", module.getName(), first);
		defaultLabel = this.messageSource.getMessage(defaultLabel, new Object[0], null, locale);
		if(defaultLabel != null) {
			return defaultLabel;
		}
		
		StringBuilder nameBuilder = new StringBuilder();
		for(int i = 0; i < nameArray.length; i++) {
			String name = nameArray[i];
			if(i == 0 && nameArray.length > 1) {
				String propName = String.join(".", module.getName(), name);
				String defaultMessage = getMessage(name, locale);
				nameBuilder.append(this.messageSource.getMessage(propName, new Object[0], defaultMessage, locale));
			}
			else {
				String[] lastNameArray = name.split("\\.");
				String lastName = lastNameArray[lastNameArray.length - 1];
				String defaultMessage = getMessage(lastName, locale);
				String propName = lastName;
				if(lastNameArray.length > 1) {
					propName = String.join(".", lastNameArray[0], propName);
				}
				nameBuilder.append(this.messageSource.getMessage(propName, new Object[0], defaultMessage, locale));
			}
		}
		return nameBuilder.toString();
	}
	
	private Map<String,Formatter> getPropFormatters(DataModule module, List<DataViewProp> viewProps) {
		Map<String,Formatter> formatters = new HashMap<String,Formatter>();
		
		viewProps.forEach(vp -> {
			vp.setName(formatViewProp(module, vp));
			vp.setMaxLength(getCharsLength(vp.getName()));
			
			if(Enum.class.isAssignableFrom(vp.getTypeClass())) {
				formatters.put(vp.getProp(), new EnumFormatter());
			}
			else if(Feature.SELECT.equals(vp.getFeature())) {
				formatters.put(vp.getProp(), new EnumFormatter());
			}
			else if(vp.getTypeClass().equals(Float.class) || vp.getTypeClass().equals(Double.class)) {
				formatters.put(vp.getProp(), new DoubleFormatter(vp.getPattern()));
			}
			else if(BigDecimal.class.equals(vp.getTypeClass())) {
				formatters.put(vp.getProp(), new DecimalFormatter(vp.getPattern()));
			}
			else if(Date.class.isAssignableFrom(vp.getTypeClass())) {
				formatters.put(vp.getProp(), new DateFormatter(vp.getPattern()));
			}
		});
		return formatters;
	}
		
	private int getCharsLength(String text) {
		int size = 0;
		for(int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			size += String.valueOf(c).matches(CHINESE) ? 2 : 1;
		}
		return size;
	}
	
	class DateFormatter implements Formatter {
		private String pattern = "yyyy-MM-dd HH:mm:ss";
		DateFormatter(String pattern) {
			if(StringUtils.hasText(pattern)) {
				this.pattern = pattern;
			}
		}
		public String format(Object value) {
			return DateUtils.formatDate((java.util.Date) value, pattern);
		}
	}
	
	class DoubleFormatter implements Formatter {
		private String pattern = "0.00";
		DoubleFormatter(String pattern) {
			this.pattern = pattern == null ? "0.00" : pattern;
		}
		public String format(Object value) {
			value = Double.valueOf(value.toString());
			return new DecimalFormat(pattern).format(value);
		}
	}
	
	class DecimalFormatter implements Formatter {
		private int precision = 2;
		// 0.00  0.000  10
		DecimalFormatter(String pattern) {
			if(pattern != null) {
				int i = pattern.indexOf(".");
				if(i > -1) {
					precision = pattern.length() - (i + 1);
				}
				else {
					precision = 0;
				}
			}
		}
		public String format(Object value) {
			BigDecimal decimal = (BigDecimal) value;
			return decimal.setScale(precision, RoundingMode.HALF_UP).toString();
		}
	}
	
	class EnumFormatter implements Formatter {
		EnumFormatter() {}
		public String format(Object value) {
			return getMessage(value.toString(), getLocale());
		}
	}
}
