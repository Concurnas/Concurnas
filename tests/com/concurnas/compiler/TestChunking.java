package com.concurnas.compiler;

import junit.framework.TestCase;

import org.junit.Test;

import com.concurnas.runtime.utils.Chunking;

public class TestChunking extends TestCase{
	
	private String strTheIdx(int[][] xxx){
		StringBuilder sb = new StringBuilder("[");
		for(int[] xx : xxx){
			sb.append("[");
			for(int x : xx){
				sb.append(x + ",");
			}
			sb.deleteCharAt(sb.length()-1);
			sb.append("],");
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append("]");
		
		return sb.toString();
	}
	
	@Test
	public void testChunking()  {
		//odd
		assertEquals("[[0,3,6],[1,4,7],[2,5]]", "" + strTheIdx(Chunking.chunkIndexes(8, 3)));
		//odd bigger
		assertEquals("[[0,6,12],[1,7,13],[2,8,14],[3,9,15],[4,10],[5,11]]", "" + strTheIdx(Chunking.chunkIndexes(16, 6)));
		//even
		assertEquals("[[0,4],[1,5],[2,6],[3,7]]", "" + strTheIdx(Chunking.chunkIndexes(8, 4)));
		//more slots than jobs
		assertEquals("[[0],[1],[2],[3],[4],[5],[6],[7]]", "" + strTheIdx(Chunking.chunkIndexes(8, 16)));
		//nothing to do
		assertEquals("]", "" + strTheIdx(Chunking.chunkIndexes(0, 16)));
		//no slots
		assertEquals("[[0,1,2,3,4,5,6,7]]", "" + strTheIdx(Chunking.chunkIndexes(8, 0)));
		
	}

}
