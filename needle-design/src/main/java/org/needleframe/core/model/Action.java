package org.needleframe.core.model;

import java.util.ArrayList;
import java.util.List;

import org.needleframe.core.model.ModuleProp.Select;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

import lombok.Getter;
import lombok.Setter;
	
@Getter
@Setter
public class Action implements Cloneable {
	
	public static enum ActionStatus {
		EXECUTE,     // 可执行 
		DISABLE;     // 不可执行
	}
	
	private String module;
	
	private String identity;
	
	private String name;
		
	private String uri;
	
	private boolean batchable = false;
	
	private ActionStatus actionStatus = ActionStatus.EXECUTE;
	
	private List<ActionType> actionTypes = new ArrayList<ActionType>();
	
	private List<ActionProp> props = new ArrayList<ActionProp>();
	
	private String disableProp = null;
	
	private List<Object> disableValues = new ArrayList<Object>();
	
	private boolean async = true;
	
	public static Action def(String identity) {
		Action action = new Action();
		action.identity = identity;
		action.setName(identity);
		action.setUri(String.join("/", "/action", identity));
		return action;
	}
	
	public void setIdentity(String identity) {
		this.identity = identity;
	}
	
	public Action addAct(Module module, Act act) {
		Assert.notNull(module, "定义操作目标必须指定相应的模块");
		ActionType actionType = new ActionType();
		actionType.setModule(module.getName());
		actionType.setAct(act);
		getActionTypes().add(actionType);
		if(getModule() == null) {
			setModule(module.getName());
		}
		return this;
	}
	
	public Act getAct() {
		return this.actionTypes.isEmpty() ? null : this.actionTypes.get(0).getAct();
	}
	
	public Action clone() {
		Action copy = new Action();
		BeanUtils.copyProperties(this, copy);
		
		List<ActionType> copyOfActionTypes = new ArrayList<ActionType>();
		actionTypes.forEach(at -> {
			copyOfActionTypes.add(at);
		});
		copy.setActionTypes(copyOfActionTypes);
		
		List<ActionProp> copyOfProps = new ArrayList<ActionProp>();
		props.forEach(prop -> {
			copyOfProps.add(prop.clone());
		});
		copy.setProps(copyOfProps);
		
		return copy;
	}

	
	@Getter
	@Setter
	public static class ActionProp {
		String prop;
		Object value;
		int ruleType;
		List<Select> values = new ArrayList<Select>();
		
		public ActionProp clone() {
			ActionProp clone = new ActionProp();
			clone.setProp(prop);
			clone.setValue(value);
			clone.setRuleType(ruleType);
			clone.setValues(values);
			return this;
		}
	}
}
