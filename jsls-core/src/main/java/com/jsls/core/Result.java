package com.jsls.core;

import java.util.ArrayList;
import java.util.List;

public class Result<D> {
	public static final String CODE_SUCCESS = "0000";
	public static final String CODE_FAIL = "0001";
	public static final Result<Void> SUCCESS = new UnModifiableResult<>();
	private String code;
	private String message;
	private D data;

	public Result() {
		this(CODE_SUCCESS, "SUCCESS");
	}

	public Result(String code, String message) {
		this.code = code;
		this.message = message;
	}

	public Result(D data) {
		this();
		this.data = data;
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

	public D getData() {
		return data;
	}

	public void setData(D data) {
		this.data = data;
	}

	public <T> Result<T> copy() {
		return new Result<T>(code, message);
	}

	public boolean isSuccess() {
		return CODE_SUCCESS.equals(code);
	}

	public static <D> Result<D> fail(String code, String message) {
		return new Result<D>(CODE_FAIL, message);
	}

	public static <D> Result<D> fail(String message) {
		return new Result<D>(CODE_FAIL, message);
	}

	public static <D> Result<D> success(D data) {
		return new Result<D>(data);
	}

	public static class UnModifiableResult<D> extends Result<D> {
		public UnModifiableResult() {
			super();
		}

		public UnModifiableResult(String code, String message) {
			super(code, message);
		}

		public UnModifiableResult(D data) {
			super(data);
		}

		@Override
		public void setCode(String code) {
			throw new UnsupportedOperationException("不可修改");
		}

		@Override
		public void setMessage(String message) {
			throw new UnsupportedOperationException("不可修改");
		}

		@Override
		public void setData(D data) {
			throw new UnsupportedOperationException("不可修改");
		}
	}

	public static <T> Result<Void> merge(List<Result<T>> retList) {
		return merge(retList, false);
	}

	public static <T> Result<Void> merge(List<Result<T>> retList, boolean negative) {
		long success = 0;
		long failure = 0;
		long index = 0;
		List<String> messages = new ArrayList<>();
		for (Result<T> ret : retList) {
			index++;
			if (ret.isSuccess()) {
				success++;
			} else {
				if (negative) {
					return ret.copy();
				}
				failure++;
				messages.add("第" + index + "个" + ret.getMessage());
			}
		}
		String message = "成功：" + success + ", 失败：" + failure;
		if (!messages.isEmpty()) {
			message += ";" + messages;
		}
		return new Result<Void>(CODE_SUCCESS, message);
	}

}