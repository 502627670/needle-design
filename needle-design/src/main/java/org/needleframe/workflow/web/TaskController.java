package org.needleframe.workflow.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.needleframe.context.AppContextService;
import org.needleframe.context.ModuleContext;
import org.needleframe.context.ModuleViewBuilder;
import org.needleframe.core.exception.ServiceException;
import org.needleframe.core.model.ActionData;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ViewFilter;
import org.needleframe.core.model.ViewProp;
import org.needleframe.core.service.Condition;
import org.needleframe.core.service.DefaultDataService;
import org.needleframe.core.service.module.ModuleService;
import org.needleframe.core.web.handler.DataHandler;
import org.needleframe.core.web.handler.ResponseHandler;
import org.needleframe.core.web.response.DataModule;
import org.needleframe.core.web.response.DataModuleBuilder;
import org.needleframe.core.web.response.ResponseMessage;
import org.needleframe.core.web.response.ResponseModule;
import org.needleframe.core.web.response.formatter.DataFormatter;
import org.needleframe.security.SecurityUtils;
import org.needleframe.security.UserDetailsServiceImpl.SessionUser;
import org.needleframe.security.domain.User;
import org.needleframe.security.service.UserService;
import org.needleframe.workflow.domain.Task;
import org.needleframe.workflow.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/data")
public class TaskController {
		
	@Autowired
	private ModuleService moduleService;
		
	@Autowired
	private DefaultDataService dataService;
	
	@Autowired
	private TaskService taskService;
	
	@Autowired
	private AppContextService appContextService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private ObjectMapper om;
	
	@Autowired
	private DataFormatter dataFormatter;
	
	@RequestMapping("/task/modules")
	@ResponseBody
	public ResponseMessage findTaskModules() {
		List<Module> modules = moduleService.findModules();
		List<DataModule> dataModules = modules.stream().map(m -> {
				DataModule dm = new DataModule(m);
				dataFormatter.format(dm);
				return dm;
			})
			.collect(Collectors.toList());
		return ResponseMessage.success(dataModules);
	}
	
	@RequestMapping("/task/module")
	@ResponseBody
	public ResponseMessage getTaskModule(String module) {
		Module m = moduleService.getModule(module);
		DataModuleBuilder builder = new DataModuleBuilder(m);
		DataModule dm = builder.build();
		dataFormatter.format(dm);
		return ResponseMessage.success(dm);
	}
	
	/**
	 * @param id
	 * @return
	 */
	@RequestMapping("/task/nexts")
	@ResponseBody
	public ResponseMessage findTaskNexts(Long id) {
		return ResponseHandler.doResponse(() -> {
			List<Task> tasks = taskService.findSelfAndNextTasks(id);
			Module taskModule = appContextService.getModuleContext().getModule(Task.class);
			DataModuleBuilder builder = new DataModuleBuilder(taskModule);
			List<ViewProp> viewProps = ViewProp.from(taskModule);
			new ModuleViewBuilder(appContextService.getModuleContext()).buildProps(taskModule, viewProps);
			DataModule dataModule = builder.build(viewProps);
			List<Map<String,Object>> taskList = new ArrayList<Map<String,Object>>();
			tasks.forEach(task -> {
				Map<String,Object> data = taskModule.toData(task);
				dataFormatter.format(dataModule, data);
				taskList.add(data);
			});
			return ResponseMessage.success(taskList);
		});
	}
	
	// http://localhost:8080/data/role/1
	@RequestMapping("/task/get")
	@ResponseBody
	public ResponseMessage getEntity(@RequestParam("id") Long id,
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseHandler.doResponse(() -> {
			ModuleContext moduleContext = appContextService.getModuleContext();
			Module module = moduleContext.getModule(Task.class, true);
			DataHandler dataHandler = new DataHandler(module, om);
			List<ViewProp> viewProps = dataHandler.getDefaultViewProps();
			Map<String,Object> data = dataService.get(module, viewProps, id);
			String assignee = (String) data.get("assignee");
			String reporter = (String) data.get("reporter");
			User assigneeUser = userService.getByUsername(assignee);
			User reporterUser = userService.getByUsername(reporter);
			if(assigneeUser != null) {
				data.put("assigneeUser", assigneeUser.getId());
				data.put("assigneeUser.username", assigneeUser.getUsername());
			}
			if(reporterUser != null) {
				data.put("reporterUser", reporterUser.getId());
				data.put("reporterUser.username", reporterUser.getUsername());
			}
			DataModule dataModule = new DataModuleBuilder(module).build(viewProps);
			dataFormatter.format(dataModule, data);
			return ResponseModule.success(dataModule, data);
		});
	}
	
