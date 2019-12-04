package com.concurnas.lang.precompiled;

import com.concurnas.bootstrap.runtime.cps.CObject;

public class ClassNeedingDeleteOnUnused {
	public static int delCount = 0;
	
	public static final class ClassHavingDelete extends CObject{
		@Override
		public void delete() {
			delCount++;
		}
	}
	
	public static class Provider{
		@com.concurnas.lang.DeleteOnUnusedReturn
		public ClassHavingDelete getClassHavingDelete() {
			return new ClassHavingDelete();
		}
	}
	
	public static class ChildProvider extends Provider{
		public ClassHavingDelete getClassHavingDelete() {
			return super.getClassHavingDelete();
		}
	}
	
	/*@com.concurnas.lang.DeleteOnUnusedReturn
	public static class ClassHavingDeleteHasSubs extends CObject{
		
	}*/
	
	/*public static class ClassHavingDeleteSubclass extends ClassHavingDeleteHasSubs{
		@Override
		public void delete() {
			delCount++;
		}
	}*/
}
