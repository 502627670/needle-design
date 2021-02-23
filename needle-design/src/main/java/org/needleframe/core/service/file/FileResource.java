package org.needleframe.core.service.file;

import java.io.Serializable;

import javax.persistence.Transient;

import lombok.Data;

@Data
public class FileResource implements Serializable {
	private static final long serialVersionUID = 7446399656954498678L;
	
	private Long id;
	
	private String originalFilename;
	
	private String filename;
	
	private String mediaType;
	
	private String uri;
	
	private String path;
	
	private String extName;
	
	private Long size = 0L;
	
	private Integer width = 0;
	
	private Integer height = 0;
	
	@Transient
	private String url;
	
	public FileResource() {}
	
	public FileResource(Long id) {
		this.id = id;
	}
	
}
