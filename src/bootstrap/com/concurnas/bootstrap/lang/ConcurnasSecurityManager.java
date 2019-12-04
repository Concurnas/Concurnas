package com.concurnas.bootstrap.lang;

import java.security.AllPermission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.WeakHashMap;

import com.concurnas.bootstrap.runtime.cps.Fiber;


public class ConcurnasSecurityManager extends Policy {
	private static final PermissionCollection defaultPermissions = new Permissions();
	static {
		defaultPermissions.add(new AllPermission());//all permissions
	}
	
	private ConcurnasSecurityManager() {
	}
	
	private static final ConcurnasSecurityManager singleInstance;
	
	static {
		singleInstance = new ConcurnasSecurityManager();
		Policy.setPolicy(singleInstance);
	 	System.setSecurityManager(new SecurityManager());  
	}
	
	public static ConcurnasSecurityManager getInstance(Fiber fib) {
		return getInstance();
	}
	public static ConcurnasSecurityManager getInstance() {
		return singleInstance;
	}
	
	public PermissionCollection getPermissions(ProtectionDomain domain, Fiber fib) {
		return getPermissions(domain);
	}
	public PermissionCollection getPermissions(ProtectionDomain domain) {
		ClassLoader cl = domain.getClassLoader();

		PermissionCollection toApply = classLoaderToPermissions.get(cl);
		
		if(null == toApply) {
			toApply = defaultPermissions;
		}
		return toApply;
	}

	private WeakHashMap<ClassLoader, PermissionCollection> classLoaderToPermissions = new WeakHashMap<ClassLoader, PermissionCollection>();
	public void registerClassloader(ClassLoader cl, PermissionCollection permissions, Fiber fib) {
		registerClassloader( cl,  permissions);
	}
	public void registerClassloader(ClassLoader cl, PermissionCollection permissions) {
		classLoaderToPermissions.put(cl, permissions);
	}
}
