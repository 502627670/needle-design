package org.needleframe.workflow.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.needleframe.core.domain.AbstractEntity;
import org.needleframe.security.domain.User;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
@Entity
@Table(name="t_task")
@EntityListeners(AuditingEntityListener.class)
@Inheritance
public class Task extends AbstractEntity {
		
	/** 业务主题 */
	@Column(name="subtitle", length=120)
	private String subtitle;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="work_flow_id")
	private WorkFlow workFlow;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="work_node_id")
	private WorkNode workNode;
	
	/** 业务项 */
	@Column(name="module", length=80)
	private String module;
	
	/** 业务项实例 */
	@Column(name="instance_id", length=40)
	private String instanceId;
	
	/** 业务项的主键属性名 */
	@Column(name="instance_pk", length=30)
	private String instancePk;
	
	@Column(name="instance_title", length=120)
	private String instanceTitle;
	
	// 报告人
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="reporter_user_id")
	private User reporterUser;
	
	// 报告人
	@Column(name="reporter", length=120)
	private String reporter;
	
	// 接收人
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="assignee_user_id")
	private User assigneeUser;
	
	// 接收人
	@Column(name="assignee", length=120)
	private String assignee;
	
	// 分配时间
	@Column(name="assign_date")
	private Date assignDate;
	
	// 开始时间
	@Column(name="begin_date")
	private Date beginDate;
	
	// 完成时间
	@Column(name="complete_date")
	private Date completeDate;
	
	// 任务进度
	@Column(name="task_status")
	@Enumerated(EnumType.STRING)
	private TaskStatus taskStatus = TaskStatus.TODO;
	
	@Column(name="prev_task_id")
	private Long prevTaskId;
	
	@Column(name="next_task_id")
	private Long nextTaskId;
	
	public Task() {}
	
	public Task(Long id) {
		this.setId(id);
	}
	
	public static enum TaskStatus {
		TODO,        // 待处理
		PROCESS,     // 处理中
		COMPLETE,    // 已完成
		REOPEN,      // 已重开
		BACK;        // 已返回
	}
	
}
