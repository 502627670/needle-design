package org.needleframe.workflow.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.needleframe.core.domain.AbstractEntity;
import org.needleframe.security.domain.User;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作节点
 * @author admin
 */
@Data
@EqualsAndHashCode(callSuper=true)
@Entity
@Table(name="t_work_node")
@EntityListeners(AuditingEntityListener.class)
public class WorkNode extends AbstractEntity {
	
	// 所属工作流
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="work_flow_id")
	private WorkFlow workFlow;
	
	// 工作项名称
	@Column(name="name", length=120)
	private String name;
	
	// 流程业务项的属性
	@Column(name="prop", length=60)
	private String prop;
	
	// 流程业务项的属性名
	@Column(name="prop_name", length=120)
	private String propName;
	
	// 业务项属性值
	@Column(name="from_value", length=120)
	private String fromValue;
	
	// 下一步属性值 
	@Column(name="next_value", length=120)
	private String nextValue;
	
	// 接收用户
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="assignee_user_id")
	private User assigneeUser;
	
	// 接收用户的名称
	@Column(name="assignee_name", length=120)
	private String assigneeName;
	
	// 序列
	@Column(name="sort_order")
	private Integer sortOrder;
	
}
