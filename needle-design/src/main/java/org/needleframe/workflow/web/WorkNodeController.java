package org.needleframe.workflow.web;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.needleframe.context.AppContextService;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.model.ModuleProp.Feature;
import org.needleframe.core.model.ModuleProp.Select;
import org.needleframe.core.model.ViewFilter;
import org.needleframe.core.model.ViewProp;
import org.needleframe.core.service.Condition;
import org.needleframe.core.web.AbstractDataController;
import org.needleframe.core.web.handler.DataHandler;
import org.needleframe.core.web.response.DataModule;
import org.needleframe.core.web.response.DataModuleBuilder;
import org.needleframe.core.web.response.ResponseMessage;
import org.needleframe.core.web.response.ResponseModule;
import org.needleframe.workflow.domain.WorkFlow;
import org.needleframe.workflow.domain.WorkNode;
import org.needleframe.workflow.service.WorkFlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
@Controller
@RequestMapping("/data")
public class WorkNodeController extends AbstractDataController {
	
	@Autowired
	private AppContextService appContextService;
	
	@Autowired
	private WorkFlowService workFlowService;
	
	@Override
	protected Module getModule(String moduleName) {
		return appContextService.getModuleContext().getModule(WorkNode.class);
	}
	
	@RequestMapping("/workNode/get")
	@ResponseBody
	public ResponseMessage getData(@RequestParam("id") Long id,
			HttpServletRequest request, HttpServletResponse response) {
		Module module = getModule("workNode");
		DataHandler dataHandler = new DataHandler(module, om);
		List<ViewProp> viewProps = dataHandler.getDefaultViewProps();
		Map<String,Object> data = dataService.get(module, viewProps, id);
		
		WorkNode workNode = module.fromData(data);
		WorkFlow workFlow = workFlowService.getById(workNode.getWorkFlow().getId());
		resolveWorkNodeProp(module, workFlow);
		
		DataModule dataModule = new DataModuleBuilder(module).build(viewProps);
		dataFormatter.format(dataModule, data);
		return ResponseModule.success(dataModule, data);
	}
	
	// http://localhost:8080/data/role/list
	@RequestMapping("/workNode/list")
	@ResponseBody
	public ResponseMessage findList(
			String[] _vp,  // viewProps: _vp=name&_vp=role.user.name,role.id
			String _vf,    // viewFilters: _vf=[{prop:name, op:=, value:john},...]
			String _sf,    // viewFilters: _sf=[{prop:name, op:in, value:{SubFilter}},...]
			@RequestParam(defaultValue="0") int _page, 
			@RequestParam(defaultValue="10") int _size,
			String _sort,
			String _direction,
			HttpServletRequest request, HttpServletResponse response) {
		Module module = getModule("workNode");
		DataHandler dataHandler = new DataHandler(module, om);
		List<ViewProp> viewProps = dataHandler.getViewProps(_vp, request);
		List<ViewFilter> viewFilters = dataHandler.getViewFilters(request, _vf, _sf);
		Pageable pageable = PageRequest.of(_page, _size);
		Sort srt = dataHandler.getSort(_sort, _direction);
		Condition condition = new Condition(viewProps, viewFilters, srt);
		Page<Map<String,Object>> dataPage = dataService.findPage(module, condition, pageable);
		viewProps = module.sortShowList(viewProps);
		
		Optional<ViewFilter> optionalViewFilter = viewFilters.stream().filter(vf -> vf.getProp().startsWith("workFlow")).findFirst();
		if(optionalViewFilter.isPresent()) {
			Object value = optionalViewFilter.get().getValue();
			if(value != null && StringUtils.hasText(value.toString())) {
				Long workFlowId = Long.valueOf(value.toString());
				WorkFlow workFlow = workFlowService.getById(workFlowId);
				resolveWorkNodeProp(module, workFlow);
			}
		}
		
		DataModule dataModule = new DataModuleBuilder(module).build(viewProps);
		dataFormatter.format(dataModule, dataPage);
		return ResponseModule.success(dataModule, dataPage);
	}
	
	@RequestMapping("/workNode/create")
	@ResponseBody
	public ResponseMessage create(String _data,
			HttpServletRequest request, HttpServletResponse response) {
		Module module = getModule("workNode");
		String moduleName = module.getName();
		return super.create(moduleName, _data, request, response);
	}
	
	@RequestMapping("/workNode/update")
	@ResponseBody
	public ResponseMessage update(String _data,
			HttpServletRequest request, HttpServletResponse response) {
		Module module = getModule("workNode");
		String moduleName = module.getName();
		return super.update(moduleName, _data, request, response);
	}
	
	@RequestMapping("/workNode/remove")
	@ResponseBody
	public ResponseMessage remove(String[] id,
			HttpServletRequest request, HttpServletResponse response) {
		Module module = getModule("workNode");
		String moduleName = module.getName();
		return super.remove(moduleName, id, request, response);
	}
	
	@RequestMapping("/workNode/preview")
	@ResponseBody
	public ResponseMessage preview(String _props, String _data,
			HttpServletRequest request, HttpServletResponse response) {
		Module module = getModule("workNode");
		String moduleName = module.getName();
		return super.preview(moduleName, _props, _data, request, response);
	}
	
	@RequestMapping("/workNode/saveImport")
	@ResponseBody
	public ResponseMessage saveImport(String _data,
			HttpServletRequest request, HttpServletResponse response) {
		Module module = getModule("workNode");
		String moduleName = module.getName();
		return super.saveImport(moduleName, _data, request, response);
	}
	
	private void resolveWorkNodeProp(Module module, WorkFlow workFlow) {
		String pojectModuleName = workFlow.getModule();
		Module projectModule = appContextService.getModuleContext().getModule(pojectModuleName);
		
		List<Select> values = projectModule.getProps().values().stream()
				.filter(mp -> !mp.isAuditProp() && !mp.isSecurityGroup() && !mp.isSystemProp() &&
						!mp.isCollection() && !mp.isPk() && !mp.isTransientProp())
				.map(mp -> new Select(mp.getName(), mp.getProp()))
				.collect(Collectors.toList());
		ModuleProp mp = module.getProp("prop");
		mp.feature(Feature.SELECT);
		mp.setValues(values);
	}
	
}
