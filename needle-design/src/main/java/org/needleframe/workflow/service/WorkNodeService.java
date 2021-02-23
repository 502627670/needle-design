package org.needleframe.workflow.service;

import java.util.List;
import java.util.Map;

import org.needleframe.core.model.ActionData;
import org.needleframe.core.model.Module;
import org.needleframe.core.service.AbstractDataService;
import org.needleframe.workflow.domain.WorkNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkNodeService extends AbstractDataService {

//	@Autowired
//	private WorkFlowRepository workFlowRepository;
//	
//	@Autowired
//	private AppContextService appContextService;
	
	@Override
	protected Class<?> getModelClass() {
		return WorkNode.class;
	}

	@Override
	protected void beforeCreate(Module module, List<ActionData> dataList) {
//		dataList.forEach(actionData -> {
//			actionData.getData().forEach(data -> {
//				WorkNode workNode = module.fromData(data);
//				data.put("propName", getPropName(workNode));
//			});
//		});
	}

	@Override
	protected void beforeUpdate(Module module, List<Map<String, Object>> dataList) {
//		dataList.forEach(data -> {
//			WorkNode workNode = module.fromData(data);
//			data.put("propName", getPropName(workNode));
//		});
	}
	
//	private String getPropName(WorkNode workNode) {
//		String prop = workNode.getProp();
//		if(StringUtils.hasText(prop)) {
//			WorkFlow workFlow = workNode.getWorkFlow();
//			workFlow = workFlowRepository.findById(workFlow.getId()).get();
//			String projectModuleName = workFlow.getModule();
//			Module projectModule = appContextService.getModuleContext().getModule(projectModuleName);
//			ModuleProp mp = projectModule.getProp(prop);
//			return mp.getName();
//		}
//		return null;
//	}
	
}
