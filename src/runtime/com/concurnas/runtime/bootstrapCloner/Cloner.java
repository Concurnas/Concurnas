package com.concurnas.runtime.bootstrapCloner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.concurnas.bootstrap.runtime.CopyDefinition;
import com.concurnas.bootstrap.runtime.CopyTracker;
import com.concurnas.bootstrap.runtime.InitUncreatable;
import com.concurnas.bootstrap.runtime.ref.Ref;
import com.concurnas.lang.Actor;
import com.concurnas.lang.Shared;
import com.concurnas.lang.Transient;
import com.concurnas.lang.Uninterruptible;
import com.concurnas.runtime.ConcImmutable;
import com.concurnas.runtime.channels.PriorityQueue;
import com.concurnas.runtime.ref.Local;
import com.concurnas.runtime.ref.RefArray;

import sun.misc.Unsafe;

@Uninterruptible
public final class Cloner {

	@Shared	private final static Set<Class<?>>								ignored				= new HashSet<Class<?>>();
	@Shared private final static Set<Class<?>>								ignoredInstanceOf	= new HashSet<Class<?>>();
	@Shared private final static Map<Class<?>, IFastCloner>					fastCloners			= new HashMap<Class<?>, IFastCloner>();
	@Shared private final Map<Object, Boolean>						    ignoredInstances	 = Collections.synchronizedMap(new IdentityHashMap<Object, Boolean>());
	@Shared	private final ConcurrentHashMap<Class<?>, List<Field>>			fieldsCache			= new ConcurrentHashMap<Class<?>, List<Field>>();

	
	static {
		registerKnownJdkImmutableClasses();
		registerFastCloners();
	}

	/*
	 * private void init() { registerKnownConstants(); }
	 */
	
	private Cloner(){
		//init();
	}
	
	private Cloner(Cloner c){
		this();
	}
	
	@Shared public final static Cloner cloner = new Cloner();
	

	/**
	 * registers a std set of fast cloners.
	 */
	private static void registerFastCloners()
	{
		fastCloners.put(GregorianCalendar.class, new FastClonerCalendar());
		fastCloners.put(ArrayList.class, new FastClonerArrayList());
		fastCloners.put(LinkedList.class, new FastClonerLinkedList());
		fastCloners.put(HashSet.class, new FastClonerHashSet());
		fastCloners.put(HashMap.class, new FastClonerHashMap());
		fastCloners.put(TreeMap.class, new FastClonerTreeMap());
		fastCloners.put(ConcurrentHashMap.class, new FastClonerConcurrentHashMap());
	}

	private Object fastClone(final Object o, final CopyTracker tracker) throws IllegalAccessException
	{
		final Class<? extends Object> c = o.getClass();
		final IFastCloner fastCloner = fastCloners.get(c);
		if (fastCloner != null) return fastCloner.clone(o, this, tracker);
		return null;
	}

