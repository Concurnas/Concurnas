package com.concurnas.bootstrap.runtime;

public interface CopyDefinition {
	public CopyDefinition getSuperCopier();
	public Object getOverride(String field, Object ifNone);
	public boolean shouldCopyField(String field);
	public CopyDefinition getFieldCopier(String field);
	public boolean incDefaults();
}
