package org.needleframe.workflow.repository;

import java.util.List;

import org.needleframe.security.domain.User;
import org.needleframe.workflow.domain.Task;
import org.needleframe.workflow.domain.Task.TaskStatus;
import org.needleframe.workflow.domain.WorkNode;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface TaskRepository extends PagingAndSortingRepository<Task, Long> {
	
	@Query("from Task t where t.assigneeUser=?1 order by t.createdDate desc")
	public List<Task> findByAssigneeUser(User assigneeUser);
	
	@Query("from Task t where t.assigneeUser=?1 and t.taskStatus=?2 order by t.createdDate desc")
	public List<Task> findByAssigneeUserAndTaskStatus(User assigneeUser, TaskStatus taskStatus);
	
	public List<Task> findByWorkNodeAndModuleAndInstanceId(WorkNode workNode, String module, String instanceId);
	
}
