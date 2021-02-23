package org.needleframe.workflow.service;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.needleframe.core.model.ActionData;
import org.needleframe.core.model.Module;
import org.needleframe.core.repository.DataRepository;
import org.needleframe.core.service.AbstractDataService;
import org.needleframe.security.SecurityUtils;
import org.needleframe.security.UserDetailsServiceImpl.SessionUser;
import org.needleframe.security.domain.User;
import org.needleframe.utils.BeanUtils;
import org.needleframe.workflow.domain.Step;
import org.needleframe.workflow.domain.Step.StepStatus;
import org.needleframe.workflow.domain.Task;
import org.needleframe.workflow.domain.Task.TaskStatus;
import org.needleframe.workflow.domain.WorkNode;
import org.needleframe.workflow.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Service
public class StepService extends AbstractDataService {

	@Autowired
	private TaskRepository taskRepository;
	
	@Autowired
	private DataRepository dataRepository;
	
	@Autowired
	private WorkFlowService workFlowService;
	
	@Autowired
	private MessageService messageService;
	
	protected Class<?> getModelClass() {
		return Step.class;
	}
	
	@Override
	protected void beforeCreate(Module module, List<ActionData> dataList) {
		SessionUser user = SecurityUtils.currentUser();
		dataList.forEach(actionData -> {
			actionData.getData().forEach(data -> {
				data.put("submitter", user.getUsername());
				data.put("submitDate", new Date());
			});
		});
	}

	@Override
	protected void afterCreate(Module module, List<ActionData> dataList) {
		dataList.forEach(actionData -> {
			actionData.getData().forEach(data -> {
				Object stepStatusString = data.get("stepStatus");
				Object taskId = data.get("task");
				Assert.notNull(stepStatusString, "提交任务执行必须指定任务状态");
				Assert.notNull(taskId, "提交任务执行必须指定任务主键");
				StepStatus stepStatus = StepStatus.valueOf(stepStatusString.toString());
				Task task = taskRepository.findById(Long.valueOf(taskId.toString().trim())).get();
				if(StepStatus.BACK.equals(stepStatus)) {
					User assigneeUser = task.getAssigneeUser();
					User reporterUser = task.getReporterUser();
					String assignee = task.getAssignee();
					String reporter = task.getReporter();
					
					if(task.getBeginDate() == null) {
						task.setBeginDate(new Date());
					}
					
					task.setTaskStatus(TaskStatus.BACK);
					task.setAssignee(reporter);
					task.setAssigneeUser(reporterUser);
					task.setAssignDate(new Date());
					task.setReporter(assignee);
					task.setReporterUser(assigneeUser);
				}
				else if(StepStatus.PROCESS.equals(stepStatus)) {
					task.setTaskStatus(TaskStatus.PROCESS);
					task.setBeginDate(new Date());
				}
				else if(StepStatus.COMPLETE.equals(stepStatus)) {
					TaskStatus originalTaskStatus = task.getTaskStatus();
					if(TaskStatus.BACK.equals(originalTaskStatus)) {
						User assigneeUser = task.getAssigneeUser();
						User reporterUser = task.getReporterUser();
						String assignee = task.getAssignee();
						String reporter = task.getReporter();
						
						task.setTaskStatus(TaskStatus.TODO);
						task.setAssignee(reporter);
						task.setAssigneeUser(reporterUser);
						task.setAssignDate(new Date());
						task.setReporter(assignee);
						task.setReporterUser(assigneeUser);
					}
					else {
						task.setTaskStatus(TaskStatus.COMPLETE);
						task.setCompleteDate(new Date());
						
						if(task.getBeginDate() == null) {
							task.setBeginDate(new Date());
						}
						String moduleName = task.getModule();
						String instanceId = task.getInstanceId();
						
						WorkNode workNode = task.getWorkNode();
						if(workNode != null) {
							Module instanceModule = appContextService.getModuleContext().getModule(moduleName);
							Class<?> idType = instanceModule.getProp(instanceModule.getPk()).getType();
							Serializable id = (Serializable) BeanUtils.convert(idType, instanceId);
							Map<String,Object> instanceData = new HashMap<String,Object>();
							instanceData.put(instanceModule.getPk(), id);
							
							if(StringUtils.hasText(workNode.getNextValue())) {
								Class<?> type = instanceModule.getProp(workNode.getProp()).getType();
								Object nextValue = BeanUtils.convert(type, workNode.getNextValue());
								instanceData.put(workNode.getProp(), nextValue);
								List<Map<String,Object>> instanceList = Arrays.asList(instanceData);
								dataRepository.update(instanceModule, instanceList);
								
								Object instance = dataRepository.getEntity(instanceModule.getModelClass(), id);
								workFlowService.executeIfNecessary(instanceModule, instanceModule.toData(instance));
							}
						}
					}
				}
				taskRepository.save(task);
				messageService.sendMessage(task);
			});
		});
	}
	
}	
