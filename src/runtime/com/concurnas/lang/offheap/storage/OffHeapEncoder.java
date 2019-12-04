package com.concurnas.lang.offheap.storage;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.IdentityHashMap;

import com.concurnas.bootstrap.lang.offheap.Encoder;
import com.concurnas.bootstrap.runtime.cps.CObject;
import com.concurnas.lang.Uninterruptible;
import com.concurnas.runtime.Pair;

@Uninterruptible
class OffHeapEncoder<T> implements Encoder {
	IdentityHashMap<Object, Long> graphOffsets;
	int putSoFar=0;//Real impl has long
	private boolean allocated = false;
	private long mallocAddress;
	ByteBuffer buffer;
	private MallocProvider mallocprovider;
	protected final OffHeap vidprovider;
	
	static final byte OBJECT_POINTER = 0;
	static final byte OBJECT_SERIALIZED = 1;
	static final byte OBJECT_NULL = 2;
	static final byte OBJECT_ARRAY = 3;
	
	boolean regsiterVids = true;
	private OffHeapDecoder<T> decoderForReencodings;
	
	public OffHeapEncoder(MallocProvider mallocprovider, OffHeap vidprovider){
		this.mallocprovider = mallocprovider;
		this.vidprovider = vidprovider;
		
		this.decoderForReencodings = new OffHeapDecoder<T>(mallocprovider,vidprovider);
	}
	
	public long getWritenCount() {
		return mallocAddress+putSoFar;
	}
	
	public String toString(){
		return super.toString();
		
	}
	
	private void malloc(Object myclass, int size) {
		allocated=true;
		//test = new byte[this.putSoFar];
		Pair<ByteBuffer, Long> bbandAddress = mallocprovider.malloc(size);
		buffer = bbandAddress.getA();
		mallocAddress = bbandAddress.getB();//buffer.position();
		
		//System.err.println("got mallocprovider: " + mallocprovider);
		//System.err.println("got mallocprovider buffer: " + buffer);
		//System.err.println("got mallocAddress: " + mallocAddress);
		
		//buffer = ByteBuffer.allocate(this.putSoFar);
		
		putSoFar=0;
	}
	
	public int lastEncodedSize;//TODO: for debug
	public boolean isSizeof = false;
	
	public long encode(Object object){
		long ret;
		HashMap<Class<?>, Long> needsreencode = null;
		try{
			regsiterVids = true;
			lastEncodedSize = this.put(object);
		} finally{
			ret = this.mallocAddress;
			needsreencode = requiresReencoding;
			whenDone();
		}
		
		if(needsreencode != null){
			Object got = decoderForReencodings.get(ret);
			//System.err.println("got: " + got);
			long correctlyEncoded = encode(got);
			mallocprovider.free(ret);
			//remove the erronous entries from vidprovider
			
			for(Class<?> item : needsreencode.keySet()){
				this.vidprovider.unregisterVidUse(this.vidprovider.classToVidGet(item), needsreencode.get(item));
			}
			
			return correctlyEncoded;
		}else{
			return ret;
		}
	}
	
	public int sizeof(T object){
		try{
			isSizeof = true;
			return this.put(object);
		}finally{
			isSizeof = false;
			whenDone();
		}
	}
	
	private void whenDone(){
		graphOffsets = null;
		allocated=false;
		buffer=null;
		putSoFar=0;
		mallocAddress=0;
		requiresReencoding=null;
	}
	
	
	@Override
	public int put(int aint){
		if(null != buffer){//if(allocated){
			//System.err.println(String.format("pos: %s -> %s[int]", buffer.position(), aint));
			buffer.putInt(aint);
			/*test[putSoFar] = (byte) (aint >> 24);
			test[putSoFar+1] = (byte) (aint >> 16);
			test[putSoFar+2] = (byte) (aint >> 8);
			test[putSoFar+3] = (byte) (aint);*/
		}
		putSoFar+=4;
		return 4;
	}
	
