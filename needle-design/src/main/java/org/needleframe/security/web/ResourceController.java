package org.needleframe.security.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.needleframe.core.web.handler.ResponseHandler;
import org.needleframe.core.web.response.ResponseMessage;
import org.needleframe.security.SecurityUtils;
import org.needleframe.security.UserDetailsServiceImpl.SessionUser;
import org.needleframe.security.domain.Permission;
import org.needleframe.security.domain.Resource;
import org.needleframe.security.domain.Role;
import org.needleframe.security.domain.Resource.ResourceType;
import org.needleframe.security.service.PermissionService;
import org.needleframe.security.service.ResourceService;
import org.needleframe.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/data/resource")
public class ResourceController {
		
	@Autowired
	private ResourceService resourceService;
	
	@Autowired
	private PermissionService permissionService;
	
	@Autowired
	private MessageSource messageSource;
	
	@Autowired
	private ObjectMapper om;
	
	@RequestMapping("/list")
	@ResponseBody
	public ResponseMessage list(long roleId,  @PageableDefault Pageable pageable, 
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseHandler.doResponse(() -> {
			SessionUser currentUser = SecurityUtils.currentUser();
			List<Resource> userViewResources = new ArrayList<Resource>();
			List<Resource> userActionResources = new ArrayList<Resource>();
			List<Resource> userMenuResources = new ArrayList<Resource>();
			if(SecurityUtils.isAdministrator(currentUser)) {
				userMenuResources = resourceService.getAllMenuResources();
				userActionResources = resourceService.getAllActionResources();
			}
			else {
				List<Role> currentRoles = SecurityUtils.currentRoles();
				List<Permission> currentUserPermissions = permissionService.findPermissions(currentRoles);
				userViewResources = resourceService.findViewResources(currentUserPermissions);
				userActionResources = resourceService.findActionResources(currentUserPermissions);
				userMenuResources = resourceService.findMenuResources(currentUserPermissions);
			}
			
			List<Role> roles = Arrays.asList(new Role(roleId));
			List<Permission> permissions = permissionService.findPermissions(roles);
			userMenuResources = resourceService.resolveCheckedMenuResources(userMenuResources, permissions);
			userViewResources = resourceService.resolveCheckedViewResources(userViewResources, permissions);
			userActionResources = resourceService.resolveCheckedActionResources(userActionResources, permissions);
			formatActionResources(userActionResources);
			
			ModelMap model = new ModelMap()
				.addAttribute("viewResources", userViewResources)
				.addAttribute("menuResources", userMenuResources)
				.addAttribute("actionResources", userActionResources);
			return ResponseMessage.success(model);
		});
	}
	
	@RequestMapping("/create")
	@ResponseBody
	public ResponseMessage create(Long roleId, String _vr, String _ar, 
			HttpServletRequest request, HttpServletResponse response) {
		return ResponseHandler.doResponse(() -> {
			List<Resource> resourceList = new ArrayList<Resource>();
			if(StringUtils.hasText(_vr)) {
				resourceList = JsonUtils.fromJSON(_vr, new TypeReference<List<Resource>>() {}, om);
			}
			if(StringUtils.hasText(_ar)) {
				resourceList.addAll(JsonUtils.fromJSON(_ar, new TypeReference<List<Resource>>() {}, om));
			}
			
			Role role = new Role(roleId);
			resourceList = getResourceList(resourceList);
			permissionService.savePermission(role, resourceList);
			return ResponseMessage.success(resourceList);
		});
	}
	
	private void formatActionResources(List<Resource> resources) {
		Locale locale = Locale.getDefault();
		Object[] args = new Object[0];
		resources.forEach(resource -> {
			String showName = resource.getShowName();
			showName = messageSource.getMessage(showName, args, showName, locale);
			resource.setShowName(showName);
			
			List<Resource> children = resource.getChildren();
			formatActionResources(children);
		});
	}
	
	private List<Resource> getResourceList(List<Resource> resources) {
		List<Resource> resourceList = new ArrayList<Resource>();
		resources.forEach(resource -> {
			List<Resource> children = resource.getChildren();
			if(ResourceType.ACTION.equals(resource.getResourceType())) {
				if(!children.isEmpty()) {
					children.forEach(child -> {
						child.setShowName(resource.getShowName() + " " + child.getShowName());
						resourceList.add(child);
					});
				}
			}
			else {
				if(children.isEmpty()) {
					resourceList.add(resource);
				}
			}
		});
		return resourceList;
	}
	
//	private List<Resource> getResourceList(List<Resource> resources) {
//		List<Resource> resourceList = new ArrayList<Resource>();
//		resources.forEach(resource -> {
//			List<Resource> children = resource.getChildren();
//			if(children.isEmpty()) {
//				resourceList.add(resource);
//			}
//		});
//		return resourceList;
//	}
}
