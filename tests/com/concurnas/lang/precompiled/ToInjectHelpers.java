package com.concurnas.lang.precompiled;

import com.concurnas.lang.Inject;

public class ToInjectHelpers {

	public static class MessageProvider {
		private String msg;

		public MessageProvider(String msg) {
			this.msg = msg;
		}
		
		public String provide(String sticky) {
			return msg + " " + sticky;
		}
	}
	
	public static class StringHolder {
		private String msg;

		public StringHolder(String msg) {
			this.msg = msg;
		}
		
		public String getHeld() {
			return msg;
		}
	}
	
	public static class Doer{
		public String doit(MessageProvider mp, StringHolder sh) {
			return mp.provide(sh.msg);
		}
	}
	
	public static class Client{

		private Doer doer;
		
		@Inject public StringHolder sh;

		private MessageProvider mp;
		
		@Inject
		public Client(Doer doer) {
			this.doer = doer;
		}
		
		@Inject
		public void setMessageProvider(MessageProvider mp) {
			this.mp = mp;
		}
		
		public String runOperation() {
			return this.doer.doit(mp, sh);
		}
	}
	
	public static class Illegal{
		@Inject
		private Illegal(String a) {
		}
	}
	
	public static class ItsFine{
		@Inject
		public ItsFine(String a) {
		}
	}
}