	@Override
	public int put(long along){
		if(null != buffer){//if(allocated){
			//System.err.println(String.format("pos: %s -> %s[long]", buffer.position(), along));
			buffer.putLong(along);
			/*test[putSoFar] = (byte) (along >> 56);
			test[putSoFar+1] = (byte) (along >> 48);
			test[putSoFar+2] = (byte) (along >> 40);
			test[putSoFar+3] = (byte) (along >> 32);
			test[putSoFar+4] = (byte) (along >> 24);
			test[putSoFar+5] = (byte) (along >> 16);
			test[putSoFar+6] = (byte) (along >> 8);
			test[putSoFar+7] = (byte) (along);*/
		}
		putSoFar+=8;
		return 8;
	}
	
	@Override
	public int put(double adouble){
		if(null != buffer){//if(allocated){
			//System.err.println(String.format("pos: %s -> %s[double]", buffer.position(), adouble));
			buffer.putDouble(adouble);
			/*long dtemp = Double.doubleToLongBits(adouble);

			test[putSoFar] = (byte)(dtemp>>56);
			test[putSoFar+1] = (byte)(dtemp>>48);
			test[putSoFar+2] = (byte)(dtemp>>40);
			test[putSoFar+3] = (byte)(dtemp>>32);
			test[putSoFar+4] = (byte)(dtemp>>24);
			test[putSoFar+5] = (byte)(dtemp>>16);
			test[putSoFar+6] = (byte)(dtemp>>8);
			test[putSoFar+7] = (byte)(dtemp);*/
		}
		putSoFar+=8;
		return 8;
	}
	
	@Override
	public int put(float afloat){
		if(null != buffer){//if(allocated){
			buffer.putFloat(afloat);
			/*long ftemp = Float.floatToIntBits(afloat);

			test[putSoFar] = (byte)(ftemp>>24);
			test[putSoFar+1] = (byte)(ftemp>>16);
			test[putSoFar+2] = (byte)(ftemp>>8);
			test[putSoFar+3] = (byte)(ftemp);*/
		}
		putSoFar+=4;
		return 4;
	}
	
	@Override
	public int put(boolean aboolean){
		if(null != buffer){//if(allocated){
			buffer.put(aboolean?(byte) 1:(byte) 0);
			//test[putSoFar] = aboolean?(byte) 1:(byte) 0;
		}
		putSoFar+=1;
		return 1;
	}
	
	@Override
	public int put(short aShort){
		if(null != buffer){//if(allocated){
			buffer.putShort(aShort);
			//test[putSoFar] = (byte)(aShort>>8);
			//test[putSoFar+1] = (byte)(aShort);
		}
		putSoFar+=2;
		return 2;
	}
	
	@Override
	public int put(byte a){
		if(null != buffer){//if(allocated){
			//System.err.println(String.format("pos: %s -> %s[byte]", buffer.position(), a));
			buffer.put(a);
			//test[putSoFar] = a;
		}
		putSoFar+=1;
		return 1;
	}
	
	@Override
	public int put(char aChar){
		if(null != buffer){//if(allocated){
			buffer.putChar(aChar);
			//test[putSoFar] = (byte)(aChar>>8);
			//test[putSoFar+1] = (byte)(aChar);
		}
		putSoFar+=2;
		return 2;
	}

	//private static final int CAM_TO_ENCODER_CACHE_SUZE = 300;
	//private IdentityHashMap<Object, EncodingTranslator<T>> cacheClassToEncoder = new IdentityHashMap<Object, EncodingTranslator<T>>();
	
	/*public void onReplaceClassloader() {
		this.cacheClassToEncoder = new LRUCache<Class<?>, EncodingTranslator<T>>(CAM_TO_ENCODER_CACHE_SUZE);
	}*/
	
	//TODO: makes this more efficient - needs whitelist for string etc
	
