package com.concurnas.runtimeCache;

class PctDoer{
	private int max;
	private int cnt = 0;
	private int incrementPct;
	private double lastPubSend =0;
	private String msgBody;
	private ProgressTracker mainTracker;
	private int idx = 0;
	
	public PctDoer(ProgressTracker mainTracker, int max, int incrementPct, String msgBody, int idx){
		this.mainTracker = mainTracker;
		this.max = max;
		this.incrementPct = incrementPct;
		this.msgBody = msgBody;
		this.idx = idx;
	}
	
	public void entryDone(){
		cnt++;
		
		double currentPct =  (double)cnt / (double)max * 100.;
		
		if(cnt == max || currentPct >= (lastPubSend + incrementPct) ){
			if(max > -1) {
				lastPubSend = currentPct;
			}
			//String.format("%s%.0f%%", msgBody, lastPubSend )
			this.mainTracker.log(idx, null);
		}
			
	}
	
	
}