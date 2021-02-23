package org.needleframe.security.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="t_group", indexes={@Index(name="group_path_index", columnList="path")})
@EntityListeners(AuditingEntityListener.class)
public class Group {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@Column(name="name", length=120)
	private String name;
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="parent_id")
	private Group parent;
	
	/** 格式: id-id-id-，比如1-2-3- */
	@Column(name="path", length=120)
	private String path;
	
	@Column(name="offspring_number")
	private Integer offspringNumber;
	
	@Column(name="description", length=200)
	private String description;
	
}