	private HashMap<Class<?>, Long> requiresReencoding = null;
	
	private Class<?> checkClassDefVersion(Class<?> objCls, Object onObj, boolean addToReencodingMap){
		//if(!requiresReencoding){
			ClassLoader loader = this.vidprovider.getClassloader();
			try {
				Class<?> myDefCls = loader.loadClass(objCls.getName());
				String[] declMeta = Utils.callMetaBinary(objCls);
				if(null != declMeta){
					String[] toStoreMeta = Utils.callMetaBinary(myDefCls);
					if(null != toStoreMeta){
						
						if(!Utils.twoStringArEquals(declMeta, toStoreMeta)){
							if(!inRootsizeOfPhase && addToReencodingMap){
								if(null == requiresReencoding){
									requiresReencoding = new HashMap<Class<?>, Long>();
								}
								requiresReencoding.put(objCls, requiresReencoding.getOrDefault(objCls, 0L) + 1L);
							}
							
							return myDefCls;
						}
					}
				}
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Class definition is missing in classloader: " + e.getMessage(), e);
			}
			return null;
		//}
		
	/*	if(this.cacheClassToEncoder.containsKey(onObj)){
			return this.cacheClassToEncoder.get(onObj);
		}
		else{*/
			//EncodingTranslator<T> encodingTranslator = null;
			
			//if(this.vidprovider.getHasNonDefaultClassloaderDefined()){
				/*ClassLoader loader = this.vidprovider.getClassloader();
				try {
					Class<?> myDefCls = loader.loadClass(objCls.getName());
					String[] declMeta = Utils.callMetaBinary(objCls);
					if(null != declMeta){
						String[] toStoreMeta = Utils.callMetaBinary(myDefCls);
						if(null != toStoreMeta){
							
							if(!Utils.twoStringArEquals(declMeta, toStoreMeta)){

								//System.err.println("for cls " + objCls.getName());
								//System.err.println("from " + Arrays.toString(declMeta));
								//System.err.println("to " + Arrays.toString(toStoreMeta));
								
								//encode withexisting one
								//decode to new
								//encoder to new one and persist this
								
								//encodingTranslator = Utils.createEncodingTranslator(this, declMeta, toStoreMeta, objCls, myDefCls);
								
								//System.err.println("cust encoder for: " + objCls);
								
								//encodingTranslator = transEnc;
							}
						}
					}
				} catch (ClassNotFoundException e) {
					//throw new RuntimeException("Class definition is missing in classloader: " + e.getMessage(), e);
				}*/
			//}
			
			/*this.cacheClassToEncoder.put(onObj, encodingTranslator);
			//System.err.println("dec: " + decoder.getClass());
			return encodingTranslator;*/
		//}
		
	}
	
	private boolean inRootsizeOfPhase;
	
