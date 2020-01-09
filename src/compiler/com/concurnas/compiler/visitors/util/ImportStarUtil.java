package com.concurnas.compiler.visitors.util;

import com.concurnas.compiler.ast.ClassDefJava;

public class ImportStarUtil {
	
	public static abstract class PackageOrClass{
		public abstract String getResource(String shortname);
		public abstract int hashCode();//so it can be stored in a hashmap
		public abstract boolean equals(Object an);//so it can be stored in a hashmap
	}
	
	public static class PackageIS extends PackageOrClass{
		private String path;
		
		public PackageIS(String path) {
			this.path = path;
		}

		@Override
		public String getResource(String shortname) {
			String fullClassName = this.path + "." + shortname;
			try {
				Class.forName(fullClassName);
			}catch(ClassNotFoundException cnf){
				return null;
			}
			
			return fullClassName;
		}

		@Override
		public int hashCode() {
			return path.hashCode();
		}

		@Override
		public boolean equals(Object an) {
			if(an instanceof PackageIS) {
				PackageIS pis = (PackageIS)an;
				return pis.path.equals(this.path);
			}
			return false;
		}
		
		
	}
	
	public static class ClassIS extends PackageOrClass{
		private ClassDefJava cls;
		
		public ClassIS(Class<?> cls) {
			this.cls = new ClassDefJava(cls);
		}

		@Override
		public String getResource(String shortname) {
			if(null != cls.getVariable(null, shortname)) {
				return cls.getPrettyName() + shortname;
			}
			
			return null;
		}

		@Override
		public int hashCode() {
			return cls.hashCode();
		}

		@Override
		public boolean equals(Object an) {
			if(an instanceof ClassIS) {
				ClassIS cis = (ClassIS)an;
				return cis.cls.equals(this.cls);
			}
			return false;
		}
	}
	
	
}
