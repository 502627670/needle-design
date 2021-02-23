package org.needleframe.security.aop;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.needleframe.context.AppContextService;
import org.needleframe.context.ModuleContext;
import org.needleframe.core.model.Act;
import org.needleframe.core.model.ActionData;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.model.ViewFilter;
import org.needleframe.core.model.ViewFilter.Op;
import org.needleframe.core.model.ViewFilter.PropFilter;
import org.needleframe.core.model.ViewProp;
import org.needleframe.security.SecurityUtils;
import org.needleframe.security.UserDetailsServiceImpl.SessionUser;
import org.needleframe.security.domain.Group;
import org.needleframe.security.domain.Permission;
import org.needleframe.security.domain.Resource.ResourceType;
import org.needleframe.security.domain.Role;
import org.needleframe.security.service.GroupService;
import org.needleframe.security.service.PermissionService;
import org.needleframe.utils.BeanUtils;
import org.needleframe.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Aspect
@Component
public class SecurityServiceAspect {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());	
	
	@Autowired
	private AppContextService moduleContextService;
	
	@Autowired
	private GroupService groupService;
	
	@Autowired
	private PermissionService permissionService;
	
	public SecurityServiceAspect() {}
	
	@Before("execution(* org.needleframe.core.service.DataService.findPage(..)) && "
			+ "args(module,viewProps,viewFilters,..) ")
	public <T> void beforeFind(Module module, List<ViewProp> viewProps, List<ViewFilter> viewFilters) {
		long start = System.currentTimeMillis();
		SessionUser user = SecurityUtils.currentUser();
		logger.debug("beforeFind(..) => module={}, login user={}", module.getName(), user.getUsername());
		Long groupId = user.getGroupId();
		
		if(groupId != null && !SecurityUtils.isAdministrator(user)) {
			Optional<Group> groupOptional = groupService.getById(groupId);
			if(groupOptional.isPresent()) {
				Group group = groupOptional.get();
				if(Group.class.isAssignableFrom(module.getModelClass())) {
					this.adjustGroupFilters(module, group, viewFilters);
				}
				else {
					if(StringUtils.hasText(module.getSecurityGroup())) {
						if(module.isPublicData()) {
							group = getRootGroup(group);
						}
						String groupProp = module.getSecurityGroup();
						ModuleProp mp = module.getProp(groupProp);
						String pathProp = mp.isRefParent() ? 
								groupProp + ".path" : groupProp + ".Group.path";
						ViewFilter filter = ViewFilter.rlike(pathProp, group.getPath());
						viewFilters.add(filter);
					}
				}
			}
		}
		logger.debug("beforeFind(..) => module={}, time cost {} seconds", module.getName(), (System.currentTimeMillis() - start));
	}
	
	@AfterReturning(pointcut="execution(* org.needleframe.core.service.module.ModuleService.findModules(..))", returning="modules")
	public void afterFindModules(List<Module> modules) throws Throwable{
		long start = System.currentTimeMillis();
		ModuleContext mc = moduleContextService.getModuleContext();
		SessionUser user = SecurityUtils.currentUser();
		if(!SecurityUtils.isAdministrator(user)) {
			List<Permission> perms = permissionService.findPermissions(SecurityUtils.currentRoles(), ResourceType.ACTION);
			Map<String,Module> modulesMap = perms.stream()
				.map(p -> mc.getAction(p.getResource(), true).getModule())
				.filter(moduleName -> moduleName != null)
				.map(moduleName -> mc.getModule(moduleName))
				.collect(Collectors.toMap(m -> m.getName(), v -> v, (x, y) -> x));
			
			for(Iterator<Module> iterator = modules.iterator(); iterator.hasNext();) {
				Module module = iterator.next();
				if(!modulesMap.containsKey(module.getName())) {
					iterator.remove();
				}
			}
		}
		logger.debug("afterFindModules(..) => time cost " + (System.currentTimeMillis() - start));
	}
	
	
	@Before("execution(* org.needleframe.core.service.DefaultDataService.create(..)) && args(module,dataList,..) ")
	public <T> void beforeCreate(Module module, List<ActionData> dataList) {
		long start = System.currentTimeMillis();
		SessionUser user = SecurityUtils.currentUser();
		List<Role> roles = SecurityUtils.currentRoles();
		logger.debug("beforeCreate(..) => module={}, login user={}", module.getName(), user.getUsername());
		
		if(!permissionService.hasActionPermission(user, roles, Act.CREATE)) {
			throw new AccessDeniedException("特权不足");
		}
		groupService.setGroupIfNecessary(user, dataList);
		
		ModuleContext mc = moduleContextService.getModuleContext();
		dataList.forEach(actionData -> {
			Module currentModule = mc.getModule(actionData.getModule());
			String createdByProp = currentModule.getCreatedBy();
			Object createdBy = getCreatedBy(currentModule, createdByProp, user);
			String lastModifiedByProp = currentModule.getLastModifiedBy();
			Object lastModifiedBy = getLastModifiedBy(currentModule, lastModifiedByProp, user);
			String createdDateProp = currentModule.getCreatedDate();
			Object createdDate = getCreatedDate(currentModule, createdDateProp);
			String lastModifiedDateProp = currentModule.getLastModifiedDate();
			Object lastModifiedDate = getLastModifiedDate(currentModule, lastModifiedDateProp);
			actionData.getData().forEach(data -> {
				if(createdByProp != null) {
					data.put(createdByProp, createdBy);
				}
				if(lastModifiedByProp != null) {
					data.put(lastModifiedByProp, lastModifiedBy);
				}
				if(createdDateProp != null) {
					data.put(createdDateProp, createdDate);
				}
				if(lastModifiedDateProp != null) {
					data.put(lastModifiedDateProp, lastModifiedDate);
				}
			});
		});
		logger.debug("beforeCreate(..) => module={}, time cost {} seconds", module.getName(), (System.currentTimeMillis() - start));
	}
	
	@Before("execution(* org.needleframe.core.service.DefaultDataService.update(..)) && args(module,dataList,..) ")
	public void beforeUpdate(Module module, List<Map<String,Object>> dataList) {
		long start = System.currentTimeMillis();
		SessionUser user = SecurityUtils.currentUser();
		List<Role> roles = SecurityUtils.currentRoles();
		logger.debug("beforeUpdate(..) => module={}, login user={}", module.getName(), user.getUsername());
		
		if(!permissionService.hasActionPermission(user, roles, Act.UPDATE)) {
			throw new AccessDeniedException("特权不足");
		}
		dataList.forEach(data -> {
			String lastModifiedByProp = module.getLastModifiedBy();
			Object lastModifiedBy = getLastModifiedBy(module, lastModifiedByProp, user);
			String lastModifiedDateProp = module.getLastModifiedDate();
			Object lastModifiedDate = getLastModifiedDate(module, lastModifiedDateProp);
			if(lastModifiedByProp != null) {
				data.put(lastModifiedByProp, lastModifiedBy);
			}
			if(lastModifiedDateProp != null) {
				data.put(lastModifiedDateProp, lastModifiedDate);
			}
		});
		logger.debug("beforeUpdate(..) => module={}, time cost {} seconds", module.getName(), (System.currentTimeMillis() - start));
	}
	
	@Before("execution(* org.needleframe.core.service.DefaultDataService.delete(..)) && args(module,..) ")
	public <T> void beforeDelete(Module module) {
		long start = System.currentTimeMillis();
		SessionUser user = SecurityUtils.currentUser();
		List<Role> roles = SecurityUtils.currentRoles();
		logger.debug("beforeDelete(..) => module={}, login user={}", module.getName(), user.getUsername());
		if(!permissionService.hasActionPermission(user, roles, Act.DELETE)) {
			throw new AccessDeniedException("特权不足");
		}
		logger.debug("beforeDelete(..) => module={}, time cost {} seconds", module.getName(), (System.currentTimeMillis() - start));
	}
	
	private void adjustGroupFilters(Module module, Group group, List<ViewFilter> viewFilters) {
		if(viewFilters.isEmpty()) {
			viewFilters.add(ViewFilter.rlike("path", group.getPath()));
		}
		else {
			ModuleProp parentMp = module.getProps().values().stream()
					.filter(mp -> mp.isRefSelf()).findFirst().get();
			ViewFilter pf = viewFilters.stream()
					.filter(vf -> vf.getProp().startsWith(parentMp.getProp()))
					.findFirst().orElse(null);
			if(pf != null && pf instanceof PropFilter) {
				if(Op.IS_NULL.match(pf.getOp())) {
					pf.setProp(module.getPk());
					pf.setOp(Op.EQUAL);
					((PropFilter) pf).setValue(group.getId());
				}
				else {
					Object value = pf.getValue();
					if(value == null || 
							((value instanceof Collection) && ((Collection<?>) value).isEmpty())) {
						pf.setProp("path");
						pf.setOp(Op.RLIKE);
						((PropFilter) pf).setValue(group.getPath());
					}
				}
			}
		}
	}
	
	private Object getCreatedBy(Module module, String createdByProp, SessionUser user) {
		String createdBy = null;
		if(StringUtils.hasText(createdByProp) && !module.getProp(createdByProp).isRefProp()) {
			createdBy = user.getUsername();
		}
		return createdBy;
	}
	
	private Object getLastModifiedBy(Module module, String lastModifiedByProp, SessionUser user) {
		String lastModifiedBy = null;
		if(StringUtils.hasText(lastModifiedByProp) && !module.getProp(lastModifiedByProp).isRefProp()) {
			lastModifiedBy = user.getUsername();
		}
		return lastModifiedBy;
	}
	
	private Object getCreatedDate(Module module, String createdDateProp) {
		String now = DateUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss");
		Object createdDate = null;
		if(StringUtils.hasText(createdDateProp)) {
			ModuleProp cdMp = module.getProp(createdDateProp);
			createdDate = BeanUtils.convert(cdMp.getType(), now);
		}
		return createdDate;
	}
	
	private Object getLastModifiedDate(Module module, String lastModifiedDateProp) {
		String now = DateUtils.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss");
		Object lastModifiedDate = null;
		if(StringUtils.hasText(lastModifiedDateProp)) {
			ModuleProp mdMp = module.getProp(lastModifiedDateProp);
			lastModifiedDate = BeanUtils.convert(mdMp.getType(), now);
		}
		return lastModifiedDate;
	}
	
	private Group getRootGroup(Group group) {
		while(group.getParent() != null) {
			group = group.getParent();
		}
		return group;
	}
	
