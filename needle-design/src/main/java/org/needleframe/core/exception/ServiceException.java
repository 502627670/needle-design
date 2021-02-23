package org.needleframe.core.exception;

import java.text.MessageFormat;

import org.needleframe.core.MessageCode;

public class ServiceException extends RuntimeException {
	private static final long serialVersionUID = 3149885123244982860L;
	
	private int code;
	
	private String message;
	
	private Throwable cause;
	
	public static ServiceException error(String message, Object... arguments) {
		ServiceException exception = new ServiceException(message, arguments);
		return exception;
	}
	
	public ServiceException() {
		this.cause = null;
	}
	
	public ServiceException(String message) {
		this.code = MessageCode.FAILED.getCode();
		this.message = message;
	}
	
	public ServiceException(String message, Object... arguments) {
		this.message = MessageFormat.format(message, arguments);
		this.cause = null;
	}
		
	public ServiceException(Throwable throwable) {
		this.cause = throwable.getCause() == null ? throwable : throwable.getCause();
		this.code = MessageCode.FAILED.getCode();
		
		this.message = throwable.getMessage();
		Throwable cause = throwable.getCause();
		if(cause != null) {
			this.message = cause.getMessage();
		}
	}
	
	public ServiceException(Throwable throwable, int code, String message) {
		this(throwable, code, message, new Object[0]);
	}
	
	public ServiceException(Throwable throwable, int code, String message, Object... arguments) {
		this.cause = throwable.getCause() == null ? throwable : throwable.getCause();
		this.code = code;
		this.message = MessageFormat.format(message, arguments);
	}
	
	public ServiceException(MessageCode messageCode) {
		this.code = messageCode.getCode();
		this.message = messageCode.name();
		this.cause = null;
	}
	
	public ServiceException(MessageCode messageCode, String message) {
		this(messageCode, message, new Object[0]);
	}
	
	public ServiceException(MessageCode messageCode, String message, Object... arguments) {
		this.code = messageCode.getCode();
		this.message = MessageFormat.format(message, arguments);
		this.cause = null;
	}
	
	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Throwable getCause() {
		return cause;
	}

	public void setCause(Throwable cause) {
		this.cause = cause;
	}
	
}