	@Override
	public int put(Object object){
		int size;
		if(graphOffsets == null){//indicates this is the root object
			HashMap<Long, Long> vidToCountloc = new HashMap<Long, Long>();
			this.vidToCount = vidToCountloc;
			this.graphOffsets = new IdentityHashMap<Object, Long>();
			
			size = this.put(0);
			inRootsizeOfPhase=true;
			size += this.put(object);
			inRootsizeOfPhase=false;
			
			if(!isSizeof){
				int vidMapSize=8 + (vidToCountloc.size()*2*16);
				int objectSize = this.putSoFar;
				this.malloc(object, objectSize + vidMapSize);
				//output
				graphOffsets.clear();
				vidToCount=null;//null it because we have the info already
				
				size = this.put(objectSize);
				size += this.put(object);//returned not used
				//now write out the vidmap...
				size += this.put(vidToCountloc.size());
				for(Long key : vidToCountloc.keySet()){
					long thevid = key.longValue();
					long vidCount = vidToCountloc.get(key).longValue();
					size += this.put(thevid);
					size += this.put(vidCount);
					
					if(null != vidprovider){
						this.vidprovider.registerVidUsage(thevid, vidCount);
					}
				}
				//tag useage in one operation
			}else{
				vidToCount=null;//null it because we have the info already
			}
			
			graphOffsets=null; 
		}
		else{
			if(object==null){
				size = this.tagObject(object, null, true, false, true);
			}else{//no vid needed
				boolean visitAlready = graphOffsets.containsKey(object);
				Class<?> objCls = object.getClass();
				Class<?> storeAsVidCls = null;
				if(!isSizeof){
					storeAsVidCls = checkClassDefVersion(objCls, object, !visitAlready);
				}
				
				//EncodingTranslator<T> encTrans = isSizeof?null:getEncoder(objCls, object);//doesnt work for sizeof
				
				boolean isArray = objCls.isArray();
				boolean isEnum = objCls.isEnum();
				size=this.tagObject(object, storeAsVidCls, isArray, true, !isEnum);
				if(!visitAlready){
					
					
					if(isArray){
						Class<?> compType = objCls.getComponentType();
						int cnt = 1;
						while(compType.isArray()){
							cnt++;
							compType = compType.getComponentType();
						}
						if(compType.isPrimitive()){
							if(compType==int.class){
								size += this.putIntArray(object, cnt, false);
							}else if(compType==long.class){
								size += this.putLongArray(object, cnt, false);
							}else if(compType==short.class){
								size += this.putShortArray(object, cnt, false);
							}else if(compType==double.class){
								size += this.putDoubleArray(object, cnt, false);
							}else if(compType==float.class){
								size += this.putFloatArray(object, cnt, false);
							}else if(compType==char.class){
								size += this.putCharArray(object, cnt, false);
							}else if(compType==boolean.class){
								size += this.putBooleanArray(object, cnt, false);
							}else if(compType==byte.class){
								size += this.putByteArray(object, cnt, false);
							}
							
						}else{
							size += putObjectArray(object, cnt, false);
						}
					}else if(isEnum){
						
						
						int before = this.putSoFar;
						try {
							//objCls.getMethod("toBinary", Encoder.class).invoke(object, this);
							size += this.putByteArray(((Enum)object).name().getBytes(), 1);
						} catch (Exception e) {
							throw new RuntimeException(e);
						} 
						size += this.putSoFar-before;
					}
					else if(objCls == String.class){
						size += this.putByteArray(object.toString().getBytes(), 1);
					}else if(objCls == Integer.class){
						size += this.put(((Integer)object).intValue());
					}else if(objCls == Long.class){
						size += this.put(((Long)object).longValue());
					}else if(objCls == Double.class){
						size += this.put(((Double)object).doubleValue());
					}else if(objCls == Float.class){
						size += this.put(((Float)object).floatValue());
					}else if(objCls == Boolean.class){
						size += this.put(((Boolean)object).booleanValue());
					}else if(objCls == Character.class){
						size += this.put(((Character)object).charValue());
					}else if(objCls == Short.class){
						size += this.put(((Short)object).shortValue());
					}else if(objCls == Byte.class){
						size += this.put(((Byte)object).byteValue());
					}else if(objCls == Class.class){
						String canName = ((Class<?>)object).getName();//.getCanonicalName();
						//System.err.println("=>cannonicalName:" + canName);
						size += this.putByteArray(canName.getBytes(), 1);
					}
					else{
						int before = this.putSoFar;
						
						/*if(null != encTrans){
							try{
								if(this.allocated){//already have obj in memory
									((CObject)encTrans.objectInNewForm).toBinary(this);
								}else {
									encTrans.preEncode();
									//System.err.println("preEncode with: " + encTrans);
									((CObject)object).toBinary(this);
									encTrans.postencode();
									//((CObject)encTrans.objectInNewForm).toBinary(this);
								}
							}
							catch(Throwable thr){
								encTrans.onFail();
								throw thr;
							}
						}else{*/
							((CObject)object).toBinary(this);
						//}
						
						size += this.putSoFar-before;
					}
				}
			}
		}
		return size;
	}
		
