package org.needleframe.workflow.web;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.needleframe.context.AppContextService;
import org.needleframe.context.ModuleContext;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ViewFilter;
import org.needleframe.core.model.ViewProp;
import org.needleframe.core.service.Condition;
import org.needleframe.core.service.DefaultDataService;
import org.needleframe.core.web.handler.DataHandler;
import org.needleframe.core.web.handler.ResponseHandler;
import org.needleframe.core.web.response.DataModule;
import org.needleframe.core.web.response.DataModuleBuilder;
import org.needleframe.core.web.response.ResponseMessage;
import org.needleframe.core.web.response.ResponseModule;
import org.needleframe.core.web.response.formatter.DataFormatter;
import org.needleframe.security.SecurityUtils;
import org.needleframe.security.UserDetailsServiceImpl.SessionUser;
import org.needleframe.workflow.domain.Message;
import org.needleframe.workflow.domain.Message.MessageStatus;
import org.needleframe.workflow.service.MessageService;
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
public class MessageController {
	
	@Autowired
	private AppContextService appContextService;
	
	@Autowired
	private MessageService messageService;
	
	@Autowired
	private DefaultDataService dataService;
	
	@Autowired
	private DataFormatter dataFormatter;
	
	@Autowired
	private ObjectMapper om;
	
	@RequestMapping("/message/get")
	@ResponseBody
	public ResponseMessage getData( 
			String id,
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseHandler.doResponse(() -> {
			ModuleContext moduleContext = appContextService.getModuleContext();
			Module module = moduleContext.getModule(Message.class, true);
			DataHandler dataHandler = new DataHandler(module, om);
			List<ViewProp> viewProps = dataHandler.getDefaultViewProps();
			Map<String,Object> data = dataService.get(module, viewProps, id);
			Long messageId = Long.valueOf(id);
			messageService.updateMessageStatus(messageId, MessageStatus.READED);
			DataModule dataModule = new DataModuleBuilder(module).build(viewProps);
			dataFormatter.format(dataModule, data);
			return ResponseModule.success(dataModule, data);
		});
	}
	
	@RequestMapping("/message/list")
	@ResponseBody
	public ResponseMessage findList(
			String[] _vp,  // viewProps: _vp=name&_vp=role.user.name,role.id
			String _vf,    // viewFilters: _vf=[{prop:name, op:=, value:john},...]
			String _sf,    // viewFilters: _sf=[{prop:name, op:in, value:{SubFilter}},...]
			String _sort,
			String _direction,
			@RequestParam(defaultValue="0") int _page, 
			@RequestParam(defaultValue="10") int _size,
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseHandler.doResponse(() -> {
			ModuleContext moduleContext = appContextService.getModuleContext();
			Module module = moduleContext.getModule(Message.class, true);
			DataHandler dataHandler = new DataHandler(module, om);
			List<ViewProp> viewProps = dataHandler.getViewProps(_vp, request);
			List<ViewFilter> viewFilters = dataHandler.getViewFilters(request, _vf, _sf);
			SessionUser user = SecurityUtils.currentUser();
			viewFilters.add(ViewFilter.eq("assignee", user.getUsername()));
			Pageable pageable = PageRequest.of(_page, _size);
			Sort sort = dataHandler.getSort(_sort, _direction);
			if(sort == null) {
				sort = Sort.by("assignDate", "id").descending();
			}
			Condition condition = new Condition(viewProps, viewFilters, null, sort);
			Page<Map<String,Object>> dataPage = dataService.findPage(module, condition, pageable);
			DataModule dataModule = new DataModuleBuilder(module).build(viewProps);
			dataFormatter.format(dataModule, dataPage);
			return ResponseModule.success(dataModule, dataPage);
		});
	}
	
}
