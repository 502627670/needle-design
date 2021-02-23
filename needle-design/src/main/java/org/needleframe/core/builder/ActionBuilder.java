package org.needleframe.core.builder;

import java.util.Arrays;
import java.util.List;

import org.needleframe.context.ModuleFactory.ActionFactory;
import org.needleframe.core.exception.ServiceException;
import org.needleframe.core.model.Act;
import org.needleframe.core.model.Action;
import org.needleframe.core.model.Action.ActionProp;
import org.needleframe.core.model.Action.ActionStatus;
import org.needleframe.core.model.ActionType;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.needleframe.core.model.ModuleProp.RuleType;
import org.needleframe.core.model.ModuleProp.Select;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import lombok.Getter;

public class ActionBuilder {
	
	private ActionFactory af;
	
	private Action action;
	
	public ActionBuilder(Action action, ActionFactory af) {
		this.action = action;
		this.af = af;
	}
	
	public ActionBuilder name(String name) {
		action.setName(name);
		return this;
	}
	
	public ActionBuilder execute(String uri) {
		action.setActionStatus(ActionStatus.EXECUTE);
		return uri(uri);
	}
	
	public ActionBuilder disableFor(String prop, Object... value) {
		action.setDisableProp(prop);
		action.setDisableValues(Arrays.asList(value));
		return this;
	}
	
	public ActionBuilder uri(String uri) {
		action.setUri(uri);
		return this;
	}
	
	public ActionBuilder batchable(boolean batchable) {
		action.setBatchable(batchable);
		return this;
	}
	
	public ActionBuilder async(boolean async) {
		action.setAsync(async);
		return this;
	}
	
	public ActionBuilder addCreate(Class<?> clazz) {
		return addCreate(af.getModule(clazz));
	}
	
	public ActionBuilder addCreate(Module module) {
		return addAct(module, Act.CREATE);
	}
	
	public ActionBuilder addUpdate(Class<?> clazz) {
		return addUpdate(af.getModule(clazz));
	}
	
	public ActionBuilder addUpdate(Module module) {
		return addAct(module, Act.UPDATE);
	}
	
	public ActionBuilder addRemove(Class<?> clazz) {
		return addRemove(af.getModule(clazz));
	}
	
	public ActionBuilder addRemove(Module module) {
		return addAct(module, Act.DELETE);
	}
	
	public ActionBuilder addAct(Class<?> clazz, Act act) {
		Module module = af.getModule(clazz);
		return addAct(module, act);
	}
	
	public ActionBuilder addAct(Module module, Act act) {
		Assert.notNull(module, "定义操作"+ action.getIdentity() + "新增动作" + act + "必须指定相应的模块");
		ActionType actionType = new ActionType();
		actionType.setModule(module.getName());
		actionType.setAct(act);
		action.getActionTypes().add(actionType);
		this.target(module);
		return this;
	}
	
	public ActionBuilder target(Class<?> clazz) {
		return target(af.getModule(clazz));
	}
	
	public ActionBuilder target(Module target) {
		if(StringUtils.hasText(this.action.getModule())) {
			af.getModule(action.getModule()).getActions().remove(action);
		}
		
		action.setModule(target.getName());
		if(!target.getActions().contains(action)) {
			target.getActions().add(action);
		}
		return this;
	}
	
	public ActionPropBuilder addProp(String prop) {
		if(!StringUtils.hasText(this.action.getModule())) {
			throw new ServiceException("操作" + action.getIdentity() + "未绑定模块，不能指定操作属性");
		}
		Module module = af.getModule(action.getModule());
		ModuleProp mp = module.getProp(prop);
		ActionPropBuilder builder = new ActionPropBuilder(mp);
		action.getProps().add(builder.getActionProp());
		return builder;
	}
	
	public ActionBuilder addProps(String... props) {
		if(!StringUtils.hasText(this.action.getModule())) {
			throw new ServiceException("操作" + action.getIdentity() + "未绑定模块，不能指定操作属性");
		}
		Module module = af.getModule(action.getModule());
		for(int i = 0; i < props.length; i++) {
			ModuleProp mp = module.getProp(props[i]);
			ActionPropBuilder builder = new ActionPropBuilder(mp);
			action.getProps().add(builder.getActionProp());
		}
		return this;
	}
	
	@Getter
	public class ActionPropBuilder {
		ActionProp actionProp;
		ActionPropBuilder(ModuleProp mp) {
			this.actionProp = new ActionProp();
			actionProp.setProp(mp.getProp());
		}
		
		public ActionPropBuilder addRule(RuleType ruleType) {
			int mask = ruleType.getMask() | this.actionProp.getRuleType();
			this.actionProp.setRuleType(mask);
			return this;
		}
		
		public ActionPropBuilder addValue(String value) {
			this.actionProp.getValues().add(new Select(value, value));
			return this;
		}
		
		public ActionPropBuilder addValue(String label, String value) {
			this.actionProp.getValues().add(new Select(label, value));
			return this;
		}
		
		public ActionBuilder value(Object value) {
			this.actionProp.setValue(value);
			return ActionBuilder.this;
		}
		
		public ActionBuilder values(List<Select> values) {
			this.actionProp.setValues(values);
			return ActionBuilder.this;
		}
		
		public ActionBuilder required() {
			this.addRule(RuleType.REQUIRED);
			return ActionBuilder.this;
		}
	}
	
	public ActionFactory and() {
		return af;
	}
}
