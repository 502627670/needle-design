package org.needleframe.core.service.file;

import java.io.File;
import java.io.IOException;

import org.needleframe.core.exception.ServiceException;
import org.needleframe.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileUploadService {
	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private String getFilename(String uploadPath, String originalFilename, String extName) {
		logger.info("getFilename(..) => uploadPath={}, originalFilename={}, extName={}", 
				uploadPath,originalFilename,extName);
		String basename = FileUtils.getBasename(originalFilename);
		uploadPath += uploadPath.endsWith(File.separator) ? "" : File.separator;
		String filePath = uploadPath + (basename + "." + extName);
		File file = new File(filePath);
		int size = 1;
		while(file.exists()) {
			String newFilename = basename + "(" + size + ")" + "." + extName;
			filePath = uploadPath + newFilename;
			file = new File(filePath);
			size++;
		}
		return file.getName();
	}
	
	public FileResource upload(MultipartFile multipartFile, String uri, String path) {
		logger.debug("upload(..) 上传资源文件({})到{}目录 ", multipartFile.getOriginalFilename(), path);
		String originalFilename = multipartFile.getOriginalFilename();
		String extName = FileUtils.getSuffix(originalFilename);
		String realFilename = getFilename(path, originalFilename, extName);
		uri += uri.endsWith("/") ? realFilename : "/" + realFilename;
		path += path.endsWith(File.separator) ? realFilename : File.separator + realFilename;
		File uploadFile = new File(path);
		try {
			multipartFile.transferTo(uploadFile);
		} 
		catch (IllegalStateException | IOException e) {
			logger.error("文件上传失败:{0}", e.getMessage(), e);
			throw new ServiceException("文件上传失败:{0}", e.getMessage());
		}
		
		FileResource fileResource = new FileResource();
		fileResource.setOriginalFilename(originalFilename);
		fileResource.setFilename(realFilename);
		fileResource.setMediaType(multipartFile.getContentType());
		fileResource.setExtName(extName);
		fileResource.setSize(multipartFile.getSize());
		fileResource.setPath(path);
		fileResource.setUri(uri);
		logger.info("upload(..) 上传资源文件({})的uri为{} ，保存路径为{}", multipartFile.getOriginalFilename(), uri, path);
		return fileResource;
	}
	
	
	public FileResource upload(byte[] data, String filename, String uri, String path) {
		logger.debug("upload(..) 上传文件数据到{}目录 ", path);
		String extName = FileUtils.getSuffix(filename);
		String realFilename = getFilename(path, filename, extName);
		uri += uri.endsWith("/") ? realFilename : "/" + realFilename;
		path += path.endsWith(File.separator) ? realFilename : File.separator + realFilename;
		File uploadFile = new File(path);
		try {
			org.apache.commons.io.FileUtils.writeByteArrayToFile(uploadFile, data);
		} 
		catch (IllegalStateException | IOException e) {
			logger.error("文件上传失败:{0}", e.getMessage(), e);
			throw new ServiceException("文件上传失败:{0}", e.getMessage());
		}
		
		FileResource fileResource = new FileResource();
		fileResource.setOriginalFilename(filename);
		fileResource.setFilename(realFilename);
		fileResource.setMediaType(extName);
		fileResource.setExtName(extName);
		fileResource.setSize(new Long(data.length));
		fileResource.setPath(path);
		fileResource.setUri(uri);
		logger.info("upload(..) 上传资源文件({})的uri为{} ，保存路径为{}", filename, uri, path);
		return fileResource;
	}
	
	
//	public FileResource upload(MultipartFile multipartFile, String fileUploadPath, String moduleName) {
//		logger.debug("upload(..) 上传资源({})文件({})到{}目录 ", moduleName, multipartFile.getOriginalFilename(), fileUploadPath);
//		LocalDate now = LocalDate.now();
//		StringBuilder uriBuilder = new StringBuilder("/").append(moduleName).append("/")
//			.append(now.getYear()).append("/").append(now.getMonthValue()).append("/").append(now.getDayOfMonth())
//			.append("/");
//		String uriToPath = fileUploadPath.endsWith(File.separator) ? 
//				uriBuilder.substring(1) : uriBuilder.toString();
//		StringBuilder dirBuilder = new StringBuilder(fileUploadPath)
//				.append(StringUtils.replaceAll(uriToPath, "/", File.separator));
//		new File(dirBuilder.toString()).mkdirs();
//		String originalFilename = multipartFile.getOriginalFilename();
//		String extName = FileUtils.getSuffix(originalFilename, "");
//		String filename = getFilename(dirBuilder.toString(), originalFilename, extName);
//		uriBuilder.append(filename);
//		dirBuilder.append(filename);
//		String uri = uriBuilder.toString();
//		String path = dirBuilder.toString();
//		File uploadFile = new File(path);
//		try {
//			multipartFile.transferTo(uploadFile);
//		} 
//		catch (IllegalStateException | IOException e) {
//			logger.error("文件上传失败:{0}", e.getMessage(), e);
//			throw new ServiceException("文件上传失败:{0}", e.getMessage());
//		}
//		
//		FileResource fileResource = new FileResource();
//		fileResource.setOriginalFilename(originalFilename);
//		fileResource.setFilename(filename);
//		fileResource.setMediaType(multipartFile.getContentType());
//		fileResource.setExtName(extName);
//		fileResource.setSize(multipartFile.getSize());
//		fileResource.setPath(path);
//		fileResource.setUri(uri);
//		logger.info("upload(..) 上传模块({})文件({})的uri为{} ，完整上传路径为{}", moduleName, multipartFile.getOriginalFilename(), uri, path);
//		return fileResource;
//	}
}
