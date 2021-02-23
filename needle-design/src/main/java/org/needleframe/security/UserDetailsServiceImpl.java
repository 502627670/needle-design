package org.needleframe.security;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.needleframe.core.exception.ServiceException;
import org.needleframe.security.domain.Role;
import org.needleframe.security.domain.User;
import org.needleframe.security.domain.User.UserStatus;
import org.needleframe.security.repository.RoleRepository;
import org.needleframe.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private RoleRepository roleRepository;
	
	@Override
	public SessionUser loadUserByUsername(String username) throws UsernameNotFoundException {
		List<User> users = userRepository.findByUsername(username);
		if(users.isEmpty()) {
			throw new ServiceException("用户" + username + "不存在");
		}
		User user = users.get(0);
		if(SecurityUtils.isAdministrator(user)) {
			return new SessionUser(user, new String[] { SecurityConfig.ADMIN_USER });
		}
		else {
			List<Role> roles = roleRepository.findByUser(user);
			return new SessionUser(user, roles);
		}
	}
	
	@Getter
	@Setter
	public class SessionUser implements org.springframework.security.core.userdetails.UserDetails {
		private static final long serialVersionUID = -8066895165584674094L;
		private Long id;
		private String username;
		private String password;
		private Long groupId;
		private boolean enabled = true;
		private List<Long> roleIds = new ArrayList<Long>();
		private List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		
		private SessionUser(User user, String[] authorities) {
			this.id = user.getId();
			this.username = user.getUsername();
			this.password = user.getPassword();
			this.groupId = user.getGroup() == null ? null : user.getGroup().getId();
			if(!UserStatus.ENABLE.equals(user.getUserStatus())) {
				this.enabled = false;
			}
			for(int i = 0; i < authorities.length; i++) {
				this.authorities.add(new SimpleGrantedAuthority(authorities[i]));
			}
		}
		
		private SessionUser(User user, List<Role> roles) {
			this.id = user.getId();
			this.username = user.getUsername();
			this.password = user.getPassword();
			this.groupId = user.getGroup() == null ? null : user.getGroup().getId();
			if(!UserStatus.ENABLE.equals(user.getUserStatus())) {
				this.enabled = false;
			}
			roles.forEach(role -> {
				this.roleIds.add(role.getId());
				this.authorities.add(new SimpleGrantedAuthority(role.getName()));
			});
		}
		
		public List<String> getRoleNames() {
			return this.authorities.stream()
					.map(auth -> auth.getAuthority())
					.collect(Collectors.toList());
		}
		
		@Override
		public boolean isAccountNonExpired() {
			return true;
		}
		@Override
		public boolean isAccountNonLocked() {
			return true;
		}
		@Override
		public boolean isCredentialsNonExpired() {
			return true;
		}
		@Override
		public boolean isEnabled() {
			return this.enabled;
		}
	}
}
