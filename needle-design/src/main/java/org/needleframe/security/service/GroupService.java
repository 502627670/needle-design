package org.needleframe.security.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.needleframe.context.AppContextService;
import org.needleframe.context.ModuleContext;
import org.needleframe.core.model.ActionData;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.model.ViewProp;
import org.needleframe.core.repository.GroupRepository;
import org.needleframe.core.service.AbstractDataService;
import org.needleframe.core.service.DefaultDataService;
import org.needleframe.security.SecurityUtils;
import org.needleframe.security.UserDetailsServiceImpl.SessionUser;
import org.needleframe.security.domain.Group;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

@Service
public class GroupService extends AbstractDataService {
	
	@Autowired
	private AppContextService moduleContextService;
	
	@Autowired
	private DefaultDataService dataService;
	
	@Autowired
	private GroupRepository groupRepository;
	
	public GroupService() {
	}
	
	protected Class<?> getModelClass() {
		return Group.class;
	}
	
	@Override
	protected void afterCreate(Module module, List<ActionData> dataList) {
		SessionUser user = SecurityUtils.currentUser();
		Long groupId = user.getGroupId();
		Group userGroup = getById(groupId).orElse(null);
		
		dataList.forEach(actionData -> {
			actionData.getData().forEach(data -> {
				Serializable id = (Serializable) data.get(module.getPk());
				Group group = getById(Long.valueOf(id.toString())).get();
				String path = group.getId() + "-";
				Group parent = group.getParent() == null ? userGroup : group.getParent();
				if(parent != null) {
					while(parent != null) {
						path = parent.getPath() + path;
						int offspringNumber = parent.getOffspringNumber() == null ? 0 : parent.getOffspringNumber();
						parent.setOffspringNumber(offspringNumber + 1);
						saveOrUpdate(parent);
						parent = parent.getParent();
					}
				}
				group.setPath(path);
				group.setOffspringNumber(0);
				
				saveOrUpdate(group);
			});
		});
	}
	
	@Override
	protected void beforeDelete(Module module, Serializable[] ids) {
		for(int i = 0; i < ids.length; i++) {
			Serializable id = ids[i];
			Group group = getById(Long.valueOf(id.toString())).get();
			Group parent = group.getParent();
			while(parent != null) {
				parent.setOffspringNumber(parent.getOffspringNumber() - 1);
				saveOrUpdate(parent);
				parent = parent.getParent();
			}
		}
	}
	
	public Optional<Group> getById(Long id) {
		return groupRepository.findById(id);
	}
	
	public void saveOrUpdate(Group group) {
		groupRepository.save(group);
	}
	
	public void setGroupIfNecessary(SessionUser user, List<ActionData> dataList) {
		ModuleContext mc = moduleContextService.getModuleContext();
		dataList.forEach(actionData -> {
			Module module = mc.getModule(actionData.getModule(), true);
			actionData.getData().forEach(data -> {
				setGroupIfNecessary(user, module, data);
			});
		});
	}
	
	public void setGroupIfNecessary(SessionUser user, Module module, Map<String,Object> data) {
		if(StringUtils.hasText(module.getSecurityGroup())) {
			Group minGroup = getMinGroupForRefs(module, data);
			Long userGroupId = user.getGroupId();
			if(minGroup == null) {
				if(userGroupId != null) {
					Optional<Group> opp = groupRepository.findById(userGroupId);
					if(opp.isPresent()) {
						minGroup = opp.get();
					}
				}
			}
			else {
				if(userGroupId != null) {
					Optional<Group> opp = groupRepository.findById(userGroupId);
					if(opp.isPresent()) {
						if(getLevel(opp.get()) < getLevel(minGroup)) {
							minGroup = opp.get();
						}
					}
				}
			}
			if(minGroup != null) {
				data.put(module.getSecurityGroup(), minGroup.getId());
			}
		}
	}
	
	public Group getMinGroupForRefs(Module module, Map<String,Object> data) {
		ModuleContext mc = moduleContextService.getModuleContext();
		int minLevel = Integer.MAX_VALUE;
		Group minGroup = null;
		List<ModuleProp> mps = module.getProps().values().stream()
				.filter(mp -> mp.isRefParent()).collect(Collectors.toList());
		for(int i = 0; i < mps.size(); i++) {
			ModuleProp mp = mps.get(i);
			Object value = mp.getValue(data);
			if(value == null) continue;
			
			Serializable id = null;
			if(BeanUtils.isSimpleProperty(ClassUtils.getUserClass(value))) {
				id = (Serializable) value;
			}
			else {
				Module refModule = mc.getModule(ClassUtils.getUserClass(value));
				BeanWrapper beanWrapper = new BeanWrapperImpl(value);
				id = (Serializable)  beanWrapper.getPropertyValue(refModule.getPk());
			}
			
			if(id != null && StringUtils.hasText(id.toString().trim())) {
				String refModuleName = mp.getRefModule().getRefModuleName();
				Module ref = mc.getModule(refModuleName, true);
				
				Optional<Group> opp = null;
				if(ref.getModelClass().equals(Group.class)) {
					opp = groupRepository.findById(Long.valueOf(id.toString()));
				}
				else if(StringUtils.hasText(ref.getSecurityGroup())) {
					List<ViewProp> viewProps = ViewProp.from(ref.getPk());
					Map<String,Object> refData= dataService.get(ref, viewProps, id);
					Long groupId = (Long) refData.get(ref.getSecurityGroup());
					if(groupId != null) {
						opp = groupRepository.findById(groupId);
					}
				}
				if(opp != null && opp.isPresent()) {
					Group group = opp.get();
					int level = getLevel(group);
					if(level < minLevel) {
						minLevel = level;
						minGroup = group;
					}
				}
			}
		}
		return minGroup;
	}
	
	private int getLevel(Group group) {
		return -1 * group.getPath().split("-").length;
	}
}
