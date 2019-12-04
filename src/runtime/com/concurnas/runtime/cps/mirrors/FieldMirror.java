package com.concurnas.runtime.cps.mirrors;

public class FieldMirror {
	public String name;
	public String desc;
	public boolean isShared;

	public FieldMirror(String name, String desc, boolean isShared) {
		this.name = name;
		this.desc = desc;
		this.isShared = isShared;
	}
}