	private HashMap<Long, Long> vidToCount = new HashMap<Long, Long>();
	
	private int tagObject(Object object, Class<?> storeVidCls, /*EncodingTranslator<T> encTrans, */boolean isArray, boolean includeVid, boolean addToGraph){
		if(object == null){
			return put(OBJECT_NULL);
		}else if(graphOffsets.containsKey(object)){
			long objectOffset = graphOffsets.get(object);
			int size = this.put(OBJECT_POINTER);//goto pnter
			size += this.put(objectOffset);//pnter
			return size;//TODO: 9 when long for pnter
		}else{//serialize this
			int size =0;
			size += this.put(isArray?OBJECT_ARRAY:OBJECT_SERIALIZED);//goto pnter
			if(includeVid){
				//size += this.putObjectVid(object);
				//boolean tagVid;
				/*Class<?> cls;
				if(null == encTrans){
					cls = object.getClass();
					//tagVid = false;
				}else{
					cls = encTrans.getEncodedClass();
					//cls = object.getClass();
					//tagVid = true;
				}*/
				
				//Class<?> cls = storeVidCls!=null?storeVidCls:object.getClass();
				Class<?> cls = object.getClass();
				
				//this.vidprovider.getClassloader()
				
				if(cls.isArray()){
					cls = cls.getComponentType();
					int dimentions=1;
					while(cls.isArray()){
						cls = cls.getComponentType();
						dimentions++;
					}
					size += put(dimentions);
				}
				size += 8;
				
				long vid;
				if(buffer != null){
					int pos = buffer.position();
					vid = vidprovider==null?0:this.vidprovider.getObjectClassUIDAndTrackUsage(cls, storeVidCls);
					buffer.position(pos);
				}else{
					vid = vidprovider==null?0:this.vidprovider.getObjectClassUIDAndTrackUsage(cls, storeVidCls);
				}

				//System.err.println(String.format("%s -> %s", cls, vid));
				
				put(vid);
				/*if(null != encTrans){
					encTrans.putObjTypeAndVid(OBJECT_SERIALIZED, vid);
				}else*/
				
				//System.err.println("persist vid: " + vid);
				if(null != vidToCount && regsiterVids){
					vidToCount.put(vid, vidToCount.containsKey(vid)?vidToCount.get(vid)+1:1l);
				}
			}
			
			if(addToGraph){
				//System.err.println("put obj: " + object + " obj type: " + object.getClass());
				graphOffsets.put(object, this.getWritenCount());
			}
			
			return size;
		}
	}
	
	@Override
	public int putIntArray(Object object, int levels){
		return putIntArray(object, levels, true);
	}
	
	@Override
	public int putLongArray(Object object, int levels){
		return putLongArray(object, levels, true);
	}
	
	@Override
	public int putDoubleArray(Object object, int levels){
		return putDoubleArray(object, levels, true);
	}
	
	@Override
	public int putFloatArray(Object object, int levels){
		return putFloatArray(object, levels, true);
	}
	
	@Override
	public int putShortArray(Object object, int levels){
		return putShortArray(object, levels, true);
	}
	
	@Override
	public int putCharArray(Object object, int levels){
		return putCharArray(object, levels, true);
	}
	
	@Override
	public int putByteArray(Object object, int levels){
		return putByteArray(object, levels, true);
	}
	
	@Override
	public int putBooleanArray(Object object, int levels){
		return putBooleanArray(object, levels, true);
	}
	
