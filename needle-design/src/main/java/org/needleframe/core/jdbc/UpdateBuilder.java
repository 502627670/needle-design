package org.needleframe.core.jdbc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.needleframe.core.exception.ServiceException;
import org.needleframe.core.jdbc.QueryBuilder.SqlClause;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.utils.BeanUtils;
import org.needleframe.utils.DateUtils;
import org.springframework.util.StringUtils;

public class UpdateBuilder {
	
	private Module module;
	
	public UpdateBuilder(Module module) {
		this.module = module;
	}
	
	public SqlClause<List<Object>> build(List<Map<String,Object>> dataList) {
		StringBuilder sqlBuilder = new StringBuilder("update ");
		sqlBuilder.append(module.getTable());
		
		Map<String,Object> dataFirst = dataList.isEmpty() ? new HashMap<String,Object>() : dataList.get(0);
		if(!dataFirst.containsKey(module.getPk())) {
			throw new ServiceException("更新实例必须指定主键");
		}
		
		Map<String,String> propColumnMap = new LinkedHashMap<String,String>();
		module.getProps().values().stream().filter(mp -> !mp.isCollection()).forEach(mp -> {
			String prop = mp.getProp();
			if(!module.getPk().equals(prop)) {
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
			}
		});
		
		String columns = propColumnMap.values().stream().map(col -> col + "=?").collect(Collectors.joining(","));
		sqlBuilder.append(" set ").append(columns);
		sqlBuilder.append(" where ").append(module.getPk()).append("=").append("?");
		
		List<List<Object>> parameters = new ArrayList<List<Object>>();
		dataList.forEach(data -> {
			List<Object> dataRecord = new ArrayList<Object>();
			propColumnMap.keySet().forEach(path -> {
				Object value = data.get(path);
				ModuleProp mp = module.getProp(path);
				if(value != null) {
					if(Enum.class.isAssignableFrom(mp.getType())) {
						value = value.toString();
					}
					else if(!(mp.getType().isAssignableFrom(value.getClass()))) {
						try {
							value = BeanUtils.convert(mp.getType(), value.toString());
						}
						catch(Exception e) {
							throw new ServiceException(mp.getProp() + "转换值失败:" + e.getMessage());
						}
						
						if(java.util.Date.class.isAssignableFrom(mp.getType()) && StringUtils.hasText(mp.getPattern())) {
							String date = DateUtils.formatDate((java.util.Date) value, mp.getPattern());
							value = DateUtils.parseDate(date, mp.getPattern());
						}
					}
				}
				dataRecord.add(value);
			});
			dataRecord.add(data.get(module.getPk()));
			
			parameters.add(dataRecord);
		});
		
		return new SqlClause<List<Object>>(sqlBuilder.toString(), "", parameters);
	}
}
