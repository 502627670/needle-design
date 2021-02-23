package org.needleframe.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.needleframe.core.exception.ServiceException;
import org.needleframe.core.model.ViewFilter;
import org.needleframe.core.model.ViewFilter.Op;
import org.needleframe.core.model.ViewFilter.SubFilter;
import org.needleframe.core.model.ViewFilter.SubQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
	
	private static Logger logger = LoggerFactory.getLogger(JsonUtils.class);
	
	private static ObjectMapper om = new ObjectMapper();
	
	public static <T> String toJSON(T obj) {
		return toJSON(obj, om);
	}
	
	public static <T> String toJSON(T obj, ObjectMapper om) {
		try {
			return om.writeValueAsString(obj);
		} 
		catch (JsonProcessingException e) {
			logger.error("toJSON(..) => {}的实例转换为JSON格式失败：{}。obj={}", 
					obj.getClass().getSimpleName(), e.getMessage(), obj);
			throw new ServiceException(obj.getClass().getSimpleName() + "的实例转换为JSON格式失败：" + e.getMessage());
		}
	}
	
	public static <T> T fromJSON(String json, TypeReference<?> ref) {
		return fromJSON(json, ref, om);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T fromJSON(String json, TypeReference<?> ref, ObjectMapper om) {
		try {
			return (T) om.readValue(json, ref);
		} 
		catch (IOException e) {
			e.printStackTrace();
			logger.error("解析{}类的JSON数据失败：{}，JSON={}", 
					ref.getType().getTypeName(), e.getMessage(), json);
			throw new ServiceException("解析" + ref.getType().getTypeName() + "类的JSON数据失败：" + e.getMessage());
		}
	}
	
	public static void main(String[] args) {
		String s = "[{\"prop\":\"role\",\"op\":\"not in\",\"subQuery\":{\"module\":\"userRoles\",\"prop\":\"id\",\"filters\":[{\"prop\":\"user\",\"op\":\"=\",\"value\":3}]}}]";
		
		List<SubFilter> ss = fromJSON(s, new TypeReference<ArrayList<SubFilter>>() {});
		System.out.println(ss);

		SubQuery sq = new SubQuery("userRoles", "id").addFilter("user", 3);
		SubFilter vf = new SubFilter("role", Op.NOT_IN, sq);
		List<ViewFilter> filters = Arrays.asList(vf);
		String json = toJSON(filters);
		System.out.println(json);
		fromJSON(json, new TypeReference<ArrayList<SubFilter>>() {});
	}
	
}
