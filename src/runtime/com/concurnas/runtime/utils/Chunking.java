package com.concurnas.runtime.utils;

public class Chunking {
	
	/**
	 * e.g. : (8, 3) -> [[0 3 6], [1 4 7] [2 5]]
	 * 
	 * @param indexesUpTo
	 * @param distributAcross
	 * @return
	 */
	public static int[][] chunkIndexes(int indexesUpTo, int distributAcross){
		int[][] ret;
		if(indexesUpTo <=0){
			ret = new int[0][0];
		}
		else if(distributAcross <=0){
			int[] me = new int[indexesUpTo];
			
			for(int n=0; n < indexesUpTo; n++){
				me[n] = n;
			}
			
			ret = new int[][]{me};
			//ret =
		}
		else{
			distributAcross = Math.min(distributAcross, indexesUpTo);//if less than distributAcross buckets
			
			int ineach = indexesUpTo / distributAcross;
			
			int remainder = indexesUpTo % distributAcross;
			
			ret = new int[distributAcross][0];
			
			for(int n=0; n < distributAcross; n++){
				int[] me = new int[ineach + (n < remainder?1:0)];
				
				int m=n;
				int i=0;
				while(m < indexesUpTo){
					me[i++] = m;
					m += distributAcross;
				}
				
				ret[n] = me;
			}
		}
		
		
	    return ret;
	}
	
}