	// http://localhost:8080/data/role/list
	@RequestMapping("/task/list")
	@ResponseBody
	public ResponseMessage findList(
			String[] _vp,  // viewProps: _vp=name&_vp=role.user.name,role.id
			String _vf,    // viewFilters: _vf=[{prop:name, op:=, value:john},...]
			String _sf,    // viewFilters: _sf=[{prop:name, op:in, value:{SubFilter}},...]
			String _sort,
			String _direction,
			@RequestParam(defaultValue="false") Boolean _sendTask,
			@RequestParam(defaultValue="0") int _page, 
			@RequestParam(defaultValue="10") int _size,
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseHandler.doResponse(() -> {
			ModuleContext moduleContext = appContextService.getModuleContext();
			Module module = moduleContext.getModule(Task.class, true);
			DataHandler dataHandler = new DataHandler(module, om);
			List<ViewProp> viewProps = dataHandler.getViewProps(_vp, request);
			List<ViewFilter> viewFilters = dataHandler.getViewFilters(request, _vf, _sf);
			String boolFilter = null;
			Optional<ViewFilter> opv = 
					viewFilters.stream().filter(vf -> vf.getProp().startsWith("workFlow")).findAny();
			if(!opv.isPresent()) {
				SessionUser user = SecurityUtils.currentUser();
				if(_sendTask) {
					viewFilters.add(ViewFilter.eq("reporter", user.getUsername()));
				}
				else {
					viewFilters.add(ViewFilter.eq("assignee", user.getUsername()));
				}
			}
			Pageable pageable = PageRequest.of(_page, _size);
			Sort sort = dataHandler.getSort(_sort, _direction);
			if(sort == null) {
				sort = Sort.by("createdDate", "id").descending();
			}
			Condition condition = new Condition(viewProps, viewFilters, boolFilter, sort);
			Page<Map<String,Object>> dataPage = dataService.findPage(module, condition, pageable);
			DataModule dataModule = new DataModuleBuilder(module).build(viewProps);
			dataFormatter.format(dataModule, dataPage);
			return ResponseModule.success(dataModule, dataPage);
		});
	}
	
	@RequestMapping("/task/create")
	@ResponseBody
	public ResponseMessage create(String _data,
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseHandler.doResponse(() -> {
			ModuleContext moduleContext = appContextService.getModuleContext();
			Module module = moduleContext.getModule(Task.class, true);
			DataHandler dataHandler = new DataHandler(module, om);
			List<ActionData> _dataList = dataHandler.getCreateData(_data, request);
			
			_dataList.forEach(actionData -> {
				actionData.getData().forEach(data -> {
					Task task = module.fromData(data);
					User assigneeUser = userService.getById(task.getAssigneeUser().getId());
					User reporterUser = userService.getById(task.getReporterUser().getId());
					data.put("assignee", assigneeUser.getUsername());
					data.put("reporter", reporterUser.getUsername());
					if(task.getAssignDate() == null) {
						data.put("assignDate", new Date());
					}
				});
			});
			
			List<ActionData> dataList = dataService.create(module, _dataList);
			DataModule dataModule = new DataModuleBuilder(module).build();
			dataFormatter.format(dataModule);
			return ResponseModule.success(dataModule, dataList);
		});
	}
	
	@RequestMapping("/task/update")
	@ResponseBody
	public ResponseMessage update(String _data,
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseHandler.doResponse(() -> {
			ModuleContext moduleContext = appContextService.getModuleContext();
			Module module = moduleContext.getModule(Task.class, true);
			DataHandler dataHandler = new DataHandler(module, om);
			List<Map<String,Object>> _dataList = dataHandler.getUpdateData(_data, request);
			
			_dataList.forEach(data -> {
				Task task = module.fromData(data);
				User assigneeUser = userService.getById(task.getAssigneeUser().getId());
				User reporterUser = userService.getById(task.getReporterUser().getId());
				data.put("assignee", assigneeUser.getUsername());
				data.put("reporter", reporterUser.getUsername());
				if(task.getAssignDate() == null) {
					data.put("assignDate", new Date());
				}
			});
			
			List<Map<String,Object>> dataList = dataService.update(module, _dataList);
			DataModule dataModule = new DataModuleBuilder(module).build();
			dataFormatter.format(dataModule, dataList);
			return ResponseModule.success(dataModule, dataList);
		});
	}
	
	
	// http://localhost:8080/data/role/remove?id=1&id=2
//	@RequestMapping("/task/remove")
//	@ResponseBody
//	public ResponseMessage remove(String[] id,
//			HttpServletRequest request, HttpServletResponse response) {
//		return ResponseHandler.doResponse(() -> {
//			ModuleContext moduleContext = appContextService.getModuleContext();
//			Module module = moduleContext.getModule(Task.class, true);
//			DataHandler dataHandler = new DataHandler(module);
//			List<Serializable> ids = dataHandler.getDeleteData(id);
//			dataService.delete(module, ids.toArray(new Serializable[ids.size()]));
//			DataModule dataModule = new DataModuleBuilder(module).build();
//			dataFormatter.format(dataModule);
//			return ResponseModule.success(dataModule, "ok");
//		});
//	}
	
	@RequestMapping("/task/preview")
	@ResponseBody
	public ResponseMessage preview(String _props, String _data,
			HttpServletRequest request, HttpServletResponse response) {
		throw new ServiceException("不支持的功能");
	}
	
	@RequestMapping("/task/saveImport")
	@ResponseBody
	public ResponseMessage saveImport(String _data,
			HttpServletRequest request, HttpServletResponse response) {
		throw new ServiceException("不支持的功能");
	}
	
}
