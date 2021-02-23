package org.needleframe.workflow.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.needleframe.core.model.Module;
import org.needleframe.workflow.domain.Task;
import org.needleframe.workflow.domain.WorkFlow;
import org.needleframe.workflow.domain.WorkNode;
import org.needleframe.workflow.repository.TaskRepository;
import org.needleframe.workflow.repository.WorkFlowRepository;
import org.needleframe.workflow.repository.WorkNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkFlowService {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private WorkFlowRepository workFlowRepository;
	
	@Autowired
	private WorkNodeRepository workNodeRepository;
	
	@Autowired
	private TaskRepository taskRepository;
	
	@Autowired
	private MessageService messageService;
	
	public WorkFlow getById(Long workFlowId) {
		return workFlowRepository.findById(workFlowId).get();
	}
	
	public void executeIfNecessary(Module module, Map<String,Object> data) {
		List<WorkFlow> workFlows = workFlowRepository.findByModule(module.getName());
		for(WorkFlow workFlow : workFlows) {
			List<WorkNode> workNodes = workNodeRepository.findByWorkFlow(workFlow);
			for(int i = 0; i < workNodes.size(); i++) {
				WorkNode workNode = workNodes.get(i);
				String prop = workNode.getProp();
				String value = data.get(prop) == null ? null : data.get(prop).toString();
				if(workNode.getFromValue().equals(value)) {  // 找到一个匹配的工作节点
					this.sendTask(workNodes, i, module, data);
				}
			}
		}
	}
	
	private void sendTask(List<WorkNode> workNodes, int workNodeIndex, Module module, Map<String,Object> data) {
		WorkNode workNode = workNodes.get(workNodeIndex);
		WorkFlow workFlow = workNode.getWorkFlow();
		Object id = data.get(module.getPk());
		Object title = data.get(module.getTaskProp());
		
		Task task = new Task();
		String subtitle = workNode.getName();
		if(title != null) {
			subtitle += "：" + title;
		}
		task.setSubtitle(subtitle);
		task.setWorkFlow(workFlow);
		task.setWorkNode(workNode);
		task.setAssignDate(new Date());
		task.setAssignee(workNode.getAssigneeUser().getUsername());
		task.setAssigneeUser(workNode.getAssigneeUser());
		task.setModule(module.getName());
		task.setReporter(workFlow.getReporterUser().getUsername());
		task.setReporterUser(workFlow.getReporterUser());
		task.setInstancePk(module.getPk());
		task.setInstanceId(id.toString());
		task.setInstanceTitle(title == null ? null : title.toString());
		taskRepository.save(task);
		
		if(workNodeIndex > 0) {
			WorkNode prevNode = workNodes.get(workNodeIndex - 1);
			List<Task> tasks =
					taskRepository.findByWorkNodeAndModuleAndInstanceId(prevNode, 
							module.getName(), task.getInstanceId());
			if(!tasks.isEmpty()) {
				Task prevTask = tasks.get(0);
				prevTask.setNextTaskId(task.getId());
				task.setPrevTaskId(prevTask.getId());
				taskRepository.save(prevTask);
				taskRepository.save(task);
			}
		}
		
		logger.info("executeIfNecessary(..) => 模块{}#{}找到匹配的工作节点:{}(#{})，新建任务#{}", 
				module.getName(), id, workNode.getName(), workNode.getId(), task.getId());
		
		messageService.sendMessage(task);
	}
	
//	public void executeIfNecessary(Module module, Map<String,Object> data) {
//		List<WorkFlow> workFlows = workFlowRepository.findByModule(module.getName());
//		if(!workFlows.isEmpty()) {
//			WorkFlow workFlow = workFlows.get(0);
//			String prop = workFlow.getProp();
//			List<WorkNode> workNodes = findWorkNodes(workFlow, module, data);
//			String value = data.get(prop) == null ? null : data.get(prop).toString();
//			int index = getMatchedWorkNodeIndex(workNodes, value);
//			
//			if(index > -1) {
//				Object id = data.get(module.getPk());
//				WorkNode workNode = workNodes.get(index);
//				Object title = data.get(module.getTaskProp());
//				
//				Task task = new Task();
//				String subtitle = workNode.getName();
//				if(title != null) {
//					subtitle += "：" + title;
//				}
//				task.setSubtitle(subtitle);
//				task.setWorkFlow(workFlow);
//				task.setWorkNode(workNode);
//				task.setAssignDate(new Date());
//				task.setAssignee(workNode.getAssigneeUser().getUsername());
//				task.setAssigneeUser(workNode.getAssigneeUser());
//				task.setModule(module.getName());
//				task.setReporter(workFlow.getReporterUser().getUsername());
//				task.setReporterUser(workFlow.getReporterUser());
//				task.setInstancePk(module.getPk());
//				task.setInstanceId(id.toString());
//				task.setInstanceTitle(title == null ? null : title.toString());
//				taskRepository.save(task);
//				
//				if(index > 0) {
//					WorkNode prevNode = workNodes.get(index - 1);
//					List<Task> tasks =
//							taskRepository.findByWorkNodeAndModuleAndInstanceId(prevNode, 
//									module.getName(), task.getInstanceId());
//					if(!tasks.isEmpty()) {
//						Task prevTask = tasks.get(0);
//						prevTask.setNextTaskId(task.getId());
//						task.setPrevTaskId(prevTask.getId());
//						taskRepository.save(prevTask);
//						taskRepository.save(task);
//					}
//				}
//				
//				logger.info("executeIfNecessary(..) => 模块{}#{}找到匹配的工作节点:{}(#{})，新建任务#{}", 
//						module.getName(), id, workNode.getName(), workNode.getId(), task.getId());
//				
//				messageService.sendMessage(task);
//			}
//		}
//	}
	
}
