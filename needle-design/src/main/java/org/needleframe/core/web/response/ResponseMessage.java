package org.needleframe.core.web.response;

import org.needleframe.core.MessageCode;

import lombok.Getter;

@Getter
public class ResponseMessage {
	
	public final static String SUCCESS = "SUCCESS";
	public final static String FAILED = "FAILED";
	
    private String status = SUCCESS;
    private String message = "SUCCESS";
    private int code;
    
    private Object data;
    
    
    
    public ResponseMessage() {
    	this(SUCCESS, "");
    }
    
    public ResponseMessage(String status, String message) {
        this(status, MessageCode.OK.getCode(), message);
    }

    public ResponseMessage(String status, int code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
    
    public ResponseMessage data(Object data) {
    	this.data = data;
    	return this;
    }
    
    public boolean isSuccess() {
    	return SUCCESS.equals(this.status);
    }
    
	public static ResponseMessage success(Object data) {
		ResponseMessage responseMessage = new ResponseMessage(SUCCESS, null);
		responseMessage.data(data);
		return responseMessage;
    }
	
	public static ResponseMessage success(String message) {
        return new ResponseMessage(SUCCESS, message);
    }

	public static ResponseMessage failed(String message) {
		return failed(MessageCode.FAILED.getCode(), message);
	}
	
	public static ResponseMessage failed(MessageCode messageCode, String message) {
        return new ResponseMessage(FAILED, messageCode.getCode(), message);
    }
	
    public static ResponseMessage failed(int code, String message) {
        return new ResponseMessage(FAILED, code, message);
    }
}
