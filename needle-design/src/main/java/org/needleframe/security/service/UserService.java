package org.needleframe.security.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.needleframe.core.model.ActionData;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.service.AbstractDataService;
import org.needleframe.security.domain.User;
import org.needleframe.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserService extends AbstractDataService {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	public PasswordEncoder passwordEncoder;
	
	protected UserService() {
	}
	
	protected Class<?> getModelClass() {
		return User.class;
	}
	
	public User getById(Long id) {
		return userRepository.findById(id).get();
	}
	
	public User getByUsername(String username) {
		if(!StringUtils.hasText(username)) {
			return null;
		}
		List<User> users = userRepository.findByUsername(username);
		return users.isEmpty() ? null : users.get(0);
	}

	@Override
	protected void beforeCreate(Module module, List<ActionData> dataList) {
		ModuleProp passwdProp = module.getProp("password");
		dataList.forEach(actionData -> {
			String actionModule = actionData.getModule();
			if(module.getName().equals(actionModule)) {
				actionData.getData().forEach(data -> {
					User user = module.fromData(data);
					String passwd = passwordEncoder.encode(user.getPassword());
					data.put(passwdProp.getProp(), passwd);
				});
			}
		});
	}

	@Override
	protected void beforeUpdate(Module module, List<Map<String, Object>> dataList) {
		ModuleProp passwdProp = module.getProp("password");
		
		dataList.forEach(userData -> {
			String password = (String) userData.get(passwdProp.getProp());
			if(StringUtils.hasText(password)) {
				Serializable id = (Serializable) userData.get(module.getPk());
				User persist = userRepository.findById(Long.valueOf(id.toString())).get();
				String persistPasswd = persist.getPassword();
				if(!persistPasswd.equals(password)) {
					String passwd = passwordEncoder.encode(password);
					userData.put(passwdProp.getProp(), passwd);
				}
			}
		});
	}
	
}
