package org.needleframe.security.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.needleframe.core.model.Menu;
import org.needleframe.core.model.Menu.MenuItem;
import org.needleframe.core.web.file.FileController;
import org.needleframe.core.web.response.ResponseMessage;
import org.needleframe.security.SecurityUtils;
import org.needleframe.security.UserDetailsServiceImpl.SessionUser;
import org.needleframe.security.domain.Permission;
import org.needleframe.security.domain.Resource.ResourceType;
import org.needleframe.security.domain.Role;
import org.needleframe.security.domain.User;
import org.needleframe.security.service.MenuService;
import org.needleframe.security.service.PermissionService;
import org.needleframe.security.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class UserController {
		
	@Autowired
	private MenuService menuService;
	
	@Autowired
	private PermissionService permissionService;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private FileController fileController;
	
	private void buildMenuItemPermissions(List<MenuItem> menuItems, Map<String,String> viewPermissions) {
		menuItems.forEach(menuItem -> {
			viewPermissions.put(menuItem.getName(), menuItem.getUri());
			buildMenuItemPermissions(menuItem.getChildren(), viewPermissions);
		});
	}
	
	// http://localhost:8080/user/getUserInfo
	@RequestMapping("/user/getUserInfo")
	@ResponseBody
	public ResponseMessage getUserInfo(HttpServletRequest request, HttpServletResponse response) {
		SessionUser sessionUser = SecurityUtils.currentUser();
		User user = userService.getById(sessionUser.getId());
		List<Permission> permissions = new ArrayList<Permission>();
		List<Menu> accessMenus = new ArrayList<Menu>();
		Map<String,String> viewPermissions = new LinkedHashMap<String,String>();
		if(SecurityUtils.isAdministrator(user)) {
			accessMenus = menuService.getAllMenus();
			for(int i = 0; i < accessMenus.size(); i++) {
				Menu menu = accessMenus.get(i);
				viewPermissions.put(menu.getName(), menu.getUri());
				buildMenuItemPermissions(menu.getChildren(), viewPermissions);
			}
		}
		else {
			List<Role> roles = SecurityUtils.currentRoles();
			permissions = permissionService.findPermissions(roles);
			accessMenus = menuService.findAccessableMenus(permissions);
			viewPermissions = permissions.stream()
				.filter(p -> !p.getResourceType().equals(ResourceType.ACTION))
				.collect(Collectors.toMap(Permission::getName, Permission::getResourceUri, (x,y) -> x));
		}
		
		ModelMap model = new ModelMap();
		model.addAttribute("roles", sessionUser.getRoleNames());
		model.addAttribute("username", sessionUser.getUsername());
		model.addAttribute("avatar", "");
		model.addAttribute("introduction", user.getDescription());
		model.addAttribute("accessMenus", accessMenus);
		model.addAttribute("viewPermissions", viewPermissions);
		model.addAttribute("fileHttpServer", fileController.getFileHttpServer(request));
		return ResponseMessage.success(model);
	}
	
//	@RequestMapping("/data/user/create")
//	@ResponseBody
//	public ResponseMessage create(String _data,
//			HttpServletRequest request, HttpServletResponse response) {
//		return ResponseHandler.doResponse(() -> {
//			ModuleContext moduleContext = moduleContextService.getModuleContext();
//			Module module = moduleContext.getModule(User.class, true);
//			DataHandler dataHandler = new DataHandler(module);
//			List<ActionData> _dataList = dataHandler.getCreateData(_data, request);
//			ActionData userData = _dataList.stream()
//				.filter(actionData -> module.getName().equals(actionData.getModule()))
//				.findFirst().orElseThrow(() -> new ServiceException("新建的用户参数不存在"));
//			ModuleProp passwdProp = module.getProp("password");
//			List<ActionData> dataList = dataService.create(module, _dataList, () -> {
//				userData.getData().forEach(data -> {
//					String passwd = passwordEncoder.encode((String) data.get(passwdProp.getProp()));
//					data.put(passwdProp.getProp(), passwd);
//				});
//			}, null);
//			
//			DataModule dataModule = new DataModuleBuilder(module).build();
//			dataFormatter.format(dataModule);
//			return ResponseModule.success(dataModule, dataList);
//		});
//	}
//	
//	@RequestMapping("/data/user/update")
//	@ResponseBody
//	public ResponseMessage update(String _data,
//			HttpServletRequest request, HttpServletResponse response) {
//		return ResponseHandler.doResponse(() -> {
//			ModuleContext moduleContext = moduleContextService.getModuleContext();
//			Module module = moduleContext.getModule(User.class, true);
//			DataHandler dataHandler = new DataHandler(module);
//			List<Map<String,Object>> _dataList = dataHandler.getUpdateData(_data, request);
//			ModuleProp passwdProp = module.getProp("password");
//			List<ViewProp> viewProps = ViewProp.from("password");
//			List<Map<String,Object>> dataList = dataService.update(module, _dataList, () -> {
//				_dataList.forEach(userData -> {
//					String password = (String) userData.get(passwdProp.getProp());
//					if(StringUtils.hasText(password)) {
//						Serializable id = (Serializable) userData.get(module.getPk());
//						Map<String,Object> persistObject = dataService.getEntity(module, viewProps, id);
//						String persistPasswd = (String) persistObject.get(passwdProp.getProp());
//						if(!persistPasswd.equals(password)) {
//							String passwd = passwordEncoder.encode(password);
//							userData.put(passwdProp.getProp(), passwd);
//						}
//					}
//				});
//			}, null);
//			DataModule dataModule = new DataModuleBuilder(module).build();
//			dataFormatter.format(dataModule, dataList);
//			return ResponseModule.success(dataModule, dataList);
//		});
//	}
}
