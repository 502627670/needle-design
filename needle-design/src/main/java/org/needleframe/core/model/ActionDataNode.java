package org.needleframe.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class ActionDataNode {
	
	private Module module;
	
	private ActionData actionData;
	
	private ModuleProp refParentProp;
	
	private List<ActionDataNode> children = new ArrayList<ActionDataNode>();
	
	public void addChild(ActionDataNode node) {
		this.children.add(node);
	}
	
	public void setRefParentPk(Serializable id) {
		if(actionData != null && refParentProp != null) {
			List<Map<String,Object>> dataList = actionData.getData();
			for(int i = 0; i < dataList.size(); i++) {
				dataList.get(i).put(refParentProp.getProp(), id);
			}
		}
	}
}
