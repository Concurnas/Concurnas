package com.concurnas.compiler.dma;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

import com.concurnas.lang.offheap.storage.OffHeapMalloc;

import junit.framework.TestCase;

public class NonGCHeapTests extends TestCase {
	
	private long _10meg = 10*1024*1024;
	private int _2meg = 2*1024*1024;
	/*
	private String interp(String interpAs, ByteBuffer buf, int startpos){
		
		interpAs = interpAs.replace(" ", "").replace(",", "");
		StringBuilder sb = new StringBuilder();
		buf.position(startpos);
		
		int consumed = 0;
		
		char[] items = interpAs.toCharArray();
		for(int n = 0; n < items.length; n++ ){
			char s = items[n];
			boolean islast = items.length -1 == n;
			switch(s){
				case 'L': 
					sb.append(buf.getLong()+",");
					consumed+=8;
					break;
				case 'l': //long with MSB split out
					long ll = buf.getLong(); 
					sb.append("(" + (ll >>> 63) +":" + (ll ^ 1 << 63 ) +"),"); 
					consumed+=8;
					break;
				case ']':
					if(!islast){sb.append("], ");}else{sb.append("]");}
					break;
				case '_':  break;
				default: sb.append(s);
			}
		}
		
		
		int endposx = buf.position();
		int endpos = startpos + consumed;
		if(endpos != endposx){
			sb.append(String.format("Invalid position: expect: %s, got: %s", endpos, endposx));
		}
		
		return sb.toString();
	}
	
	public void atestConstructBasic() throws Exception {
		
		OffHeap heap = new OffHeap(_10meg, _2meg);
		
		Field buffersf = OffHeap.class.getDeclaredField("buffers");
		buffersf.setAccessible(true);
		ByteBuffer[] buffers = (ByteBuffer[]) buffersf.get(heap);
		super.assertEquals(buffers.length, 5);
		ByteBuffer first = buffers[0];
		
		int endpos = first.position();
		super.assertEquals(endpos,  8*7);
		
		String got = interp("[lLLl], [lLL]", first, 0);
		super.assertEquals("[(1:0),32,32,(1:0),], [(1:10485696),0,0,]", got);
	}
	
	@Override
	public void setUp() throws Exception{
		heap = new OffHeap(_10meg, _2meg);
		Field buffersf = OffHeap.class.getDeclaredField("buffers");
		buffersf.setAccessible(true);
		ByteBuffer[] buffers = (ByteBuffer[]) buffersf.get(heap);
		first = buffers[0];
	}
	
	private ByteBuffer first;
	private OffHeap heap;
	
	public void testSimpleMalloc() throws Exception {
		
		heap.malloc(200);
		
		
		String got = interp("[lLLl], [l 218:l], [lLL]", first, 0);
		super.assertEquals("0|32|32|32|0|,10485696|0|0|", got);
	}*/

	/*public void testSimpleMalloc() throws Exception {
		OffHeapMalloc offHeapMalloc = new OffHeapMalloc(_10meg);
		long address = offHeapMalloc.malloc(128).position();
		long bigOne = offHeapMalloc.malloc(1024).position();
		long another = offHeapMalloc.malloc(128).position();
		offHeapMalloc.free(address);
		offHeapMalloc.free(bigOne);
		long address2 = offHeapMalloc.malloc(128).position();
		
		assertEquals(address, address2);//should 'reuse' use same slot from free list
		assertNotSame(address, another);//use a different one
	}*/
}
