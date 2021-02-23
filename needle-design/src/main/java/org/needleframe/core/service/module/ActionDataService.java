package org.needleframe.core.service.module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.needleframe.context.ModuleContext;
import org.needleframe.context.AppContextService;
import org.needleframe.core.model.ActionData;
import org.needleframe.core.model.ActionDataNode;
import org.needleframe.core.model.Module;
import org.needleframe.core.model.ModuleProp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ActionDataService {
	
	@Autowired
	private AppContextService moduleContextService;
	
	public List<ActionDataNode> buildDataNode(List<ActionData> dataList) {
		ModuleContext moduleContext = moduleContextService.getModuleContext();
		
		Map<String,ActionDataNode> allNodes = new LinkedHashMap<String,ActionDataNode>();
		Map<String,ActionDataNode> nodes = new LinkedHashMap<String,ActionDataNode>();
		Map<String,ActionData> moduleActionDatas = dataList.stream()
				.collect(Collectors.toMap(ActionData::getModule, v -> v));
		
		dataList.forEach(actionData -> {
			Module currentModule = moduleContext.getModule(actionData.getModule(), true);
			ActionDataNode aNode = null;
			if(allNodes.containsKey(actionData.getModule())) {
				aNode = allNodes.get(actionData.getModule());
			}
			else {
				aNode = new ActionDataNode();
				aNode.setModule(currentModule);
				aNode.setActionData(actionData);
				allNodes.put(actionData.getModule(), aNode);
				nodes.put(actionData.getModule(), aNode);
			}
			
			List<ModuleProp> refProps = currentModule.getProps().values().stream()
				.filter(mp -> mp.isRefParent())
				.collect(Collectors.toList());
			if(!refProps.isEmpty()) {
				for(int i = 0; i < refProps.size(); i++) {
					ModuleProp rmp = refProps.get(i);
					String refModuleName = rmp.getRefModule().getRefModuleName();
					ActionData refActionData = moduleActionDatas.get(refModuleName);
					if(refActionData != null) {
						ActionDataNode parentNode = allNodes.computeIfAbsent(refModuleName, v -> {
							ActionDataNode actionNode = new ActionDataNode();
							actionNode.setModule(moduleContext.getModule(refModuleName, true));
							actionNode.setActionData(refActionData);
							return actionNode;
						});
						
						aNode.setRefParentProp(rmp);
						if(!parentNode.getModule().getName().equals(aNode.getModule().getName())) { // 不是自引用对象
							parentNode.addChild(aNode);
							nodes.computeIfAbsent(refModuleName, v -> parentNode);
							if(nodes.containsKey(actionData.getModule())) {
								nodes.remove(actionData.getModule());
							}
						}
					}
				}
			}
		});
		return new ArrayList<ActionDataNode>(nodes.values());
	}
	
}
