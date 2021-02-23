package org.needleframe.security.aop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.needleframe.context.AppContextService;
import org.needleframe.context.ModuleContext;
import org.needleframe.core.web.response.DataModuleBuilder.DataAction;
import org.needleframe.core.web.response.DataModuleBuilder.DataChild;
import org.needleframe.core.web.response.ResponseModule;
import org.needleframe.security.SecurityUtils;
import org.needleframe.security.UserDetailsServiceImpl.SessionUser;
import org.needleframe.security.domain.Permission;
import org.needleframe.security.domain.Resource.ResourceType;
import org.needleframe.security.domain.Role;
import org.needleframe.security.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice(annotations={Controller.class, RestController.class})
public class ResponseAdvice implements ResponseBodyAdvice<Object> {

	private Logger logger = LoggerFactory.getLogger(ResponseAdvice.class);
	
	@Autowired
	private PermissionService permissionService;
	
	@Autowired
	private AppContextService appContextService;
	
	@Override
	public boolean supports(MethodParameter returnType, 
			Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType,
			MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
			ServerHttpRequest request, ServerHttpResponse response) {
		if(body instanceof ResponseModule) {
			logger.debug("beforeBodyWrite(..) => 返回ResponseModule，执行操作权限过滤");
			ResponseModule responseModule = (ResponseModule) body;
			List<DataChild> children = responseModule.getModule().getChildren();
			List<DataAction> actions = responseModule.getModule().getActions();
			List<DataAction> results = new ArrayList<DataAction>();
			SessionUser currentUser = SecurityUtils.currentUser();
			if(!SecurityUtils.isAdministrator(currentUser)) {
				List<Role> currentRoles = SecurityUtils.currentRoles();
				List<Permission> currentUserPermissions = 
						permissionService.findPermissions(currentRoles, ResourceType.ACTION);
				Map<String,Permission> namePermissions = currentUserPermissions.stream()
					.collect(Collectors.toMap(Permission::getResource, v -> v, (x,y) -> x));
				
				ModuleContext mc = appContextService.getModuleContext();
				List<String> permissionModules = new ArrayList<String>();
				
				mc.getActions().values().forEach(action -> {
					String moduleName = action.getModule();
					if(namePermissions.containsKey(action.getIdentity()) && 
							!permissionModules.contains(moduleName)) {
						permissionModules.add(moduleName);
					}
				});
				
				actions.forEach(action -> {
					if(namePermissions.containsKey(action.getId())) {
						results.add(action);
					}
				});
				
				List<DataChild> permissionChildren = new ArrayList<DataChild>();
				children.forEach(child -> {
					if(permissionModules.contains(child.getName())) {
						permissionChildren.add(child);
					}
				});
				
				responseModule.getModule().setChildren(permissionChildren);
				responseModule.getModule().setActions(results);
				logger.debug("beforeBodyWrite(..) => 操作权限过滤，减少了{}个操作", (results.size() - actions.size()));
			}
		}
		return body;
	}
	
}
