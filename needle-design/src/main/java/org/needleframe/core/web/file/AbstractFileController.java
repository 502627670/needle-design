package org.needleframe.core.web.file;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.needleframe.core.service.file.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;

public abstract class AbstractFileController {
	
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Value("${file.upload.path:}")
	private String fileUploadPath;
	
	@Value("${file.http.server:}")
	private String fileHttpServer;
		
	@Autowired
	protected FileUploadService fileUploadService;
	
	protected abstract String getUploadRelativeDir();
	
	public String getFileHttpServer(HttpServletRequest request) {
		if(!StringUtils.hasText(this.fileHttpServer)) {
			String uri = request.getRequestURI();
			String url = request.getRequestURL().toString();
			this.fileHttpServer = url.replace(uri, request.getContextPath()) + "/";
		}
		return this.fileHttpServer;
	}
	
	public String getFileUploadPath(HttpServletRequest request) {
		if(!StringUtils.hasText(this.fileUploadPath)) {
			this.fileUploadPath = request.getSession().getServletContext().getRealPath("/");
			if(!this.fileUploadPath.endsWith(File.separator)) {
				this.fileUploadPath += File.separator;
			}
			new File(this.fileUploadPath).mkdirs();
		}
		return this.fileUploadPath;
	}
	
//	public List<FileResource> fileUpload(String module, MultipartHttpServletRequest request, HttpServletResponse response) {
//		String moduleName = StringUtils.hasText(module) ? module : "file";
//		logger.info("fileUpload(..) => 开始资源文件上传，未指定存储模块名，使用默认{}模块名", moduleName);
//		Map<String, MultipartFile> fileMap = request.getFileMap();
//		Collection<MultipartFile> multipartFiles = fileMap.values();
//		
//		String fileUploadPath = this.getFileUploadPath(request);
//		String fileHttpServer = this.getFileHttpServer(request);
//		String dir = getUploadRelativeDir(!fileUploadPath.endsWith(File.separator)); 
//		String currentUploadPath = fileUploadPath + dir + File.separator;
//		List<FileResource> fileResources = new ArrayList<FileResource>();
//		multipartFiles.stream().forEach(multipartFile -> {
//			FileResource fileResource = fileUploadService.upload(multipartFile, currentUploadPath, moduleName);
//			fileResource.setUri(dir + fileResource.getUri());
//			fileResource.setUrl(new StringBuilder(fileHttpServer).append(fileResource.getUri()).toString());
//			fileResources.add(fileResource);
//			logger.info("fileUpload(..) 上传模块({})文件({})的完整url为{} ", moduleName, multipartFile.getOriginalFilename(), fileResource.getUrl());
//		});
//		return fileResources;
//	}
	
//	public Object fileRemove(String uri, HttpServletRequest request, HttpServletResponse response) {
//		logger.info("fileRemove(..) => 开始删除资源{}文件", uri);
//		String fileUploadPath = this.getFileUploadPath(request);
//		String url = fileUploadPath + uri;
//		File file = new File(url);
//		if(file.exists()) {
//			logger.info("fileRemove(..) => 删除资源文件{}", file.getAbsolutePath());
//			file.delete();
//		}
//		return ResponseEntity.ok(Collections.emptyMap());
//	}
	
	
}
