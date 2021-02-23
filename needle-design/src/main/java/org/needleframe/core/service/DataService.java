package org.needleframe.core.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.needleframe.core.model.ActionData;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ViewProp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DataService {
	
	public Map<String,Object> get(Module module, List<ViewProp> viewProps, Serializable id);
	
	public Page<Map<String,Object>> findPage(Module module, Condition condition, Pageable pageable);
	
	public List<Map<String,Object>> findList(Module module, Condition condition, Pageable pageable);
	
	public List<ActionData> create(Module module, List<ActionData> dataList);
	
	public List<Map<String,Object>> update(Module module, List<Map<String,Object>> dataList);
	
	public void delete(Module module, Serializable[] ids);
	
}
