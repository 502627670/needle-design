package org.needleframe.security;

import java.util.List;
import java.util.stream.Collectors;

import org.needleframe.core.exception.ServiceException;
import org.needleframe.security.UserDetailsServiceImpl.SessionUser;
import org.needleframe.security.domain.Role;
import org.needleframe.security.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
	
	/**
	 *     如果用户未登录或会话结束，则返回null
	 * @return
	 */
	public static SessionUser getUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if(authentication != null) {
			Object principal = authentication.getPrincipal();
			if(principal != null) {
				if(principal instanceof SessionUser) {
					return (SessionUser) principal;
				}
			}
		}
		return null;
	}
	
	/**
	 *     如果用户不存在，则抛出异常
	 * @return
	 */
	public static SessionUser currentUser() {
		SessionUser user = getUser();
		if(user == null) {
			throw new ServiceException("当前会话不存在");
		}
		return user;
	}
	
	/**
	 *     当前登录用户的角色
	 * @return
	 */
	public static List<Role> currentRoles() {
		SessionUser user = currentUser();
		return user.getRoleIds().stream()
				.map(role -> new Role(role)).collect(Collectors.toList());
	}
	
	/**
	 *     是否是系统管理员用户
	 * @param user
	 * @return
	 */
	public static boolean isAdministrator(User user) {
		return SecurityConfig.ADMIN_USER.equals(user.getUsername());
	}
	
	/**
	 *     是否是系统管理员用户
	 * @param user
	 * @return
	 */
	public static boolean isAdministrator(SessionUser user) {
		return SecurityConfig.ADMIN_USER.equals(user.getUsername());
	}
	
}
