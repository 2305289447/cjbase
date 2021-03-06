package com.xcz.afcs.api.result;

import com.xcz.afcs.validate.annotation.ApiField;

import java.util.Collection;

public class ListResult<T> extends BaseResult {

	public ListResult(int retCode, String retMsg) {
		super(retCode, retMsg);
	}

	public ListResult() {
	}

	@ApiField(name="列表数据对象")
	private Collection<T> data;


	public Collection<T> getData() {
		return data;
	}

	public void setData(Collection<T> data) {
		this.data = data;
	}

}
