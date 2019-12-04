package com.concurnas.runtimeCache;

class ProgressTracker {
	private int completed = 0;
	private int totalToComplete;
	private long startTime;
	private int maxLen = 0;
	private int[] icons;
	
	public ProgressTracker(int totalToComplete) {
		this.totalToComplete = totalToComplete;
		this.icons = new int[this.totalToComplete];
		startTime = System.currentTimeMillis();
	}
	
	private String fmtDuration() {
		long now = System.currentTimeMillis();
		long dur = now - startTime;
		if(dur < 1000) {//milliseconds
			return String.format("%3dms",dur);
		}
		dur /= 1000;
		if(dur < 60) {
			return String.format("%4ds",dur);
		}
		
		dur /= 60;
		if(dur < 60) {
			return String.format("%4dm",dur);
		}	
		
		return "ERR";//more than an hour!
	}


	public synchronized void onDone(int idx) {
		completed++;
		this.icons[idx] = -1;
		if(totalToComplete == completed) {
			log(-1, "Completed!");
			System.out.println();
		}else {
			log(-1, null);
		}
	}
	
	private static final String anim= "-\\|/";
	
	public synchronized void log(int idx, String append) {
		//<--------------------> 30% | [time] | append
		
		if(idx > -1) {
			this.icons[idx] = (this.icons[idx]+1) % anim.length();
		}
		
		StringBuilder sb = new StringBuilder("\r <");
		//sb.append(new String(new char[completed]).replace('\0', '*'));
		//sb.append(new String(new char[totalToComplete-completed]).replace('\0', '-'));
		
		for(int n=0; n < this.icons.length; n++) {
			int what = this.icons[n];
			sb.append(what == -1? '*' : anim.charAt(what));
		}
		
		sb.append("> ");
		
		sb.append(String.format("%3.0f%%", ((double)completed)/totalToComplete * 100));
		
		sb.append(" | [");
		sb.append(fmtDuration());
		sb.append("]");
		
		if(append != null) {
			sb.append(" | ");
			sb.append(append);
		}
		String toOutput = sb.toString();
		int thisLen = toOutput.length();
		if(thisLen < maxLen) {
			toOutput += new String(new char[maxLen-thisLen]).replace('\0', ' ');
		}else {
			maxLen = thisLen;
		}
		
		System.out.print(toOutput);
	}
}