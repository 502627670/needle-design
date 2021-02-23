package org.needleframe.workflow.repository;

import java.util.List;

import org.needleframe.security.domain.User;
import org.needleframe.workflow.domain.Message;
import org.needleframe.workflow.domain.Message.MessageStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface MessageRepository extends PagingAndSortingRepository<Message, Long> {
	
	@Query("from Message m where m.assigneeUser=?1 order by m.assignDate desc")
	public List<Message> findByAssigneeUser(User assigneeUser);
	
	@Query("from Message m where m.assigneeUser=?1 and m.messageStatus=?2 order by m.assignDate desc")
	public List<Message> findByAssigneeUserAndMessageStatus(User assigneeUser, MessageStatus messageStatus);
	
}
