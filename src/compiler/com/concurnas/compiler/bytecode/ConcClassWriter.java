package com.concurnas.compiler.bytecode;

import java.util.ArrayList;
import java.util.HashMap;

import org.objectweb.asm.ClassWriter;

import com.concurnas.compiler.ast.ClassDef;
import com.concurnas.compiler.ast.ClassDefJava;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.util.ErrorRaiseableSupressErrors;

public class ConcClassWriter extends ClassWriter{
    //public ConcClassWriter(final int flags, GCSCResolver gCSCResolver) {
	private final HashMap<String, ClassDef> typeDirectory;
	private final ErrorRaiseableSupressErrors rsup = new ErrorRaiseableSupressErrors(null);
	
    public ConcClassWriter(final int flags, final HashMap<String, ClassDef> typeDirectory) {
    	super(flags);
    	this.typeDirectory=typeDirectory;//TODO:is this the best way?
    }
    
    private NamedType getNamedType(String type) throws ClassNotFoundException{
    	String name = type.replace("$", ".").replace("/", ".").replace("..", ".$");
    	String name2 = type.replace("/", ".");
    	//System.err.println(String.format("type: %s -> %s vs %s", type, name, name2));
    	ClassDef cd = typeDirectory.get(name);//TODO: hack, what if you have a $ in a name?
    	
    	if(null == cd){
    		//TODO: seems to have problems on initial bytecode genneration
    		cd = this.typeDirectory.get(name2);
    		if(null == cd) {
    			cd = new ClassDefJava(Class.forName(name2));
    		}
    		
    	}
    	
    	return new NamedType(cd);
    }
    
    protected String getCommonSuperClass(final String type1, final String type2)
    {
    	String onfail = type2;//more likely to be this type (as a subtype) if one is missing
    	try{
    		NamedType lhs = getNamedType(type1);
    		onfail = type1;
            NamedType rhs = getNamedType(type2);
            
            if (null != TypeCheckUtils.checkSubType(rsup, lhs, rhs, 42, 42, 42, 42)) {
                return type1;
            }
            if (null != TypeCheckUtils.checkSubType(rsup, rhs, lhs, 42, 42, 42, 42)) {
                return type2;
            }
            
            if (lhs.isInterface() || rhs.isInterface()) {
                return "java/lang/Object";
            } else {
            	ArrayList<Type> types = new ArrayList<Type>(2);
            	types.add(lhs);
            	types.add(rhs);
            	
            	Type ret = TypeCheckUtils.getMoreGenericDo(rsup, 42, 42, types, null, false);
            	ClassDef cd = ((NamedType)ret).getSetClassDef();
            		
                return cd.bcFullName();
            }
            //hmm, going to need to pass something in which can do this
            //both at compile time and also at runtime
            //classpath needs to be definable at compile time...
    	}
    	catch(Exception e){
    		if(type2.equals("java/lang/Object") || type1.equals("java/lang/Object")) {
    			return "java/lang/Object";//hack
    		}
    		
    		//throw new RuntimeException(e);
    		return onfail;
    	}
        
    }
    
    private static class WrittenClassLoader extends ClassLoader{
    	public Class<?> defineClass(String name, byte[] code){
    		return super.defineClass(name, code, 0, code.length);
    	}
    }
    
    private WrittenClassLoader wcl = new WrittenClassLoader();
    
    public byte[] toByteArray() {
    	byte[] ret = super.toByteArray();
    	
    	return ret;
    }
    
    public byte[] toByteArray(String name) {
    	byte[] ret = super.toByteArray();
    	
    	name = name.replace("/", ".");
    	try{
    		this.typeDirectory.put(name, new ClassDefJava(wcl.defineClass(name, ret)));
    	}catch(Throwable t) {
    		//TODO: class ordering can be a problem hence this exception can be thrown.
    	}
    	
    	return ret;
    }
    
}