package org.needleframe.core.exception;

public class ApiException extends RuntimeException {
	private static final long serialVersionUID = 754121720095617577L;

	private String code;

	private String message;

	public ApiException(String code, String message, Throwable e) {
		super(message + "(" + code + ")", e);
		this.code = code;
		this.message = message;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
