package org.needleframe.workflow.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.needleframe.core.model.ActionData;
import org.needleframe.core.model.Module;
import org.needleframe.core.service.AbstractDataService;
import org.needleframe.workflow.domain.Task;
import org.needleframe.workflow.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class TaskService extends AbstractDataService {
	
	@Autowired
	private TaskRepository taskRepository;
	
	protected Class<?> getModelClass() {
		return Task.class;
	}
	
	public Task getById(Long id) {
		return taskRepository.findById(id).orElse(null);
	}
	
	@Override
	protected void beforeCreate(Module module, List<ActionData> dataList) {
		dataList.forEach(actionData -> {
			actionData.getData().forEach(data -> {
				String assignee = (String) data.get("assignee");
				if(StringUtils.hasText(assignee)) {
					data.put("assignDate", new Date());
				}
			});
		});
	}
	
	@Override
	protected void afterCreate(Module module, List<ActionData> dataList) {
		saveNextIfHasPrev(module, dataList);
	}
	
	@Override
	protected void beforeUpdate(Module module, List<Map<String, Object>> dataList) {
		dataList.forEach(data -> {
			String assignee = (String) data.get("assignee");
			if(StringUtils.hasText(assignee)) {
				data.put("assignDate", new Date());
			}
		});
	}
	
	public List<Task> findSelfAndNextTasks(Long taskId) {
		List<Task> nextList = new ArrayList<Task>();
		Optional<Task> opt = taskRepository.findById(taskId);
		if(opt.isPresent()) {
			Task task = opt.get();
			nextList.add(task);
			int loopSize = 0;
			while(task.getNextTaskId() != null && task.getNextTaskId() != task.getId()) {
				Optional<Task> next = taskRepository.findById(task.getNextTaskId());
				if(!next.isPresent()) break;
				task = next.get();
				nextList.add(task);
				if(++loopSize > 20) break;   // 防止数据错误造成死循环
			}
		}
		return nextList;
	}
		
	private void saveNextIfHasPrev(Module module, List<ActionData> _dataList) {
		_dataList.forEach(actionData -> {
			actionData.getData().forEach(data -> {
				saveNextIfHasPrev(module, data);
			});
		});
	}
	
	private void saveNextIfHasPrev(Module module, Map<String,Object> data) {
		Object idObj = data.get(module.getPk());
		Object prevTaskIdObj = data.get("prevTaskId");
		if(idObj != null && !idObj.toString().trim().equals("")) {
			Long taskId = Long.valueOf(idObj.toString().trim());
			if(prevTaskIdObj != null && !prevTaskIdObj.toString().trim().equals("")) {
				Long prevTaskId = Long.valueOf(prevTaskIdObj.toString().trim());
				Task prev = taskRepository.findById(prevTaskId).orElse(null);
				if(prev != null) {
					prev.setNextTaskId(taskId);
					taskRepository.save(prev);
				}
			}
		}
	}
	
	public void update(Task task) {
		Task persist = taskRepository.findById(task.getId()).get();
		persist.setSubtitle(task.getSubtitle());
		persist.setModule(task.getModule());
		persist.setInstanceTitle(task.getInstanceTitle());
		persist.setInstanceId(task.getInstanceId());
		persist.setInstancePk(task.getInstancePk());
		persist.setTaskStatus(task.getTaskStatus());
		taskRepository.save(persist);
	}
	
	
	
	
	
	
//	public Page<Map<String,Object>> findUserTasks(User user, Module module, List<ViewProp> viewProps, 
//			List<ViewFilter> viewFilters, Pageable pageable) {
//		StringBuilder boolBuilder = new StringBuilder();
//		int i = 0;
//		for(; i < viewFilters.size(); i++) {
//			boolBuilder.append(i + 1).append(" and ");
//		}
//		viewFilters.add(ViewFilter.eq("sendUser.id", user.getId()));
//		viewFilters.add(ViewFilter.isNull("prevTaskId"));
//		viewFilters.add(ViewFilter.eq("receiveUser.id", user.getId()));
//		boolBuilder.append(" ((").append(++i).append(" and ").append(++i).append(") or ").append(++i).append(")");
//		
//		Sort sort = Sort.by("createdDate").descending();
//		String boolFilter = boolBuilder.toString();
//		Page<Map<String,Object>> dataPage = 
//			dataService.findPage(module, viewProps, viewFilters, sort, boolFilter, pageable);
//		return dataPage;
//	}
	
//	public List<Task> findTaskSteps(Long taskId) {
//		List<Task> all = new ArrayList<Task>();
//		Optional<Task> opt = taskRepository.findById(taskId);
//		if(opt.isPresent()) {
//			Task task = opt.get();
//			List<Task> prevList = this.findPrevTasks(taskId);
//			List<Task> nextList = this.findNextTasks(taskId);
//			all.addAll(prevList);
//			all.add(task);
//			all.addAll(nextList);
//		}
//		return all;
//	}
	
//	public List<Task> findPrevTasks(Long taskId) {
//		List<Task> prevList = new ArrayList<Task>();
//		Optional<Task> opt = taskRepository.findById(taskId);
//		if(opt.isPresent()) {
//			Task task = opt.get();
//			int loopSize = 0;
//			while(task.getPrevTaskId() != null && task.getPrevTaskId() != task.getId()) {
//				Optional<Task> prev = taskRepository.findById(task.getPrevTaskId());
//				if(prev.isPresent()) {
//					task = prev.get();
//					prevList.add(task);
//				}
//				else {
//					break;
//				}
//				if(++loopSize > 20) break;   // 防止数据错误造成死循环
//			}
//		}
//		Collections.reverse(prevList);
//		return prevList;
//	}
	
	
	
//	public void completeNextTasks(Long taskId) {
//		this.findNextTasks(taskId).forEach(task -> {
//			task.setTaskStatus(TaskStatus.COMPLETE);
//			task.setCompleteDate(new Date());
//			taskRepository.save(task);
//		});
//	}
	
}
