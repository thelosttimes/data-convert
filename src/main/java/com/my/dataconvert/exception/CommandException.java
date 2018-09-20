package com.my.dataconvert.exception;
/**
 * @author 997620782@qq.com
 */
public class CommandException extends RuntimeException {


	private static final long serialVersionUID = -4474325453033343987L;

	public CommandException() {
	}

	public CommandException(String message) {
		super(message);
	}

	public CommandException(Throwable cause) {
		super(cause);
	}

	public CommandException(String message, Throwable cause) {
		super(message, cause);
	}
}
