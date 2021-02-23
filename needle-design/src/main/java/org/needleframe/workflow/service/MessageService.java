package org.needleframe.workflow.service;

import java.util.Date;

import org.needleframe.workflow.domain.Message;
import org.needleframe.workflow.domain.Message.MessageStatus;
import org.needleframe.workflow.domain.Task;
import org.needleframe.workflow.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MessageService {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private MessageRepository messageRepository;
	
	public Message sendMessage(Task task) {
		Message message = new Message();
		message.setAssignee(task.getAssignee());
		message.setAssigneeUser(task.getAssigneeUser());
		message.setAssignDate(new Date());
		message.setMessageStatus(MessageStatus.UNREAD);
		message.setTitle("消息：" + task.getSubtitle());
		message.setLink(task.getSubtitle());
		messageRepository.save(message);
		
		logger.debug("sendMessage(..) => 发送消息到用户{}", message.getAssignee());
		return message;
	}
	
	public void updateMessageStatus(Long messageId, MessageStatus messageStatus) {
		Message message = messageRepository.findById(messageId).get();
		message.setMessageStatus(messageStatus);
		messageRepository.save(message);
	}
	
}
