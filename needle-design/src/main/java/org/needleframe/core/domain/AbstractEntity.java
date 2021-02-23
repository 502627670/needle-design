package org.needleframe.core.domain;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import lombok.Data;

@Data
@MappedSuperclass
public abstract class AbstractEntity {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@CreatedBy
	@Column(name="created_by", length=30)
	private String createdBy;
	
	@CreatedDate
	@Column(name="created_date")
	private Date createdDate;
	
	@LastModifiedBy
	@Column(name="last_modified_by", length=30)
	private String lastModifiedBy;
	
	@LastModifiedDate
	@Column(name="last_modified_date")
	private Date lastModifiedDate;
	
}
