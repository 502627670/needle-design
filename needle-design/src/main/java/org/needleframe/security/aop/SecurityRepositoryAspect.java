package org.needleframe.security.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.needleframe.context.AppContextService;
import org.needleframe.context.ModuleContext;
import org.needleframe.core.model.Module;
import org.needleframe.security.SecurityUtils;
import org.needleframe.security.UserDetailsServiceImpl.SessionUser;
import org.needleframe.security.service.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Aspect
@Component
public class SecurityRepositoryAspect {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());	
	
	@Autowired
	private AppContextService moduleContextService;
	
	@Autowired
	protected GroupService groupService;
	
	@Before("execution(* org.springframework.data.repository.CrudRepository.save(..)) && args(entity) ")
	public void beforeSave(Object entity) {
		long start = System.currentTimeMillis();
		SessionUser user = SecurityUtils.currentUser();
		Long groupId = user.getGroupId();
		logger.debug("beforeSave(..) => entity={}, login user={}", entity, user.getUsername());
		ModuleContext mc = moduleContextService.getModuleContext();
		Module module = mc.getModule(entity, false);
		if(module != null) {
			if(StringUtils.hasText(module.getSecurityGroup())) {
				module.getBeanWrapper().setPropertyValue(module.getSecurityGroup(), groupId);
			}
		}
		logger.debug("beforeSave(..) => entity={}, time cost {} seconds", entity, (System.currentTimeMillis() - start));
	}
	
}
