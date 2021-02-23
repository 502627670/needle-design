package org.needleframe.core.web.handler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.needleframe.core.exception.ServiceException;
import org.needleframe.core.model.ActionData;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.model.ViewFilter;
import org.needleframe.core.model.ViewFilter.Op;
import org.needleframe.core.model.ViewFilter.PropFilter;
import org.needleframe.core.model.ViewFilter.SubFilter;
import org.needleframe.core.model.ViewProp;
import org.needleframe.utils.BeanUtils;
import org.needleframe.utils.JsonUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class DataHandler {
	
	private Module module;
	
	private ObjectMapper om = new ObjectMapper();
	
	public DataHandler(Module module) {
		this.module = module;
		
		om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		om.disable(SerializationFeature.FAIL_ON_SELF_REFERENCES);
		om.disable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS);
		om.disable(SerializationFeature.FLUSH_AFTER_WRITE_VALUE);
		om.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		om.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);
		om.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
	}
	
	public DataHandler(Module module, ObjectMapper om) {
		this.module = module;
		this.om = om;
	}
	
	public Sort getSort(String sortProp, String direction) {
		if(StringUtils.hasText(sortProp)) {
			Sort srt = Sort.by(sortProp).descending();
			if(StringUtils.hasText(direction)) {
				Direction drn = Direction.fromString(direction);
				if(drn.isAscending()) {
					srt = srt.ascending();
				}
				else {
					srt = srt.descending();
				}
			}
			return srt;
		}
		return null;
	}
	
	public List<ActionData> getCreateData(String _data, HttpServletRequest request) {
		List<ActionData> _dataList = new ArrayList<ActionData>();
		if(StringUtils.hasText(_data)) {
			_dataList = JsonUtils.fromJSON(_data, new TypeReference<List<ActionData>>() {}, om);
		}
		Map<String,Object> commonData = getCommonData(request);
		if(!commonData.isEmpty()) {
			ActionData actionData = _dataList.stream()
				.filter(adata -> adata.getModule().equals(module.getName()))
				.findFirst().orElse(new ActionData());
			actionData.setModule(module.getName());
			actionData.getData().add(commonData);
			_dataList.add(actionData);
		}
		
		_dataList.forEach(actionData -> {
			actionData.getData().forEach(data -> {
				module.getProps().forEach((prop, mp) -> {
					if(data.containsKey(prop) && mp.getEncoder() != null) {
						data.put(prop, mp.getEncoder().encode(data.get(prop)));
					}
					Object defaultValue = mp.getDefaultValue();
					if(defaultValue != null && !data.containsKey(prop)) {
						if(mp.getEncoder() != null) {
							defaultValue = mp.getEncoder().encode(defaultValue);
						}
						data.put(prop, defaultValue);
					}
				});
			});
		});
		
		return _dataList;
	}
	
	public List<Map<String,Object>> getUpdateData(String _data, HttpServletRequest request) {
		List<Map<String,Object>> _dataList = new ArrayList<Map<String,Object>>();
		if(StringUtils.hasText(_data)) {
			_dataList = JsonUtils.fromJSON(_data, 
					new TypeReference<List<LinkedHashMap<String,Object>>>() {}, om);
		}
		String pk = module.getPk();
		Map<String,Object> commonData = getCommonData(request);
		if(!commonData.isEmpty()) {
			if(!commonData.containsKey(pk)) {
				throw new ServiceException("更新实例必须指定主键");
			}
			String pkStringValue = commonData.get(pk).toString();
			commonData.put(pk, pkStringValue);
			Map<String,Object> dataMap = _dataList.stream().filter(d -> {
					if(d.get(pk) == null) {
						throw new ServiceException("更新实例必须指定主键");
					}
					return pkStringValue.equals(d.get(pk).toString());
				}).findFirst().orElse(null);
			if(dataMap == null) {
				_dataList.add(commonData);
			}
			else {
				dataMap.putAll(commonData);
			}
		}
		
		_dataList.forEach(data -> {
			module.getProps().forEach((prop, mp) -> {
				if(data.containsKey(prop) && mp.getEncoder() != null) {
					data.put(prop, mp.getEncoder().encode(data.get(prop)));
				}
				
				Object defaultValue = mp.getDefaultValue();
				if(defaultValue != null && !data.containsKey(prop)) {
					if(mp.getEncoder() != null) {
						defaultValue = mp.getEncoder().encode(defaultValue);
					}
					data.put(prop, defaultValue);
				}
			});
		});
		
		return _dataList;
	}
	
	public List<Serializable> getDeleteData(String[] id) {
		ModuleProp pkProp = module.getProp(module.getPk());
		List<Serializable> ids = new ArrayList<Serializable>();
		for(int i = 0; i < id.length; i++) {
			ids.add((Serializable) BeanUtils.convert(pkProp.getType(), id[i]));
		}
		return ids;
	}
	
	private Map<String,Object> getCommonData(HttpServletRequest request) {
		Map<String,Object> data = new HashMap<String,Object>();
		Enumeration<String> enumeration = request.getParameterNames();
		while(enumeration.hasMoreElements()) {
			String name = enumeration.nextElement();
			String value = request.getParameter(name);
			String[] nameArray = name.split("\\.");
			String propName = nameArray[0];
			if(!module.hasProp(propName)) {
				continue;
			}
			ModuleProp mp = module.getProp(propName);
			if(value != null && value.trim().equals("")) {
				value = mp.getDefaultValue() == null ? null : mp.getDefaultValue().toString();
			}
			
			Object dataValue = value;
			if(nameArray.length <= 1) {
				data.put(propName, dataValue);
			}
			else {
				if(mp.isRefParent()) {
					String nextProp = nameArray[1];
					if(mp.getRefModule().getRefProp().equals(nextProp)) {
						data.put(propName, dataValue);
					}
				}
			}
		}
		return data;
	}
	
	public List<ViewProp> getDefaultViewProps() {
		List<ViewProp> viewProps = new ArrayList<ViewProp>();
		module.getProps().forEach((name, mp) -> {
			if(!mp.isTransientProp() && !Collection.class.isAssignableFrom(mp.getType())) {
				viewProps.add(new ViewProp(mp.getProp()));
			}
		});
		return viewProps;
	}
	
	public List<ViewProp> getViewProps(String[] vps, HttpServletRequest request) {
		List<ViewProp> viewProps = new ArrayList<ViewProp>();
		if(vps == null || vps.length == 0) {
			viewProps.addAll(getDefaultViewProps());
		}
		else {
			for(int i = 0; i < vps.length; i++) {
				String vp = vps[i];
				if(StringUtils.hasText(vp)) {
					String[] vpArray = vp.split(",");
					for(int j = 0; j < vpArray.length; j++) {
						if(StringUtils.hasText(vpArray[j])) {
							viewProps.add(new ViewProp(vpArray[j]));
						}
					}
				}
			}
		}
		return viewProps;
	}
	
	public List<ViewFilter> getViewFilters(HttpServletRequest request, String _vf, String _sf) {
		List<ViewFilter> viewFilters = new ArrayList<ViewFilter>();
		Enumeration<String> enumeration = request.getParameterNames();
		while(enumeration.hasMoreElements()) {
			String name = enumeration.nextElement();
			String value = request.getParameter(name);
			if(StringUtils.hasText(value)) {
				String[] nameArray = name.split("\\.");
				String propName = nameArray[0];
				if(module.hasProp(propName)) {
					viewFilters.add(new PropFilter(name, value));
				}
			}
		}
		if(StringUtils.hasText(_vf)) {
			viewFilters.addAll(JsonUtils.fromJSON(_vf, new TypeReference<List<PropFilter>>() {}, om));
		}
		if(StringUtils.hasText(_sf)) {
			viewFilters.addAll(JsonUtils.fromJSON(_sf, new TypeReference<ArrayList<SubFilter>>() {}, om));
		}
		return viewFilters.stream().filter(vf -> {
			if(vf.getValue() != null && vf.getValue().toString().trim() != "") {
				return true;
			}
			if(Op.IS_NULL.match(vf.getOp())) {
				return true;
			}
			return false;
		}).collect(Collectors.toList());
	}
	
}
