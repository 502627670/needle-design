package org.needleframe.core.jdbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.needleframe.core.jdbc.QueryBuilder.SqlClause;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.utils.DateUtils;
import org.springframework.util.StringUtils;

import lombok.Getter;

public class InsertBuilder {
	
	private Module module;
	
	@Getter
	private List<String> columns = new ArrayList<String>();
	
	public InsertBuilder(Module module) {
		this.module = module;
	}
	
	/**
	   *   生成insert sql
	 * @param data   { a: 1, m.id: 2  } 最多支持2级级联
	 * @return
	 */
	
	public SqlClause<List<Object>> build(List<Map<String,Object>> dataList) {
		StringBuilder sqlBuilder = new StringBuilder("insert into ");
		sqlBuilder.append(module.getTable());
		
		Map<String,Object> dataFirst = dataList.isEmpty() ? new HashMap<String,Object>() : dataList.get(0);
		dataFirst.computeIfAbsent(module.getPk(), v -> null);
		
		Map<String,String> propColumnMap = new LinkedHashMap<String,String>();
		module.getProps().values().stream().filter(mp -> !mp.isCollection()).forEach(mp -> {
			String prop = mp.getProp();
			if(dataFirst.containsKey(prop)) {
				propColumnMap.put(prop, mp.getColumn());
			}
			else {
				if(mp.getRefModule() != null) {
					String path = String.join(".", prop, mp.getRefModule().getRefProp());
					if(dataFirst.containsKey(path)) {
						propColumnMap.put(path, mp.getColumn());
					}
				}
			}
		});
		
		columns.clear();
		columns.addAll(propColumnMap.values());
		
		String columnSql = propColumnMap.values().stream().collect(Collectors.joining(","));
		String values = propColumnMap.values().stream().map(column -> "?").collect(Collectors.joining(","));
		sqlBuilder.append("(").append(columnSql).append(") values(").append(values).append(")");
		
		List<List<Object>> parameters = new ArrayList<List<Object>>();
		dataList.forEach(data -> {
			List<Object> dataRecord = new ArrayList<Object>();
			propColumnMap.keySet().forEach(path -> {
				Object value = data.get(path);
				if(value != null) {
					if(value instanceof String && "".equals(value.toString().trim())) {
						value = null;
					}
					String prop = path.split("\\.")[0];
					ModuleProp mp = module.getProp(prop);
					Class<?> type = mp.getType();
					if(Boolean.class.equals(type)) {
						value = new Boolean(value.toString());
					}
					else if(Enum.class.isAssignableFrom(type)) {
						value = value.toString();
					}
					else if(java.util.Date.class.isAssignableFrom(type) && StringUtils.hasText(mp.getPattern())) {
						String date = value.toString();
						if(java.util.Date.class.isAssignableFrom(value.getClass())) {
							date = DateUtils.formatDate((java.util.Date) value, mp.getPattern());
						}
						value = DateUtils.parseDate(date, mp.getPattern());
					}
				}
				dataRecord.add(value);
			});
			parameters.add(dataRecord);
		});
		
		return new SqlClause<List<Object>>(sqlBuilder.toString(), "", parameters);
	}
	
}
