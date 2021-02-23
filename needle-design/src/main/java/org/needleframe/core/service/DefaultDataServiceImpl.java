package org.needleframe.core.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.needleframe.context.AppContextService;
import org.needleframe.context.ModuleContext;
import org.needleframe.context.ModuleViewBuilder;
import org.needleframe.core.jdbc.QueryFilter;
import org.needleframe.core.jdbc.QueryProp;
import org.needleframe.core.jdbc.QuerySort;
import org.needleframe.core.model.ActionData;
import org.needleframe.core.model.ActionDataNode;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.model.ViewFilter;
import org.needleframe.core.model.ViewProp;
import org.needleframe.core.model.ModuleProp.RefModule;
import org.needleframe.core.model.ViewFilter.PropFilter;
import org.needleframe.core.repository.DataRepository;
import org.needleframe.core.service.module.ActionDataService;
import org.needleframe.core.service.module.UniqueCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DefaultDataServiceImpl implements DefaultDataService {

	Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private DataRepository dataRepository;
	
	@Autowired
	private ActionDataService actionDataService;
	
	@Autowired
	private UniqueCheckService uniqueCheckService;
	
	@Autowired
	private AppContextService moduleContextService;
	
	public Map<String,Object> get(Module module, List<ViewProp> viewProps, Serializable id) {
		List<PropFilter> viewFilters = Arrays.asList(new PropFilter(module.getPk(), id));
		ModuleContext moduleContext = moduleContextService.getModuleContext();
		ModuleViewBuilder propRefBuilder = new ModuleViewBuilder(moduleContext);
		List<QueryProp> queryProps = propRefBuilder.buildProps(module, viewProps);
		List<QueryFilter> queryFilters = propRefBuilder.buildFilters(module, viewFilters);
		Map<String,Object> data = dataRepository.getEntity(module, queryProps, queryFilters);
		return data;
	}
	
	@Override
	public Page<Map<String,Object>> findPage(Module module, Condition query, Pageable pageable) {
		ModuleContext moduleContext = moduleContextService.getModuleContext();
		List<ViewProp> viewProps = query.getViewProps();
		List<ViewFilter> viewFilters = query.getViewFilters();
		String boolFilter = query.getBoolFilter();
		Sort sort = query.getSort();
		if(sort == null) {
			sort = module.getSort() != null ? module.getSort() : Sort.by(module.getPk()).descending();
		}
		ModuleViewBuilder propRefBuilder = new ModuleViewBuilder(moduleContext);
		List<QueryProp> queryProps = propRefBuilder.buildProps(module, viewProps);
		List<QueryFilter> queryFilters = propRefBuilder.buildFilters(module, viewFilters);
		List<QuerySort> querySortList = propRefBuilder.buildSort(module, sort);
		Page<Map<String,Object>> resultPage = 
				dataRepository.findPage(module, queryProps, queryFilters, querySortList, boolFilter, pageable);
		return resultPage;
	}
	
	@Override
	public List<Map<String,Object>> findList(Module module, Condition query, Pageable pageable) {
		ModuleContext moduleContext = moduleContextService.getModuleContext();
		List<ViewProp> viewProps = query.getViewProps();
		List<ViewFilter> viewFilters = query.getViewFilters();
		String boolFilter = query.getBoolFilter();
		Sort sort = query.getSort();
		if(sort == null) {
			sort = Sort.by(module.getPk()).descending();
		}
		ModuleViewBuilder propRefBuilder = new ModuleViewBuilder(moduleContext);
		List<QueryProp> queryProps = propRefBuilder.buildProps(module, viewProps);
		List<QueryFilter> queryFilters = propRefBuilder.buildFilters(module, viewFilters);
		List<QuerySort> querySortList = propRefBuilder.buildSort(module, sort);
		return dataRepository.findList(module, queryProps, queryFilters, querySortList, boolFilter, pageable);
	}
	
	@Override
	public List<ActionData> create(Module module, List<ActionData> dataList) {
		List<ActionDataNode> nodes = actionDataService.buildDataNode(dataList);
		nodes.forEach(node -> {
			List<Map<String,Object>> data = node.getActionData().getData();
			if(!data.isEmpty()) {
				uniqueCheckService.check(module, data);
				dataRepository.save(node.getModule(), data);
				logger.info("create(..) => 新建{}", node.getModule().getName());
				saveChildren(node);
			}
		});
		return dataList;
	}

	private void saveChildren(ActionDataNode node) {
		Serializable id = null;
		List<Map<String,Object>> data = node.getActionData().getData();
		if(data.size() == 1) {
			id = (Serializable) data.get(0).get(node.getModule().getPk());
		}
		List<ActionDataNode> children = node.getChildren();
		for(int j = 0; j < children.size(); j++) {
			ActionDataNode childNode = children.get(j);
			List<Map<String,Object>> childData = childNode.getActionData().getData();
			if(!childData.isEmpty()) {
				if(id != null) {
					childNode.setRefParentPk(id);
				}
				uniqueCheckService.check(childNode.getModule(), childData);
				dataRepository.save(childNode.getModule(), childData);
				logger.info("saveChildren(..) => 新建{}", childNode.getModule().getName());
				saveChildren(childNode);
			}
		}
	}
	
	@Override
	public List<Map<String,Object>> update(Module module, List<Map<String,Object>> dataList) {
		dataRepository.update(module, dataList);
		return dataList;
	}
	
	@Override
	public void delete(Module module, Serializable[] ids) {
		if(ids.length > 0) {
			Map<ModuleProp,Module> cascadeChildren = new LinkedHashMap<ModuleProp,Module>(); 
			module.getChildren().forEach((refPropName, child) -> {
				String[] propArray = refPropName.split("\\.");
				refPropName = propArray[propArray.length - 1];
				ModuleProp refMp = child.getProp(refPropName);
				RefModule refModule = refMp.getRefModule();
				if(refModule.isCascadeDel()) {
					cascadeChildren.put(refMp, child);
				}
			});
			
			List<ViewProp> idViewProps = Arrays.asList(new ViewProp(module.getPk()));
			cascadeChildren.forEach((refMp, child) -> {
				for(int i = 0; i < ids.length; i++) {
					Map<String,Object> data = get(module, idViewProps, ids[i]);
					Object value = data.get(refMp.getRefModule().getRefProp());
					
					int count = this.deleteChildren(value, child, refMp);
					logger.info("delete(..) => 删除模块" + module.getName() + "[id=" + ids[i] + "]的子模块" + 
							child.getName() + "[refProp=" + refMp.getProp() + "]总共" + count + "条记录");
				}
			});
			
			if(StringUtils.hasText(module.getDeletedProp())) {
				List<Map<String,Object>> dataList = new ArrayList<Map<String,Object>>();
				for(int i = 0; i < ids.length; i++) {
					Map<String,Object> data = new HashMap<String,Object>();
					data.put(module.getPk(), ids[i]);
					data.put(module.getDeletedProp(), module.getDeletedValue());
					dataList.add(data);
				}
				this.update(module, dataList);
				logger.info("delete(..) => 模块" + module.getName() + "执行逻辑删除，更改删除标记");
			}
			else {
				dataRepository.delete(module, ids);
				logger.info("delete(..) => 模块" + module.getName() + "删除记录");
			}
		}
	}
	
	public int deleteChildren(Object parentValue, Module child, ModuleProp refModuleProp) {
		List<ViewProp> viewProps = Arrays.asList(new ViewProp(child.getPk()));
		List<ViewFilter> viewFilters = Arrays.asList(new PropFilter(refModuleProp.getProp(), parentValue));
		
		if(StringUtils.hasText(child.getDeletedProp())) {
			viewFilters.add(ViewFilter.neq(child.getDeletedProp(), child.getDeletedValue()));
		}
		
		Pageable pageable = PageRequest.of(0, 100);
		Condition dataQuery = new Condition(viewProps, viewFilters);
		List<Map<String,Object>> content = findList(child, dataQuery, pageable);
		int count = 0;
		while(!content.isEmpty()) {
			for(int i = 0; i < content.size(); i++) {
				Map<String,Object> data = content.get(i);
				Serializable id = (Serializable) data.get(child.getPk());
				this.delete(child, new Serializable[] {id});
				count++;
			}
			pageable = PageRequest.of(pageable.getPageNumber() + 1, 100);
			content = findList(child, dataQuery, pageable);
		};
		return count;
	}

}
