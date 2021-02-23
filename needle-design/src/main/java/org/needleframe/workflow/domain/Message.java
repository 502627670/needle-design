package org.needleframe.workflow.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.needleframe.security.domain.User;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Data;

/**
 * 消息
 * @author admin
 */
@Data
@Entity
@Table(name="t_message")
@EntityListeners(AuditingEntityListener.class)
public class Message {
	
	public static enum MessageStatus {
		UNREAD,   // 未读
		READED;   // 已读
	}
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	// 标题
	@Column(name="title", length=120)
	private String title;
	
	// 链接
	@Column(name="link", length=120)
	private String link;
		
	// 创建时间
	@Column(name="assign_date")
	private Date assignDate;
	
	// 状态
	@Column(name="message_status", length=30)
	@Enumerated(EnumType.STRING)
	private MessageStatus messageStatus = MessageStatus.UNREAD;
	
	// 接收人
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="assignee_user_id")
	private User assigneeUser;
	
	// 接收人
	@Column(name="assignee", length=60)
	private String assignee;
	
}
