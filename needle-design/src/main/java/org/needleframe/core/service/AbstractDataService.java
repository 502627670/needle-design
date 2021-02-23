package org.needleframe.core.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.needleframe.context.AppContextService;
import org.needleframe.core.model.ActionData;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ViewProp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public abstract class AbstractDataService implements DataService {
	
	Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private ServiceDispatcher serviceDispatcher;
	
	@Autowired
	private DefaultDataService defaultDataService;
	
	@Autowired
	protected AppContextService appContextService;
	
	protected AbstractDataService() {}
	
	protected abstract Class<?> getModelClass();
	
	@PostConstruct
	protected void register() {
		serviceDispatcher.registerService(getModelClass(), this);
	}
	
	protected void beforeCreate(Module module, List<ActionData> dataList) {}
	protected void afterCreate(Module module, List<ActionData> dataList) {}
	
	protected void beforeUpdate(Module module, List<Map<String,Object>> dataList) {}
	protected void afterUpdate(Module module, List<Map<String,Object>> dataList) {}
	
	protected void beforeDelete(Module module, Serializable[] ids) {}
	
	@Override
	public Map<String, Object> get(Module module, List<ViewProp> viewProps, Serializable id) {
		return defaultDataService.get(module, viewProps, id);
	}

	@Override
	public Page<Map<String, Object>> findPage(Module module, Condition query, Pageable pageable) {
		return defaultDataService.findPage(module, query, pageable);
	}

	@Override
	public List<Map<String, Object>> findList(Module module, Condition query, Pageable pageable) {
		return defaultDataService.findList(module, query, pageable);
	}

	@Override
	public List<ActionData> create(Module module, List<ActionData> dataList) {
		beforeCreate(module, dataList);
		List<ActionData> results = defaultDataService.create(module, dataList);
		afterCreate(module, dataList);
		return results;
	}

	@Override
	public List<Map<String, Object>> update(Module module, List<Map<String, Object>> dataList) {
		beforeUpdate(module, dataList);
		List<Map<String, Object>> results = defaultDataService.update(module, dataList);
		afterUpdate(module, dataList);
		return results;
	}

	@Override
	public void delete(Module module, Serializable[] ids) {
		beforeDelete(module, ids);
		defaultDataService.delete(module, ids);
	}
	
}
