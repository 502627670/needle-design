package org.needleframe.core.exception;

import org.springframework.validation.BindingResult;

public class InvalidRequestException extends RuntimeException {
	private static final long serialVersionUID = 3328632984030161995L;
	
	private BindingResult errors;

    public InvalidRequestException(BindingResult errors, String message) {
    	super(message);
        this.errors = errors;
    }

    public BindingResult getErrors() {
        return errors;
    }

}