//	@Before(value = "@within(org.lightnframe.security.annotation.SecurityMethod) || @annotation(org.lightnframe.security.annotation.SecurityMethod)")
//	public <T> void interceptSecurityAction(JoinPoint joinPoint) throws Throwable {
//		long start = System.currentTimeMillis();
//		Object target = joinPoint.getTarget();
//		Signature sig = joinPoint.getSignature();
//        MethodSignature msig = (MethodSignature) sig;
//        Method method = target.getClass().getMethod(msig.getName(), msig.getParameterTypes());
//        User user = (User) SecurityUtils.currentUser();
//		if(user != null) {
//			User persist = userService.getById(user.getId());
//			user.setCurrentRoleId(persist.getCurrentRoleId());
//			user.setCurrentRole(roleService.getById(user.getCurrentRoleId()));
//			
//	        SecurityMethod securityMethod = method.getAnnotation(SecurityMethod.class);
//	    	if(securityMethod != null) {
//	    		Class<?> clazz = securityMethod.entityClass();
//	    		logger.debug("interceptSecurityAction(..) => objectClass={} security", clazz.getName());
//	    		String actionName = securityMethod.actionName();
//	    		ObjectClass objectClass = objectClassService.getByClassName(clazz.getName());
//	    		ObjectAction objectAction = objectActionService.getByObjectClassAndActionName(objectClass, actionName);
//	    		if(objectAction == null) {
//	    			logger.error("interceptSecurityAction(..) => Unknown ModuleAction: entityClass=" + clazz.getName() + ", actionName=" + actionName);
//	    			throw new ServiceException("Unknown ObjectAction: entityClass=" + clazz.getName() + ", actionName=" + actionName);
//	    		}
//	    		webSecurityService.requireAction(user.getCurrentRole(), objectClass, objectAction);
//	    	}
//		}
//		logger.debug("interceptSecurityAction(..) => time cost " + (System.currentTimeMillis() - start));
//	}
//	
//	@Around("execution(* org.lightnframe.service.entity.ObjectClassService.getObjectClassWithUserActionMask(..))")
//	public ObjectClass interceptMvcGetObjectClassWithUserActionMask(ProceedingJoinPoint pjp) throws Throwable {
//		long start = System.currentTimeMillis();
//		Object[] args = pjp.getArgs();
//		logger.debug("interceptMvcGetObjectClassWithUserActionMask(..) =>[start] objectClass({}) ", args[0]);
//		ObjectClass objectClass = (ObjectClass) pjp.proceed(args);
//		objectClass = (ObjectClass) objectClass.clone();
//		User user = (User) SecurityUtils.currentUser();
//		if(user != null) {
//			objectClass.setUserActionMask(webSecurityService.getActionMask(user.getCurrentRole(), objectClass));
//		}
//		logger.debug("interceptMvcGetObjectClassWithUserActionMask(..) =>[after] objectClass({}) userActionMask={}, time cost {}", objectClass.getLabel(), 
//				objectClass.getUserActionMask(), (System.currentTimeMillis() - start));
//		return objectClass;
//    }
//	
//	@AfterReturning(pointcut="execution(* org.lightnframe.service.entity.ObjectClassService.findNavTabObjectClasses*(..))", returning="objectClasses")
//	public void interceptMvcFindNavTabObjectClasses(List<ObjectClass> objectClasses) throws Throwable {
//		long start = System.currentTimeMillis();
//		logger.debug("interceptMvcFindNavTabObjectClasses(..) =>[start] navTabObjectClasses size is {}", objectClasses.size());
//		User user = (User) SecurityUtils.currentUser();
//		if(user != null) {
//			Map<String,ObjectClass> userNavTabObjectClasses = webSecurityService.findNavTabObjectClasses(user.getCurrentRole());
//			for(Iterator<ObjectClass> iterator = objectClasses.iterator(); iterator.hasNext();) {
//				ObjectClass objectClass = iterator.next();
//				if(userNavTabObjectClasses.containsKey(objectClass.getLabel())) {
//					ObjectClass userNavTabObjectClass = userNavTabObjectClasses.get(objectClass.getLabel());
//					objectClass.setActionMask(userNavTabObjectClass.getActionMask());
//					objectClass.setUserActionMask(userNavTabObjectClass.getUserActionMask());
//					continue;
//				}
//				iterator.remove();
//			}
//		}
//		logger.debug("interceptMvcFindNavTabObjectClasses(..) =>[after] navTabObjectClasses size is {}, time cost {}", objectClasses.size(), (System.currentTimeMillis() - start));
//    }
//	
//	@SuppressWarnings("unchecked")
//	@Around("execution(* org.lightnframe.service.entity.ObjectFieldService.findChildrenObjectFields(..))")
//	public List<ObjectField> interceptFindChildrenObjectFields(ProceedingJoinPoint pjp) throws Throwable {
//		long start = System.currentTimeMillis();
//		List<ObjectField> objectFields = (List<ObjectField>) pjp.proceed(pjp.getArgs());
//		User user = (User) SecurityUtils.currentUser();
//		if(user != null && user.getCurrentRole() != null) {
//			for(Iterator<ObjectField> iterator = objectFields.iterator(); iterator.hasNext();) {
//				ObjectField objectField = iterator.next();
//				ObjectClass objectClass = objectField.getObjectClass();
//				long actionMask = webSecurityService.getActionMask(user.getCurrentRole(), objectClass);
//				if((Action.READ.getMask() & actionMask) != Action.READ.getMask()) {
//					iterator.remove();
//					logger.debug("filterChildrenObjectFields(..) => 无读对象类{}权限，移出infoChildren字段{}", objectClass.getName(), objectField.getField());
//				}
//			}
//		}
//		logger.debug("interceptFindChildrenObjectFields(..) => time cost " + (System.currentTimeMillis() - start));
//		return objectFields;
//	}
//	
//	@Around("execution(* org.lightnframe.service.ActionService.getDataPage(..))")
//	public DataPage<?> interceptGetDataPage(ProceedingJoinPoint pjp) throws Throwable {
//		long start = System.currentTimeMillis();
//		Object[] args = pjp.getArgs();
//		ObjectClass objectClass = (ObjectClass) args[0];
//		logger.debug("interceptGetDataPage(..) => objectClass=[{}]:READ", objectClass.getName());
//		User user = (User) SecurityUtils.currentUser();
//		Map<String,ObjectClass> navTabObjectClasses = new HashMap<String,ObjectClass>();
//		List<Group> userGroups = new ArrayList<Group>(0);
//		if(user != null) {
//			webSecurityService.requireAction(user.getCurrentRole(), objectClass, Action.READ);
//			navTabObjectClasses = webSecurityService.findNavTabObjectClasses(user.getCurrentRole());
//			
//			StringBuilder queryStringBuilder = new StringBuilder("from Group t where (t.groups is null");
//			List<RoleGroupPermission> rops = roleGroupPermissionService.findWhichGroupIsEnableByRole(user.getCurrentRole());
//			if(!rops.isEmpty()) {
//				queryStringBuilder.append(" or (");
//				for(int i = 0; i < rops.size(); i++) {
//					RoleGroupPermission rop = rops.get(i);
//					Group group = rop.getGroup();
//					queryStringBuilder.append("t.groups like '").append("G%[").append(group.getId()).append("]%'");
//					if(i < rops.size() - 1) {
//						queryStringBuilder.append(" or ");
//					}
//				}
//				queryStringBuilder.append(")");
//			}
//			queryStringBuilder.append(")");
//			userGroups = entityRepository.find(Group.class, queryStringBuilder.toString(), Collections.emptyList());
//		}
//		DataPage<?> dataPage = (DataPage<?>) pjp.proceed(pjp.getArgs());
//		dataPage.setNavTabObjectClasses(navTabObjectClasses);
//		dataPage.getObjectView().setUserViewMask(webSecurityService.getRoleObjectViewMask(user.getCurrentRole()));
//		dataPage.setUserGroups(userGroups);
//		logger.debug("interceptGetDataPage(..) => time cost " + (System.currentTimeMillis() - start));
//		return dataPage;
//	}
//	
//	@Around("execution(* org.lightnframe.service.ActionService.getDataForm(..))")
//	public DataForm interceptGetDataForm(ProceedingJoinPoint pjp) throws Throwable {
//		long start = System.currentTimeMillis();
//		Object[] args = pjp.getArgs();
//		ObjectClass objectClass = (ObjectClass) args[0];
//		AbstractEntity<?> t = (AbstractEntity<?>) args[1];
//		Map<String,ObjectClass> navTabObjectClasses = new HashMap<String,ObjectClass>();
//		logger.debug("interceptGetDataForm(..) => objectClass=[{}]:{}", objectClass.getName(), t.isNew() ? "CREATE" : "MODIFY");
//		User user = (User) SecurityUtils.currentUser();
//		if(user != null) {
//			webSecurityService.requireAction(user.getCurrentRole(), objectClass, t.isNew() ? Action.CREATE : Action.MODIFY);
//			navTabObjectClasses = webSecurityService.findNavTabObjectClasses(user.getCurrentRole());
//			objectClass.setUserActionMask(webSecurityService.getActionMask(user.getCurrentRole(), objectClass));
//		}
//		DataForm dataForm = (DataForm) pjp.proceed(pjp.getArgs());
//		dataForm.setNavTabObjectClasses(navTabObjectClasses);
//		logger.debug("interceptGetDataForm(..) => time cost " + (System.currentTimeMillis() - start));
//		return dataForm;
//	}
//	
//	@Around("execution(* org.lightnframe.service.ActionService.getDataInfo(..))")
//	public DataInfo interceptGetDataInfo(ProceedingJoinPoint pjp) throws Throwable {
//		long start = System.currentTimeMillis();
//		Object[] args = pjp.getArgs();
//		ObjectClass objectClass = (ObjectClass) args[0];
//		Object arg2 = args[1];
//		Map<String,ObjectClass> navTabObjectClasses = new HashMap<String,ObjectClass>();
//		logger.debug("interceptGetDataInfo(..) => objectClass=[{}]:READ", objectClass.getName());
//		User user = (User) SecurityUtils.currentUser();
//		if(user != null) {
//			if(!(objectClass.getClassName().equals(User.class.getName()) && user.getId().equals(arg2))) {
//				webSecurityService.requireAction(user.getCurrentRole(), objectClass, Action.READ);
//			}
//			navTabObjectClasses = webSecurityService.findNavTabObjectClasses(user.getCurrentRole());
//			objectClass.setUserActionMask(webSecurityService.getActionMask(user.getCurrentRole(), objectClass));
//		}
//		DataInfo dataInfo = (DataInfo) pjp.proceed(pjp.getArgs());
//		dataInfo.setNavTabObjectClasses(navTabObjectClasses);
//		webSecurityService.requireGroup(user.getCurrentRole(), (AbstractEntity<?>) dataInfo.getDataObject());
//		logger.debug("interceptGetDataInfo(..) => time cost " + (System.currentTimeMillis() - start));
//		return dataInfo;
//	}
//		
//	@Before("execution(* org.lightnframe.service.ActionService.getChild(..)) && args(parentId,childObjectField,..) ")
//	public <T> void interceptGetChild(Long parentId, ObjectField childObjectField) {
//		long start = System.currentTimeMillis();
//		ObjectClass objectClass = childObjectField.getObjectClass();
//		logger.debug("interceptGetChild(..) => objectClass=[{}]:READ", objectClass.getName());
//		User user = (User) SecurityUtils.currentUser();
//		if(user != null) {
//			webSecurityService.requireAction(user.getCurrentRole(), objectClass, Action.READ);
//			objectClass.setUserActionMask(webSecurityService.getActionMask(user.getCurrentRole(), objectClass));
//		}
//		logger.debug("interceptGetChild(..) => time cost " + (System.currentTimeMillis() - start));
//	}
//	
//	/*@AfterReturning(pointcut="execution(* org.lightnframe.service.CrudService.getById(..))", returning="entity")
//	public void interceptCrudServiceGetById(Object entity) throws Throwable{
//		long start = System.currentTimeMillis();
//		ObjectClass objectClass = objectClassService.getByClassName(entity.getClass());
//		logger.debug("interceptGetById(..) => objectClass={}:READ", objectClass.getName());
//		User user = (User) SecurityUtils.currentUser();
//		if(user != null) {
//			webSecurityService.requireAction(user.getCurrentRole(), objectClass, Action.READ);
//			webSecurityService.requireGroup(user.getCurrentRole(), (AbstractEntity<?>) entity);
//		}
//		logger.debug("interceptCrudServiceGetById(..) => time cost " + (System.currentTimeMillis() - start));
//	}
//	
//	@AfterReturning(pointcut="execution(* org.lightnframe.service.EntityService.getById(..))", returning="entity")
//	public void interceptEntityServiceGetById(Object entity) {
//		long start = System.currentTimeMillis();
//		ObjectClass objectClass = objectClassService.getByClassName(entity.getClass());
//		logger.debug("interceptGetById(..) => objectClass={}:READ", objectClass.getName());
//		User user = (User) SecurityUtils.currentUser();
//		if(user != null) {
//			webSecurityService.requireAction(user.getCurrentRole(), objectClass, Action.READ);
//			webSecurityService.requireGroup(user.getCurrentRole(), (AbstractEntity<?>) entity);
//		}
//		logger.debug("interceptEntityServiceGetById(..) => time cost " + (System.currentTimeMillis() - start));
//	}*/
//	
//	@SuppressWarnings("unchecked")
//	@Around("execution(* org.lightnframe.service.EntityService.findObjectsByObjectClass(..))")
//	public Object interceptFindObjectsByObjectClass(ProceedingJoinPoint pjp) throws Throwable {
//		long start = System.currentTimeMillis();
//		Object[] args = pjp.getArgs();
//		ObjectClass objectClass = (ObjectClass) args[0];
//		List<ObjectFilter> objectFilters = (List<ObjectFilter>) args[1];
//		String boolFilter = (String) args[2];
//		logger.debug("interceptFindObjectsByObjectClass(..) => objectClass={}", objectClass.getName());
//		User user = (User) SecurityUtils.currentUser();
//		if(user != null) {
//			int number = 1;
//			StringBuilder filterBuilder = new StringBuilder();
//			if(StringUtils.hasText(boolFilter)) {
//				filterBuilder.append(boolFilter);
//				number = objectFilters.size() + 1;
//			}
//			else {
//				for(int i = 0; i < objectFilters.size(); i++) {
//					filterBuilder.append(number++);
//					filterBuilder.append((i < objectFilters.size() - 1) ? " and " : "");
//				}
//			}
//			List<RoleGroupPermission> rops = roleGroupPermissionService.findWhichGroupIsEnableByRole(user.getCurrentRole());
//			objectFilters.add(new ObjectFilter(objectClass, "groups", String.class.getName(), null, FindType.IS_NULL, null));
//			filterBuilder.append(filterBuilder.length() > 0 ? " and (" : " (").append(number++);
//			for(int i = 0; i < rops.size(); i++) {
//				Group group = rops.get(i).getGroup();
//				objectFilters.add(new ObjectFilter(objectClass, "groups", String.class.getName(), null, FindType.RLIKE, "G%[" + group.getId() + "]%"));
//				filterBuilder.append(" or ").append(number++);
//			}
//			filterBuilder.append(") ");
//			
//			args[0] = objectClass;
//			args[1] = objectFilters;
//			args[2] = filterBuilder.toString();
//		}
//		logger.debug("interceptFindObjectsByObjectClass(..) => time cost " + (System.currentTimeMillis() - start));
//		return pjp.proceed(args);
//	}
//	
//	@Before("execution(* org.lightnframe.service.EntityService.saveBatchAction(..)) && args(objectClass,objectAction,..) ")
//	public void interceptSaveBatchAction(ObjectClass objectClass, ObjectAction objectAction) throws Throwable {
//		long start = System.currentTimeMillis();
//		logger.debug("interceptSaveBatchAction(..) => objectClass={}:Batch_Action {} {}", objectClass.getName(), objectAction.getActionName());
//		User user = (User) SecurityUtils.currentUser();
//		if(user != null) {
//			webSecurityService.requireAction(user.getCurrentRole(), objectClass, objectAction);
//			objectClass.setUserActionMask(webSecurityService.getActionMask(user.getCurrentRole(), objectClass));
//		}
//		logger.debug("interceptSaveBatchAction(..) => time cost " + (System.currentTimeMillis() - start));
//	}
//	
//	@Around("execution(* org.lightnframe.service.EntityService.saveOrUpdate(..)) || execution(* org.lightnframe.service.CrudService.saveOrUpdate(..))")
//	public AbstractEntity<?> interceptSaveOrUpdate(ProceedingJoinPoint pjp) throws Throwable {
//		long start = System.currentTimeMillis();
//		Object[] args = pjp.getArgs();
//		AbstractEntity<?> entity = (AbstractEntity<?>) args[0];
//		Class<?> userClass = ClassUtils.getUserClass(entity.getClass());
//		ObjectClass objectClass = objectClassService.getByClassName(userClass.getName());
//		AbstractEntity<?> source = null;
//		boolean entityIsNew = entity.isNew();
//		if(objectClass == null) {
//			if(entity instanceof ObjectClass) {
//				entity = (AbstractEntity<?>) pjp.proceed(pjp.getArgs());
//				entity.setGroups("G[" + groupService.getAdminGroup().getId() + "]");
//				objectClass = (ObjectClass) entity;
//				objectClassService.update(objectClass);
//			}
//			logger.info("interceptSaveOrUpdate(..) => 实体对象{}不存在objectClass实例，忽略保存前的权限验证", entity.getClass().getName());
//		}
//		else {
//			logger.debug("interceptSaveOrUpdate(..) => objectClass={}, dataObject={}", objectClass.getName(), entity.getId());
//			User user = (User) SecurityUtils.currentUser();
//			if(user != null) {
//				webSecurityService.requireAction(user.getCurrentRole(), objectClass, entityIsNew ? Action.CREATE : Action.MODIFY);
//				RoleClassPermission rcp = roleClassPermissionService.getByRoleAndObjectClass(user.getCurrentRole(), objectClass);
//				if(rcp != null) {
//					String actionDataGroups = rcp.getActionDataGroups();
//					entity.setGroups(actionDataGroups.startsWith("G") ? actionDataGroups : "G" + actionDataGroups);
//					objectClass.setUserActionMask(rcp.getActionMask());
//				}
//			}
//			if(!entityIsNew) {
//				source = entityRepository.getById(entity.getClass(), entity.getId());
//				source = source == null ? null : source.clone();
//			}
//			entity = (AbstractEntity<?>) pjp.proceed(pjp.getArgs());
//		}
//		
//		webAuditService.auditIfNecessary(source, entity, entityIsNew ? Action.CREATE : Action.MODIFY);
//		logger.debug("interceptSaveOrUpdate(..) => time cost " + (System.currentTimeMillis() - start));
//		return entity;
//	}
//	
//	@Around("execution(* org.lightnframe.service.EntityService.delete(..)) || execution(* org.lightnframe.service.CrudService.remove(..))")
//	public AbstractEntity<?> interceptDelete(ProceedingJoinPoint pjp) throws Throwable {
//		long start = System.currentTimeMillis();
//		Object[] args = pjp.getArgs();
//		AbstractEntity<?> entity = (AbstractEntity<?>) args[0];
//		ObjectClass objectClass = objectClassService.getByClassName(entity.getClass().getName());
//		if(objectClass == null) {
//			logger.info("interceptDelete(..) => 实体对象{}不存在，忽略删除前权限验证", entity.getClass().getName());
//		}
//		else {
//			logger.debug("interceptDelete(..) => objectClass={}, dataObject={}", objectClass.getName(), entity.getId());
//			User user = (User) SecurityUtils.currentUser();
//			if(user != null) {
//				webSecurityService.requireAction(user.getCurrentRole(), objectClass, Action.REMOVE);
//				objectClass.setUserActionMask(webSecurityService.getActionMask(user.getCurrentRole(), objectClass));
//			}
//		}
//		pjp.proceed(pjp.getArgs());
//		webAuditService.auditIfNecessary(entity, null, Action.REMOVE);
//		recentObjectService.removeRecentObjectIfNecessary(entity);
//		logger.debug("interceptDelete(..) => time cost " + (System.currentTimeMillis() - start));
//		return entity;
//	}
	
}
