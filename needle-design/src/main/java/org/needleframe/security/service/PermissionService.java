package org.needleframe.security.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.needleframe.context.ModuleContext;
import org.needleframe.context.AppContextService;
import org.needleframe.core.model.Act;
import org.needleframe.core.model.Action;
import org.needleframe.security.SecurityUtils;
import org.needleframe.security.UserDetailsServiceImpl.SessionUser;
import org.needleframe.security.domain.Permission;
import org.needleframe.security.domain.Resource;
import org.needleframe.security.domain.Resource.ResourceType;
import org.needleframe.security.domain.Role;
import org.needleframe.security.repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class PermissionService {
	
	@Autowired
	private AppContextService moduleContextService;
	
	@Autowired
	private PermissionRepository permissionRepository;
	
	public boolean hasActionPermission(SessionUser user, List<Role> roles, Act act) {
		if(SecurityUtils.isAdministrator(user)) {
			return true;
		}
		ModuleContext mc = moduleContextService.getModuleContext();
		Map<String,Action> actions = mc.getActions();
		List<Permission> permissionList = findPermissions(roles, ResourceType.ACTION);
		Optional<Permission> opp = permissionList.stream()
			.filter(permission -> actions.containsKey(permission.getResource()))
			.findAny();
		return opp.isPresent();
	}
	
	public List<Permission> findPermissions(List<Role> roles, ResourceType resourceType) {
		List<Permission> permissionList = new ArrayList<Permission>();
		List<Permission> permList = permissionRepository.findByRoleInAndResourceType(roles, resourceType);
		permissionList.addAll(permList);
		return permissionList;
	}
	
	public List<Permission> findPermissions(List<Role> roles) {
		return permissionRepository.findByRoleIn(roles);
	}
	
	public void savePermission(Role role, List<Resource> resources) {
		List<Role> roles = Arrays.asList(role);
		List<Permission> permissionList = permissionRepository.findByRoleIn(roles);
		SessionUser user = SecurityUtils.currentUser();
		if(SecurityUtils.isAdministrator(user)) {
			permissionList.forEach(p -> {
				permissionRepository.delete(p);
			});
		}
		else {
			List<Role> currentRoles = SecurityUtils.currentRoles();
			List<Permission> userPermissions = findPermissions(currentRoles);
			Map<String,Object> userPermissionMap = userPermissions.stream()
				.collect(Collectors.toMap(Permission::getResource, v -> v, (x,y)->x));
			permissionList.forEach(p -> {
				if(userPermissionMap.containsKey(p.getResource())) {
					permissionRepository.delete(p);
				}
			});
		}
		
//		ModuleContext mc = moduleContextService.getModuleContext();
//		Map<String,Action> uriMap = mc.getActions().values().stream()
//				.collect(Collectors.toMap(Action::getUri, v -> v, (x,y) -> x));
		resources.stream().forEach(resource -> {
//			Action action = uriMap.get(resource.getUri());
			Permission permission = new Permission();
			permission.setRole(role);
			String name = StringUtils.hasText(resource.getShowName()) ? 
					resource.getShowName() : resource.getName();
			permission.setName(name);
			permission.setResource(resource.getName());
			permission.setResourceUri(resource.getUri());
			permission.setResourceType(resource.getResourceType());
			permissionRepository.save(permission);
		});
	}
	
}
