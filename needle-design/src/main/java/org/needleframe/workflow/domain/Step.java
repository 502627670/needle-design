package org.needleframe.workflow.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.needleframe.core.domain.AbstractEntity;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务执行的步骤
 * @author admin
 */
@Data
@EqualsAndHashCode(callSuper=true)
@Entity
@Table(name="t_step")
@EntityListeners(AuditingEntityListener.class)
public class Step extends AbstractEntity {
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="task_id")
	private Task task;
	
	// 提交人
	@Column(name="submitter", length=120)
	private String submitter;
	
	// 提交日期
	@Column(name="submit_date")
	private Date submitDate;
	
	// 提交内容
	@Column(name="content", length=200)
	private String content;
	
	@Column(name="step_status", length=30)
	@Enumerated(EnumType.STRING)
	private StepStatus stepStatus;
	
	/** 业务项 */
	@Column(name="module", length=80)
	private String module;
	
	/** 业务项实例 */
	@Column(name="instance_id", length=40)
	private String instanceId;
	
	/** 业务项的主键属性名 */
	@Column(name="instance_pk", length=30)
	private String instancePk;
	
	// 关联数据
	@Column(name="instance_title", length=120)
	private String instanceTitle;
	
	public static enum StepStatus {
		BACK,         // 已退回
		PROCESS,      // 处理中
		COMPLETE,     // 已完成
	}
	
}
