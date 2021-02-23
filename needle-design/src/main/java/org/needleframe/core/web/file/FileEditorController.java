package org.needleframe.core.web.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import org.needleframe.core.service.file.FileResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import lombok.Getter;
import lombok.Setter;

@Controller(("fileEditorController"))
public class FileEditorController extends AbstractFileController {
	
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public final static String HTML_TEMPLATE = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>{{body}}</body></html>";
	
	private final static String DEFAULT_MODULE = "cms";
	
	@Override
	protected String getUploadRelativeDir() {
		return DEFAULT_MODULE;
	}
	
	
	@Getter
	@Setter
	private class Response {
		Files files;
		public Response files(Files files) {
			this.files = files;
			return this;
		}
	}
	
	@Getter
	private class Files {
		String file;
		private Files(String file) {
			this.file = file;
		}
	}
	
	
	@RequestMapping("/editor/image.json")
	@ResponseBody 
	public Response editorImage(@RequestParam(name="module", required=false) String module, 
			MultipartHttpServletRequest request, HttpServletResponse response) throws Exception {
		String moduleName = StringUtils.hasText(module) ? module.replaceAll(":", "") : DEFAULT_MODULE;
		logger.info("editorUpload(..) => 开始资源文件上传，使用模块名{}", moduleName);
		Map<String, MultipartFile> fileMap = request.getFileMap();
		Collection<MultipartFile> multipartFiles = fileMap.values();
		
		Response fileResponse = new Response();
		for(Iterator<MultipartFile> iterator = multipartFiles.iterator(); iterator.hasNext(); ) {
			MultipartFile multipartFile = iterator.next();
			String contentType = multipartFile.getContentType();
			byte[] data = multipartFile.getBytes();
			String base64str = DatatypeConverter.printBase64Binary(data);
			String dataString = new StringBuilder("data:").append(contentType)
	        	.append(";base64,").append(base64str).toString();
	        
			logger.info("editorImage(..) 上传编辑器({})图片({})完成 ", moduleName, multipartFile.getOriginalFilename());
			return fileResponse.files(new Files(dataString));
		}
		return fileResponse;
	}
	
	@RequestMapping("/editor/upload.json")
	@ResponseBody 
	public FileResource editorUpload(@RequestParam(name="module", required=false) String module, 
			@RequestParam(name="content", required=false) String content,
			HttpServletRequest request, HttpServletResponse response) {
		module = StringUtils.hasText(module) ? module : DEFAULT_MODULE;
		logger.info("editorUpload(..) => 开始资源文件上传，未指定存储模块名，使用默认{}模块名", module);
		String fileUploadPath = this.getFileUploadPath(request);
		String fileHttpServer = this.getFileHttpServer(request);
		String dir = getUploadRelativeDir();
		if(!dir.startsWith("/")) {
			dir = "/" + dir;
		}
		LocalDate now = LocalDate.now();
		StringBuilder uriBuilder = new StringBuilder(dir).append("/").append(module).append("/")
			.append(now.getYear()).append("/").append(now.getMonthValue()).append("/")
			.append(now.getDayOfMonth()).append("/");
		String uriToPath = fileUploadPath.endsWith(File.separator) ? 
				uriBuilder.substring(1) : uriBuilder.toString();
		StringBuilder pathBuilder = new StringBuilder(fileUploadPath)
				.append(StringUtils.replace(uriToPath, "/", File.separator));
		new File(pathBuilder.toString()).mkdirs();
		String filename = new StringBuilder(module).append("_")
				.append(System.currentTimeMillis()).append(".html").toString();
		uriBuilder.append(filename);
		pathBuilder.append(filename);
		File uploadFile = new File(pathBuilder.toString());
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(uploadFile), "UTF-8"))) {
			writer.write(HTML_TEMPLATE.replace("{{body}}", content));
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		FileResource fileResource = new FileResource();
		fileResource.setOriginalFilename(filename);
		fileResource.setFilename(filename);
		fileResource.setMediaType(MediaType.TEXT_HTML_VALUE);
		fileResource.setExtName("html");
		fileResource.setSize(uploadFile.getTotalSpace());
		fileResource.setPath(pathBuilder.toString());
		fileResource.setUri(uriBuilder.toString());
		
		StringBuilder urlBuilder = new StringBuilder(fileHttpServer);
		if(fileHttpServer.endsWith("/") && fileResource.getUri().startsWith("/")) {
			urlBuilder.append(fileResource.getUri().substring(1));
		}
		fileResource.setUrl(urlBuilder.toString());
		logger.info("editorUpload(..) 上传模块({})文件({})的url为{} ", module, filename, fileResource.getUrl());
		return fileResource;
	}
	
	@RequestMapping("/editor/remove.json")
	@ResponseBody 
	public Object editorRemove(@RequestParam("uri") String uri, HttpServletRequest request, HttpServletResponse response) {
		logger.info("fileRemove(..) => 开始删除资源{}文件", uri);
		String fileUploadPath = this.getFileUploadPath(request);
		String url = fileUploadPath + uri;
		File file = new File(url);
		if(file.exists()) {
			logger.info("fileRemove(..) => 删除资源文件{}", file.getAbsolutePath());
			file.delete();
		}
		return ResponseEntity.ok(Collections.emptyMap());
	}
	
}
