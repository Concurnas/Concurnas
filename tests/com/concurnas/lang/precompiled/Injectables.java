package com.concurnas.lang.precompiled;

import com.concurnas.lang.Inject;
import com.concurnas.lang.Named;
import com.concurnas.lang.ParamName;

public class Injectables {

	public static class UnNamedFields{
		
		@Inject
		public UnNamedFields() {
			
		}
		@Inject public String an2;
		@Inject public String an3;
		
		public String toString() {
			return String.format("%s %s", an2, an3);
		}
		
	}
	
	public static class NamedFields{
		
		@Inject
		public NamedFields() {
			
		}
		@Inject
		@Named("Second Field")
		public String an2;
		
		@Inject
		@Named("Third Field") 
		public String an3;
		
		public String toString() {
			return String.format("%s %s", an2, an3);
		}
	}
	
	public static class UnNamedMethod{
		
		@Inject
		public UnNamedMethod() {
			
		}

		@Inject private String an2;
		@Inject private String an3;
		
		@Inject
		public void setter(String an2, String an3) {
			this.an2 = an2;
			this.an3 = an3;
		}
		
		public String toString() {
			return String.format("%s %s", an2, an3);
		}
	}
	
	public static class UnNamedMethodPName{
		
		@Inject
		public UnNamedMethodPName() {
			
		}

		@Inject private String an2;
		@Inject private String an3;
		
		@Inject
		public void setter(@ParamName(name = "an2") String an2, @ParamName(name = "an3") String an3) {
			this.an2 = an2;
			this.an3 = an3;
		}
		
		public String toString() {
			return String.format("%s %s", an2, an3);
		}
	}
	
	public static class UnNamedMethodPNameFOK{
		
		@Inject
		public UnNamedMethodPNameFOK() {
			
		}

		private String an2;
		private String an3;
		
		@Inject
		public void setter(@ParamName(name = "an2") String an2, @ParamName(name = "an3") String an3) {
			this.an2 = an2;
			this.an3 = an3;
		}
		
		public String toString() {
			return String.format("%s %s", an2, an3);
		}
	}
	
	public static class NamedMethodParams{
		
		@Inject
		public NamedMethodParams() {
			
		}
		
		private String an2;
		private String an3;
		
		@Inject
		public void setter(@Named("an2") String an2, String an3) {
			this.an2 = an2;
			this.an3 = an3;
		}
		
		public String toString() {
			return String.format("%s %s", an2, an3);
		}
	}
	
	public static class NamedWPrivateMethod{
		
		@Inject
		public NamedWPrivateMethod() {
			
		}

		private String an2;
		private String an3;
		
		@Inject
		protected void setter(@Named("an2") String an2, String an3) {
			this.an2 = an2;
			this.an3 = an3;
		}
		
		public String toString() {
			return String.format("%s %s", an2, an3);
		}
	}
	

	public static class NamedWConstrcutorArgs{
		@Inject
		public NamedWConstrcutorArgs(@ParamName(name="an2") String an2, String an3) {
			this.an2 = an2;
			this.an3 = an3;
		}
		
		private String an2;
		private String an3;
		
		public String toString() {
			return String.format("%s %s", an2, an3);
		}
	}
	
	public static class NamedWConstrcutorNamed{
		@Inject
		public NamedWConstrcutorNamed(@Named("first arg") String an2, String an3) {
			this.an2 = an2;
			this.an3 = an3;
		}

		private String an2;
		private String an3;
		
		public String toString() {
			return String.format("%s %s", an2, an3);
		}
	}
	
	public static class NamedWConstrcutorNamedIllegal{
		@Inject
		private NamedWConstrcutorNamedIllegal(@Named("first arg") String an2, String an3) {
			this.an2 = an2;
			this.an3 = an3;
		}

		private String an2;
		private String an3;
		
		public String toString() {
			return String.format("%s %s", an2, an3);
		}
	}
	
}