	private int putIntArray(Object object, int levels, boolean doNullCheck){
		int size=0;
		if(doNullCheck){
			size = this.tagObject(object, null, true, false, true);
			if(object==null){
				return size;
			}//no vid needed
		}
		
		int len = Array.getLength(object);
		size += put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				size += put((int)got);
			}else{
				size += putIntArray(got, levels-1);
			}
		}
		
		return size;		
	}
	
	private int putLongArray(Object object, int levels, boolean doNullCheck){
		int size=0;
		if(doNullCheck){
			size = this.tagObject(object, null, true, false, true);
			if(object==null){
				return size;
			}//no vid needed
		}
		int len = Array.getLength(object);
		size += put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				size += put((long)got);
			}else{
				size += putLongArray(got, levels-1);
			}
		}
		
		return size;		
	}
	
	private int putDoubleArray(Object object, int levels, boolean doNullCheck){
		int size=0;
		if(doNullCheck){
			size = this.tagObject(object,null,  true, false, true);
			if(object==null){
				return size;
			}//no vid needed
		}
		int len = Array.getLength(object);
		size += put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				size += put((double)got);
			}else{
				size += putDoubleArray(got, levels-1);
			}
		}
		
		return size;		
	}
	
	private int putFloatArray(Object object, int levels, boolean doNullCheck){
		int size=0;
		if(doNullCheck){
			size = this.tagObject(object, null, true, false, true);
			if(object==null){
				return size;
			}//no vid needed
		}
		int len = Array.getLength(object);
		size += put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				size += put((float)got);
			}else{
				size += putFloatArray(got, levels-1);
			}
		}
		
		return size;		
	}
	
	private int putBooleanArray(Object object, int levels, boolean doNullCheck){
		int size=0;
		if(doNullCheck){
			size = this.tagObject(object, null, true, false, true);
			if(object==null){
				return size;
			}//no vid needed
		}
		int len = Array.getLength(object);
		size += put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				size += put((boolean)got);
			}else{
				size += putBooleanArray(got, levels-1);
			}
		}
		
		return size;		
	}
	
	private int putShortArray(Object object, int levels, boolean doNullCheck){
		int size=0;
		if(doNullCheck){
			size = this.tagObject(object, null, true, false, true);
			if(object==null){
				return size;
			}//no vid needed
		}
		int len = Array.getLength(object);
		size += put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				size += put((short)got);
			}else{
				size += putShortArray(got, levels-1);
			}
		}
		
		return size;		
	}
	
	private int putByteArray(Object object, int levels, boolean doNullCheck){
		int size=0;
		if(doNullCheck){
			size = this.tagObject(object, null, true, false, true);
			if(object==null){
				return size;
			}//no vid needed
		}
		int len = Array.getLength(object);
		size += put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				size += put((byte)got);
			}else{
				size += putByteArray(got, levels-1);
			}
		}
		
		return size;		
	}
	
	private int putCharArray(Object object, int levels, boolean doNullCheck){
		int size=0;
		if(doNullCheck){
			size = this.tagObject(object, null, true, false, true);
			if(object==null){
				return size;
			}//no vid needed
		}
		int len = Array.getLength(object);
		size += put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				size += put((char)got);
			}else{
				size += putCharArray(got, levels-1);
			}
		}
		
		return size;		
	}
	
	private int putObjectArray(Object object, int levels, boolean tag){
		int size = 0;
		if(tag){
			size = this.tagObject(object, null, true, true, true);
		}
		if(object==null){
			return size;
		}//no vid needed
		int len = Array.getLength(object);
		size += put(len);
		for(int n=0; n < len; n++){
			Object got = Array.get(object, n);
			if(levels == 1){
				size += put(got);
			}else{
				size += putObjectArray(got, levels-1);
			}
		}
		
		return size;		
	}
	
	@Override
	public int putObjectArray(Object object, int levels){
		return putObjectArray(object, levels, true);		
	}

/*	public MallocProvider getMallocProvider(){
		return this.mallocprovider;
	}
	public void setMallocProvider(MallocProvider x){
		this.mallocprovider = x;
	}*/

	boolean getAllocated() {
		return this.allocated;
	}
	
}