	private void registerConstant(final Class<?> c, final String privateFieldName)
	{
		try
		{
			final Field field = c.getDeclaredField(privateFieldName);
			field.setAccessible(true);
			final Object v = field.get(null);
			ignoredInstances.put(v, true);
		} catch (final SecurityException e)
		{
			throw new RuntimeException(e);
		} catch (final NoSuchFieldException e)
		{
			throw new RuntimeException("missing field on: " + c.getName() + " field: " + e.getMessage(), e);
		} catch (final IllegalArgumentException e)
		{
			throw new RuntimeException(e);
		} catch (final IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static void registerKnownJdkImmutableClasses()
	{
		registerImmutable(String.class);
		registerImmutable(Integer.class);
		registerImmutable(Long.class);
		registerImmutable(Boolean.class);
		registerImmutable(Class.class);
		registerImmutable(Float.class);
		registerImmutable(Double.class);
		registerImmutable(Character.class);
		registerImmutable(Byte.class);
		registerImmutable(Short.class);
		registerImmutable(Void.class);

		registerImmutable(BigDecimal.class);
		registerImmutable(BigInteger.class);
		registerImmutable(URI.class);
		registerImmutable(URL.class);
		registerImmutable(UUID.class);
		registerImmutable(Pattern.class);
		
		registerImmutable(Thread.class);
		
		registerImmutable(Ref.class);//immutable cos u did the thing where set and get do copying
		registerImmutable(Local.class);//immutable cos u did the thing where set and get do copying
		registerImmutable(RefArray.class);//immutable cos u did the thing where set and get do copying
		registerImmutable(PriorityQueue.class);//this approach doesnt scale.. - should be able to do -everything for jar x: under this path - no copy etc
		//TODO: all children of Local are also immutable?
		//TODO: all children of Ref are also immutable?
		//TODO: need to be able to mark and force immutability on classes
		registerImmutableInstaceOf(Actor.class);
		registerImmutableInstaceOf(Ref.class);
		registerImmutableInstaceOf(ClassLoader.class);
	}

	/*
	 * private void registerKnownConstants(){// registering known constants of the
	 * jdk. registerStaticFields(TreeSet.class, HashSet.class, HashMap.class,
	 * TreeMap.class); }
	 */

	/*
	 * registers all static fields of these classes. Those static fields won't be cloned when an instance
	 * of the class is cloned.
	 * 
	 * This is useful i.e. when a static field object is added into maps or sets. At that point, there is no
	 * way for the cloner to know that it was static except if it is registered. 
	 * 
	 * @param classes		array of classes
	 */
	/*
	 * private void registerStaticFields(final Class<?>... classes) { for (final
	 * Class<?> c : classes) { final List<Field> fields = allFieldsStatic(c); for
	 * (final Field field : fields) { final int mods = field.getModifiers(); if
	 * (Modifier.isStatic(mods) && !field.getType().isPrimitive()) {
	 * registerConstant(c, field.getName()); } } } }
	 */
	
	private static List<Field> allFieldsStatic(final Class<?> c)
	{
		LinkedList<Field> l = new LinkedList<Field>();
		final Field[] fields = c.getDeclaredFields();
		addAll(l, fields);
		Class<?> sc = c;
		while ((sc = sc.getSuperclass()) != Object.class && sc != null)
		{
			addAll(l, sc.getDeclaredFields());
		}
		return l;
	}
	
	/**
	 * Immutable classes are not cloned.
	 */
	private static void registerImmutable(final Class<?>... c) {
		for (final Class<?> cl : c) {
			ignored.add(cl);
		}
	}
	
	/**
	 * All subclasses of classes passed here are not cloned.
	 */
	private static void registerImmutableInstaceOf(final Class<?>... c) {
		for (final Class<?> cl : c) {
			ignoredInstanceOf.add(cl);
		}
	}

	/**
	 * creates a new instance of c. Override to provide your own implementation
	 * 
	 * @param <T>		the type of c
	 * @param c			the class
	 * @return			a new instance of c
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	
	@Shared private static Unsafe unsafe;
	static{
		try {
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
	        unsafe = (Unsafe) f.get(null);
		} catch (Exception  e) {
			throw new RuntimeException(e);
		} 
	}
	
	private <T> T newInstance(final Class<T> cls) throws InstantiationException, IllegalAccessException	{
		T f = (T)unsafe.allocateInstance(cls);
        return f;
	}

	public static interface IFreezable
	{
		public boolean isFrozen();

	}
	
	private final Map<Class<?>, Boolean> hasImmutableAnnotationMap = Collections.synchronizedMap(new WeakHashMap<Class<?>, Boolean>());
	private boolean hazImmutableAnnotation(final Class<?> clz){
		Boolean ret = hasImmutableAnnotationMap.get(clz);
		
		if(ret != null){
			return ret;
		}
		else{
			boolean haz = false;
			for(final Annotation annot: clz.getAnnotations()){
				//System.err.println("" + annot);
				if (annot.annotationType() == ConcImmutable.class){
					haz= true;
					break;
				}
			}
			hasImmutableAnnotationMap.put(clz, haz);
			return haz;
		}
	}
	
	private final Map<Class<?>, Boolean> hazTransientAnnotationMap = Collections.synchronizedMap(new WeakHashMap<Class<?>, Boolean>());
	private boolean hazTransientAnnotation(final Class<?> clz){
		return checkForAnnotation(clz, hazTransientAnnotationMap, Transient.class);
	}
	
	private final Map<Class<?>, Boolean> hazSharedAnnotationMap = Collections.synchronizedMap(new WeakHashMap<Class<?>, Boolean>());
	private boolean hazSharedAnnotation(final Class<?> clz){
		return checkForAnnotation(clz, hazSharedAnnotationMap, Shared.class);
	}
	
	private boolean checkForAnnotation(final Class<?> clz, Map<Class<?>, Boolean> cache, Class<?> annotwanted) {
		Boolean ret = cache.get(clz);
		if(ret != null){
			return ret;
		}
		else{
			boolean haz = false;
			
			for(Class<?> iface : clz.getInterfaces()) {
				if(checkForAnnotation(iface, cache, annotwanted)) {
					haz= true;
					break;
				}
			}
			
			if(!haz) {
				Class<?> opon = clz;
				while(opon != Object.class){
					for(final Annotation annot: opon.getAnnotations()){
						//System.err.println("" + annot);
						if (annot.annotationType() == annotwanted){//Shared.class
							haz= true;
							break;
						}
					}
					if(haz){
						break;
					}else{
						opon = opon.getSuperclass();
						if(opon == null) {
							break;
						}
					}
				}
			}
			
			cache.put(clz, haz);
			return haz;
		}
	}
	

	public <T> T clone(final T obj){
		CopyTracker tracker = new CopyTracker();
		return clone(tracker, obj, null, false);
	}
	
	public <T> T cloneRepointTransient(final T obj){
		CopyTracker tracker = new CopyTracker();
		return clone(tracker, obj, null, true);
	}
	

	public <T> T clone(final CopyTracker tracker, final T obj, CopyDefinition def){
		return clone(tracker, obj, def, false);
	}
	
	@SuppressWarnings("unchecked")
	private <T> T clone(final CopyTracker tracker, final T obj, CopyDefinition def, boolean repointTransient){
		try{
			if (obj == null) return null;
			if (obj == this) return (T)this;
			
			
			if (ignoredInstances.containsKey(obj)) return obj;
			Class<T> clz = (Class<T>) obj.getClass();
			
			{
				Class<?> clzig = clz;
				while(clzig != null) {
					if (ignored.contains(clzig)) {
						return obj;
					}
					clzig = clzig.getSuperclass();
				}
				
			}
			
			
			for (final Class<?> iClz : ignoredInstanceOf)
			{
				if (iClz.isAssignableFrom(clz)) {
					ignored.add(iClz);//quicker next time
					return obj;
				}
			}
			
			if(null == def) {
				if (hazImmutableAnnotation(clz)) return obj;
				if (hazTransientAnnotation(clz)) {
					if(repointTransient) {
						return obj;
					}
					return null;
				}
				if (hazSharedAnnotation(clz)) return obj;
			}
			
			if (obj instanceof IFreezable)
			{
				final IFreezable f = (IFreezable) obj;
				if (f.isFrozen()) return obj;
			}
			Map<Object, Object> clones = tracker.clonedAlready;
			
			final Object clonedPreviously = clones != null ? clones.get(obj) : null;
			if (clonedPreviously != null) return (T) clonedPreviously;

			final Object fastClone = fastClone(obj, tracker);
			if (fastClone != null){
				if (clones != null){
					clones.put(obj, fastClone);
				}
				return (T) fastClone;
			}
			
			if (clz.isArray()){
				final int length = Array.getLength(obj);
				final T newInstance = (T) Array.newInstance(clz.getComponentType(), length);
				if (clones != null)
				{
					clones.put(obj, newInstance);
				}
				for (int i = 0; i < length; i++)
				{
					final Object v = Array.get(obj, i);
					final Object clone = clones != null ? clone(tracker, v, null) : v;
					Array.set(newInstance, i, clone);
				}
				return newInstance;
			}

			final T newInstance = newInstance(clz);
			if (clones != null)
			{
				clones.put(obj, newInstance);
			}
			
			if(def != null) {
				//super?
				Method defField = null;
				boolean incdefaults = def.incDefaults();
				
				if(incdefaults) {
					try{
						//TODO: can we cache the Methods?
						defField = clz.getDeclaredMethod("defaultFieldInit$", InitUncreatable.class, boolean[].class);
						defField.setAccessible(true);//TODO: threadlocal?
					}
					catch( NoSuchMethodException e){}
				}
				
				
				
				while(def != null) {
					CopyDefinition supCD = def.getSuperCopier();
					
					List<Field> fields = new ArrayList<Field>();
					addAll(fields, clz.getDeclaredFields());
					Collections.sort(fields, (a, b) -> a.getName().compareTo(b.getName()));
					boolean[] forDefaults = new boolean[fields.size()];
					int n=0;
					for (final Field field : fields){//TODO: cache?
						boolean needsDefault = true;
						final int modifiers = field.getModifiers();
						if (!Modifier.isStatic(modifiers)){
							
							String fname = field.getName();
							if(def.shouldCopyField(fname)) {
								if (Modifier.isTransient(modifiers)){//trans to null
									if(repointTransient) {
										field.set(newInstance, def.getOverride(fname, field.get(obj)));
									}else {
										final Class<?> type = field.getType();
										if (!type.isPrimitive()){
											field.set(newInstance, null);
										}
									}
								} else{
									final Object fieldObject = def.getOverride(fname, field.get(obj));
									final Object fieldObjectClone = clones != null ? clone(tracker, fieldObject, def.getFieldCopier(fname)) : fieldObject;
									field.set(newInstance, fieldObjectClone);
								}
								needsDefault=false;
							}
						}
						
						forDefaults[n++] = needsDefault;
					}
					
					if(null != defField) {
						defField.invoke(newInstance, null, forDefaults);
					}
					
					if(null == supCD) {//add rest of fields to instance...
						final List<Field> supfields = allFields(clz.getSuperclass());
						addfieldsToInstance(supfields, obj, clones, tracker, newInstance, repointTransient);
						def = null;
					}else {
						clz = (Class<T>) clz.getSuperclass();
						def = supCD;
						
						if(incdefaults) {
							try{
								//TODO: can we cache the Methods?
								defField = clz.getDeclaredMethod("defaultFieldInit$", InitUncreatable.class, boolean[].class);
								defField.setAccessible(true);//TODO: threadlocal?
							}
							catch( NoSuchMethodException e){}
						}
					}
				}
				
			}
			else {
				final List<Field> fields = allFields(clz);
				addfieldsToInstance(fields, obj, clones, tracker, newInstance, repointTransient);
			}
			

			//System.err.println(String.format("asked to code: %s: %s -> %s", obj.getClass(), System.identityHashCode(obj), System.identityHashCode(newInstance)));
			
			return newInstance;
		}
		catch(Exception e){
			throw new RuntimeException("Failure during copy process: " + e.getMessage(), e);
		}
	}
	
	private <T> void addfieldsToInstance(final List<Field> fields, Object obj, Map<Object, Object> clones, final CopyTracker tracker, T newInstance, boolean repointTransient) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException{
		for (final Field field : fields){
			final int modifiers = field.getModifiers();
			if (!Modifier.isStatic(modifiers)){
				if (Modifier.isTransient(modifiers)){//trans to null
					if(repointTransient) {
						field.set(newInstance, field.get(obj));
					}else {
						final Class<?> type = field.getType();
						if (!type.isPrimitive()){
							field.set(newInstance, null);
						}
					}
				} else{
					final Object fieldObject = field.get(obj);
					boolean isShared = false;
					for(Annotation annot : field.getAnnotations()) {
						if (annot.annotationType() == Shared.class){
							isShared=true;
							break;
						}
					}
					
					Object fieldObjectClone = fieldObject;
					if(!isShared) {
						fieldObjectClone = clones != null ? clone(tracker, fieldObject, null) : fieldObject;
					}
					
					if((field.getModifiers() & Modifier.FINAL) == Modifier.FINAL) {
				        unsafe.putObject( obj, unsafe.objectFieldOffset( field ), fieldObjectClone );
					}else {
						field.set(newInstance, fieldObjectClone);
					}
				}
			}
		}
	}
	

	private static void addAll(final List<Field> l, final Field[] fields)
	{
		for (final Field field : fields)
		{
			if (!field.isAccessible())
			{
				field.setAccessible(true);
			}
			l.add(field);
		}
	}

	/**
	 * reflection utils, override this to choose which fields to clone
	 */
	private List<Field> allFields(final Class<?> c)
	{
		List<Field> l = fieldsCache.get(c);
		if (l == null)
		{
			l = new LinkedList<Field>();
			final Field[] fields = c.getDeclaredFields();
			addAll(l, fields);
			Class<?> sc = c;
			while ((sc = sc.getSuperclass()) != Object.class && sc != null)
			{
				addAll(l, sc.getDeclaredFields());
			}
			fieldsCache.putIfAbsent(c, l);
		}
		return l;
	}

	private static interface IFastCloner
	{
		public Object clone(Object t, Cloner cloner, CopyTracker tracker) throws IllegalAccessException;
	}
	
	private final static class FastClonerArrayList   implements IFastCloner
	{
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Object clone(final Object t, final Cloner cloner, final CopyTracker tracker) throws IllegalAccessException
		{
			final ArrayList al = (ArrayList) t;
			final ArrayList l = new ArrayList();
			for (final Object o : al)
			{
				final Object cloneInternal = cloner.clone(tracker, o, null);
				l.add(cloneInternal);
			}
			return l;
		}

	}
	
	private final static class FastClonerCalendar   implements IFastCloner
	{
		public Object clone(final Object t, final Cloner cloner, final CopyTracker tracker)
		{
			final GregorianCalendar gc = new GregorianCalendar();
			gc.setTimeInMillis(((GregorianCalendar) t).getTimeInMillis());
			return gc;
		}
	}
	
	private final static class FastClonerConcurrentHashMap   implements IFastCloner
	{
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Object clone(final Object t, final Cloner cloner, final CopyTracker tracker) throws IllegalAccessException
		{
			final ConcurrentHashMap<Object, Object> m = (ConcurrentHashMap) t;
			final ConcurrentHashMap result = new ConcurrentHashMap();
			for (final Map.Entry e : m.entrySet())
			{
				final Object key = cloner.clone(tracker, e.getKey(), null);
				final Object value = cloner.clone(tracker, e.getValue(), null);

				result.put(key, value);
			}
			return result;
		}
	}
	
	private static final class FastClonerHashMap   implements IFastCloner
	{
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Object clone(final Object t, final Cloner cloner, final CopyTracker tracker) throws IllegalAccessException
		{
			final HashMap<Object, Object> m = (HashMap) t;
			final HashMap result = new HashMap();
			for (final Object keyx : m.keySet().toArray())//to array to avoid conc modi exception
			{
				final Object key = cloner.clone(tracker, keyx, null);
				final Object value = cloner.clone(tracker, m.get(keyx), null);

				result.put(key, value);
			}
			return result;
		}
	}
	
	private static final class FastClonerHashSet   implements IFastCloner
	{
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Object clone(final Object t, final Cloner cloner, final CopyTracker tracker) throws IllegalAccessException
		{
			final HashSet al = (HashSet) t;
			final HashSet l = new HashSet();
			for (final Object o : al)
			{
				final Object cloneInternal = cloner.clone(tracker, o, null);
				l.add(cloneInternal);
			}
			return l;
		}
	}
	
	private static final class FastClonerLinkedList   implements IFastCloner
	{
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Object clone(final Object t, final Cloner cloner, final CopyTracker tracker) throws IllegalAccessException
		{
			final LinkedList al = (LinkedList) t;
			final LinkedList l = new LinkedList();
			for (final Object o : al)
			{
				final Object cloneInternal = cloner.clone(tracker, o, null);
				l.add(cloneInternal);
			}
			return l;
		}
	}
	
	private static final class FastClonerTreeMap   implements IFastCloner
	{
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Object clone(final Object t, final Cloner cloner, final CopyTracker tracker) throws IllegalAccessException
		{
			final TreeMap<Object, Object> m = (TreeMap) t;
			final TreeMap result = new TreeMap(m.comparator());
			for (final Map.Entry e : m.entrySet())
			{
				final Object key = cloner.clone(tracker, e.getKey(), null);
				final Object value = cloner.clone(tracker, e.getValue(), null);
				result.put(key, value);
			}
			return result;
		}
	}
	
	
}
