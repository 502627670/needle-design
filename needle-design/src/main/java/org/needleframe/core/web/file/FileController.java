package org.needleframe.core.web.file;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.needleframe.core.service.file.FileResource;
import org.needleframe.core.web.response.ResponseMessage;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@Controller(("fileController"))
public class FileController extends AbstractFileController {
	
	@Override
	protected String getUploadRelativeDir() {
		return "upload";
	}
	
	public FileResource upload(String moduleName, String filename, byte[] data, HttpServletRequest request, HttpServletResponse response) {
		logger.info("upload(..) => 开始资源文件上传，使用模块名{}", moduleName);
		String fileHttpServer = this.getFileHttpServer(request);
		String fileUploadPath = this.getFileUploadPath(request);
		String dir = getUploadRelativeDir();
		if(!dir.startsWith("/")) {
			dir = "/" + dir;
		}
		LocalDate now = LocalDate.now();
		StringBuilder uriBuilder = new StringBuilder(dir).append("/").append(moduleName)
			.append("/").append(now.getYear()).append("/").append(now.getMonthValue())
			.append("/").append(now.getDayOfMonth()).append("/");
		String uriToPath = fileUploadPath.endsWith(File.separator) ? 
				uriBuilder.substring(1) : uriBuilder.toString();
		StringBuilder dirBuilder = new StringBuilder(fileUploadPath)
				.append(StringUtils.replace(uriToPath, "/", File.separator));
		new File(dirBuilder.toString()).mkdirs();
		

		FileResource fileResource = fileUploadService.upload(data, filename, uriBuilder.toString(), dirBuilder.toString());
		StringBuilder urlBuilder = new StringBuilder(fileHttpServer);
		if(fileHttpServer.endsWith("/") && fileResource.getUri().startsWith("/")) {
			urlBuilder.append(fileResource.getUri().substring(1));
		}
		fileResource.setUrl(urlBuilder.toString());
		logger.info("upload(..) 上传模块({})文件({})的完整url为{} ", moduleName, filename, fileResource.getUrl());
		return fileResource;
	}
	
	@RequestMapping("/file/upload.json")
	@ResponseBody 
	public List<FileResource> fileUpload(@RequestParam(name="module", required=false) String module, 
			MultipartHttpServletRequest request, HttpServletResponse response) {
		String moduleName = StringUtils.hasText(module) ? module.replaceAll(":", "") : "file";
		logger.info("fileUpload(..) => 开始资源文件上传，使用模块名{}", moduleName);
		Collection<MultipartFile> multipartFiles = request.getFileMap().values();
		
		String fileHttpServer = this.getFileHttpServer(request);
		
		String fileUploadPath = this.getFileUploadPath(request);
		String dir = getUploadRelativeDir();
		if(!dir.startsWith("/")) {
			dir = "/" + dir;
		}
		LocalDate now = LocalDate.now();
		StringBuilder uriBuilder = new StringBuilder(dir).append("/").append(moduleName)
			.append("/").append(now.getYear()).append("/").append(now.getMonthValue())
			.append("/").append(now.getDayOfMonth()).append("/");
		String uriToPath = fileUploadPath.endsWith(File.separator) ? 
				uriBuilder.substring(1) : uriBuilder.toString();
		StringBuilder dirBuilder = new StringBuilder(fileUploadPath)
				.append(StringUtils.replace(uriToPath, "/", File.separator));
		new File(dirBuilder.toString()).mkdirs();
		
		List<FileResource> fileResources = new ArrayList<FileResource>();
		multipartFiles.stream().forEach(multipartFile -> {
			FileResource fileResource = fileUploadService.upload(multipartFile, uriBuilder.toString(), dirBuilder.toString());
			StringBuilder urlBuilder = new StringBuilder(fileHttpServer);
			if(fileHttpServer.endsWith("/") && fileResource.getUri().startsWith("/")) {
				urlBuilder.append(fileResource.getUri().substring(1));
			}
			fileResource.setUrl(urlBuilder.toString());
			fileResources.add(fileResource);
			logger.info("fileUpload(..) 上传模块({})文件({})的完整url为{} ", moduleName, multipartFile.getOriginalFilename(), fileResource.getUrl());
		});
		return fileResources;
	}
	
	@RequestMapping("/file/remove.json")
	@ResponseBody 
	public Object fileRemove(@RequestParam("uri") String uri, HttpServletRequest request, HttpServletResponse response) {
		logger.info("fileRemove(..) => 开始删除资源{}文件", uri);
		String fileUploadPath = this.getFileUploadPath(request);
		String url = fileUploadPath + uri;
		File file = new File(url);
		if(file.exists()) {
			logger.info("fileRemove(..) => 删除资源文件{}", file.getAbsolutePath());
			file.delete();
		}
		return ResponseMessage.success("ok");
	}
}
