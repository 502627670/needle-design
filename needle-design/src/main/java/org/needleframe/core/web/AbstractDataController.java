package org.needleframe.core.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.needleframe.context.AppContextService;
import org.needleframe.core.model.ActionData;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.model.ViewFilter;
import org.needleframe.core.model.ViewProp;
import org.needleframe.core.service.Condition;
import org.needleframe.core.service.ServiceDispatcher;
import org.needleframe.core.web.handler.DataHandler;
import org.needleframe.core.web.handler.ResponseHandler;
import org.needleframe.core.web.response.DataModule;
import org.needleframe.core.web.response.DataModuleBuilder;
import org.needleframe.core.web.response.ResponseMessage;
import org.needleframe.core.web.response.ResponseModule;
import org.needleframe.core.web.response.formatter.DataFormatter;
import org.needleframe.utils.BeanUtils;
import org.needleframe.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractDataController {
	
	@Autowired
	protected ServiceDispatcher dataService;
	
	@Autowired
	protected ObjectMapper om;
	
	@Autowired
	protected DataFormatter dataFormatter;
	
	@Autowired
	protected AppContextService appContextService;
	
	protected abstract Module getModule(String moduleName);
	
	public ResponseMessage getData(String moduleName, String id,
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseHandler.doResponse(() -> {
			Module module = getModule(moduleName);
			DataHandler dataHandler = new DataHandler(module, om);
			List<ViewProp> viewProps = dataHandler.getDefaultViewProps();
			Map<String,Object> data = dataService.get(module, viewProps, id);
			DataModule dataModule = new DataModuleBuilder(module).build(viewProps);
			dataFormatter.format(dataModule, data);
			return ResponseModule.success(dataModule, data);
		});
	}
	
	public ResponseMessage findList(String moduleName, 
			String[] _vp,  // viewProps: _vp=name&_vp=role.user.name,role.id
			String _vf,    // viewFilters: _vf=[{prop:name, op:=, value:john},...]
			String _sf,    // viewFilters: _sf=[{prop:name, op:in, value:{SubFilter}},...]
			int _page, 
			int _size,
			String _sort,
			String _direction,
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseHandler.doResponse(() -> {
			Module module = getModule(moduleName);
			DataHandler dataHandler = new DataHandler(module, om);
			List<ViewProp> viewProps = dataHandler.getViewProps(_vp, request);
			List<ViewFilter> viewFilters = dataHandler.getViewFilters(request, _vf, _sf);
			Pageable pageable = PageRequest.of(_page, _size);
			Sort srt = dataHandler.getSort(_sort, _direction);
			
			String id = request.getParameter("_id");
			if(StringUtils.hasText(id)) {
				viewFilters.add(ViewFilter.eq(module.getPk(), id));
			}
			if(StringUtils.hasText(module.getDeletedProp())) {
				viewFilters.add(ViewFilter.neq(module.getDeletedProp(), module.getDeletedValue()));
			}
			Condition condition = new Condition(viewProps, viewFilters, srt);
			Page<Map<String,Object>> dataPage = dataService.findPage(module, condition, pageable);
			viewProps = module.sortShowList(viewProps);
			DataModule dataModule = new DataModuleBuilder(module).build(viewProps);
			dataFormatter.format(dataModule, dataPage);
			return ResponseModule.success(dataModule, dataPage);
		});
	}
	
	public ResponseMessage create(String moduleName, String _data,
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseHandler.doResponse(() -> {
			Module module = getModule(moduleName);
			DataHandler dataHandler = new DataHandler(module, om);
			List<ActionData> _dataList = dataHandler.getCreateData(_data, request);
			List<ActionData> data = dataService.create(module, _dataList);
			DataModule dataModule = new DataModuleBuilder(module).build();
			dataFormatter.format(dataModule);
			return ResponseModule.success(dataModule, data);
		});
	}
	
	public ResponseMessage update(String moduleName, String _data,
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseHandler.doResponse(() -> {
			Module module = getModule(moduleName);
			DataHandler dataHandler = new DataHandler(module, om);
			List<Map<String,Object>> _dataList = dataHandler.getUpdateData(_data, request);
			List<Map<String,Object>> dataList = dataService.update(module, _dataList);
			DataModule dataModule = new DataModuleBuilder(module).build();
			dataFormatter.format(dataModule, dataList);
			return ResponseModule.success(dataModule, dataList);
		});
	}
	
	public ResponseMessage remove(String moduleName, String[] id,
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseHandler.doResponse(() -> {
			Module module = getModule(moduleName);
			DataHandler dataHandler = new DataHandler(module);
			List<Serializable> ids = dataHandler.getDeleteData(id);
			dataService.delete(module, ids.toArray(new Serializable[ids.size()]));
			DataModule dataModule = new DataModuleBuilder(module).build();
			dataFormatter.format(dataModule);
			return ResponseModule.success(dataModule, "ok");
		});
	}
	
	public ResponseMessage preview(String moduleName, String _props, String _data,
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseHandler.doResponse(() -> {
			Module module = getModule(moduleName);
			List<String> _propList = new ArrayList<String>();
			List<Map<String,Object>> _dataList = new ArrayList<Map<String,Object>>();
			if(StringUtils.hasText(_props)) {
				_propList = JsonUtils.fromJSON(_props, new TypeReference<List<String>>() {}, om);
				if(StringUtils.hasText(_data)) {
					_dataList = JsonUtils.fromJSON(_data, new TypeReference<List<Map<String,Object>>>() {}, om);
				}
				_propList.add("_check");
				for(int i = 0; i < _dataList.size(); i++) {
					Map<String,Object> data = _dataList.get(i);
					StringBuilder checkMsgBuilder = new StringBuilder();
					data.forEach((prop, value) -> {
						if(value != null) {
							ModuleProp mp = module.getProp(prop);
							try {
								value = BeanUtils.convert(mp.getType(), value.toString());
							}
							catch(Exception e) {
								value = null;
								checkMsgBuilder.append(prop).append("检查失败：").append(e.getMessage());
							}
							data.put(prop, value);
						}
					});
					data.put("_check", checkMsgBuilder.length() > 0 ? checkMsgBuilder.toString() : "检查通过");
				}
			}
			return ResponseMessage.success(_dataList);
		});
	}
	
	public ResponseMessage saveImport(String moduleName, String _data,
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseHandler.doResponse(() -> {
			Module module = getModule(moduleName);
			List<Map<String,Object>> _dataList = new ArrayList<Map<String,Object>>();
			if(StringUtils.hasText(_data)) {
				_dataList = JsonUtils.fromJSON(_data, new TypeReference<List<Map<String,Object>>>() {}, om);
			}
			List<ActionData> actionDataList = new ArrayList<ActionData>();
			ActionData actionData = new ActionData();
			actionData.setModule(module.getName());
			actionData.setData(_dataList);
			actionDataList.add(actionData);
			List<ActionData> data = dataService.create(module, actionDataList);
			DataModule dataModule = new DataModuleBuilder(module).build();
			dataFormatter.format(dataModule);
			return ResponseModule.success(dataModule, data);
		});
	}
	
}
