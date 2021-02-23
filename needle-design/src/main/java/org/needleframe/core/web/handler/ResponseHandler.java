package org.needleframe.core.web.handler;

import org.needleframe.core.exception.ServiceException;
import org.needleframe.core.web.response.ResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ResponseHandler {
	
	@JsonIgnore
	private final static Logger logger = LoggerFactory.getLogger(ResponseHandler.class);
	
	public static ResponseMessage doResponse(RequestHandler executor) {
		try {
			Object data = executor.doRequest();
			
			if(data instanceof ResponseMessage) {
				return (ResponseMessage) data;
			}
			
			return ResponseMessage.success(data);
		}
		catch(ServiceException e) {
			logger.error("doResponse(..) => 执行页面请求失败：{}", e.getMessage());
			return ResponseMessage.failed(e.getMessage());
		}
		catch (Exception e) {
			logger.error("doResponse(..) => 执行页面请求失败：{}", e);
			return ResponseMessage.failed(e.getMessage());
		}
	}
	
}
