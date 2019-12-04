package com.concurnas.compiler.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import com.concurnas.compiler.visitors.Visitor;


//TODO: remove, this was not a great idea?
public class NamedTypeMany extends NamedType {
	//TODO: there are too many overriden methods here, make namedtype abstract, and FORCE def of all methods defined in namedType, so as to ensure many variant carries them through
	private final LinkedHashSet<NamedType> many;
	private final NamedType first;
	
	@Override
	public NamedTypeMany copyTypeSpecific()
	{
		LinkedHashSet<NamedType> copy = new LinkedHashSet<NamedType>();
		NamedType first1 = null;
		for(NamedType m : many)
		{
			NamedType cpy = (NamedType)m.copy();
			if(null == first1)
			{
				first1 = cpy;
			}
			copy.add(cpy);
		}
		return new NamedTypeMany(copy, first1);
	}
	
	public AbstractType getSelfOrElectChoice()
	{
		return this.first!=null? this.first: this;//Override this in order to choose from many different tpyes (NamedTypeMany in mind)
	}
	
	public NamedTypeMany(LinkedHashSet<NamedType> foundMatches) {
		this(0,0, foundMatches);
	}
	
	public String getNamedTypeStr()
	{
		return this.first.getNamedTypeStr();
	}
	
	public NamedTypeMany(int line, int col, LinkedHashSet<NamedType> foundMatches) {
		super(line, col);
		//assert !this.many.isEmpty();
		this.many = new LinkedHashSet<NamedType>(foundMatches);
		this.first = foundMatches.iterator().next();
	}
	
	private NamedTypeMany(LinkedHashSet<NamedType> foundMatches, NamedType first) {
		super(0, 0);
		this.many = foundMatches;
		this.first = first;
	}
	
	public HashSet<NamedType> getMany()
	{
		return this.many;
	}
	
	@Override
	public Object accept(Visitor visitor) {
		visitor.setLastLineVisited(super.getLine());
		return visitor.visit(first);
	}

	@Override
	public String getPrettyName() {
		return first.getPrettyName();
	}
	
	public ArrayList<Type> getGenericTypeElements()
	{
		return first.getGenericTypeElements();
	}
	
	public void setClassDef(ClassDef classDef)
	{
		first.setClassDef(classDef);
	}
	
	public ClassDef getSetClassDef()
	{
		return this.first.getSetClassDef();
	}

	@Override
	public void setArrayLevels(int levels) {
		super.setArrayLevels(levels);
		first.setArrayLevels(levels);
		for(NamedType m : this.many)
		{
			m.setArrayLevels(levels);
		}
	}

	public void setGenTypes(ArrayList<Type> genTypes)
	{
		this.genTypes = genTypes;
		this.first.setGenTypes(this.genTypes);
	}
	
	@Override
	public int getArrayLevels() {
		return first.getArrayLevels();
	}

	@Override
	public boolean hasArrayLevels() {
		return first.hasArrayLevels();
	}
	
	@Override
	public boolean equals(Object o)
	{
		return first.equals(o);
	}
	
	 @Override
    public int hashCode() {
		return first.hashCode();
    }
	 @Override
	 public  String getBytecodeTypeWithoutArray( )
	 {
		 return first.getBytecodeTypeWithoutArray();
	 }
	 @Override
	 public  String getGenericBytecodeTypeWithoutArray()
	 {
		 return first.getGenericBytecodeTypeWithoutArray();
	 }

	@Override
	public HashMap<GenericType, Type> getFromClassGenericToQualifiedType() {
		return this.first.getFromClassGenericToQualifiedType();
	}

	@Override
	public void setFromClassGenericToQualifiedType(HashMap<GenericType, Type> hashMap) {
		if(many != null){
			for(NamedType m: many){
				m.setFromClassGenericToQualifiedType( hashMap);
			}
		}
	}
	 
	
	@Override
	public String getCheckCastType() {
		return this.first.getCheckCastType();
	}
	
	@Override
	public void setOrigonalGenericTypeUpperBound(NamedType origonalGenericTypeUpperBound) {
		super.setOrigonalGenericTypeUpperBound(origonalGenericTypeUpperBound);
		if(many != null){
			for(NamedType m: many){
				m.setOrigonalGenericTypeUpperBound( origonalGenericTypeUpperBound);
			}
		}
	}
	
	@Override
	public NamedType getOrigonalGenericTypeUpperBound() {
		return first.getOrigonalGenericTypeUpperBound();
	}
	
	
	@Override
	public String getNonGenericPrettyName()
	{
		return this.first.getNonGenericPrettyName();
	}
	
	@Override
	public Type getSelf(){
		return first;
	}
	
	@Override
	public NamedType getResolvedSuperTypeAsNamed()
	{
		return this.first.getResolvedSuperTypeAsNamed();
	}
	//TODO: this sucks totally, you need to make everything direct to first automatically, omg!
	@Override
	public boolean isInterface(){
		return this.first.isInterface();
	}
	
	public boolean isGeneric(){
		return this.first.isGeneric();
	}
	
	@Override
	public boolean getIsRef(){
		return this.first.getIsRef();
	}
	
	@Override
	public void setIsRef(boolean b) {
		if(many != null){
			for(NamedType m: many){
				m.setIsRef( b);
			}
		}
	}
	
	public boolean getLockedAsRef(){
		return this.first.getLockedAsRef();
	}
	
	public void setLockedAsRef(boolean b) {
		if(many != null){
			for(NamedType m: many){
				m.setLockedAsRef( b);
			}
		}
	}
	
	@Override
	public boolean hasGenTypes(){
		return this.first.hasGenTypes();
	}
	
	@Override
	public List<Type> getGenTypes(){
		return this.first.genTypes;
	}
	
	@Override
	public int getArrayLevelsRefOVerride() {
		return this.first.getArrayLevelsRefOVerride();
	}

	@Override
	public void setLine(int line){
		first.setLine(line);
		for(NamedType m : this.many){
			m.setLine(line);
		}
	}
	
	@Override
	public void setColumn(int col){
		first.setColumn(col);
		for(NamedType m : this.many){
			m.setColumn(col);
		}
	}
	
	@Override
	public void overrideRefType(Type refee){
		first.genTypes.set(0, refee);
		for(NamedType m : this.many){
			m.overrideRefType(refee);
		}
	}
	

	@Override
	public boolean getIsTypeInFuncref() {
		return this.first.getIsTypeInFuncref();
	}

	@Override
	public NullStatus getNullStatus() {
		return this.first.getNullStatus();

	}

	@Override
	public void setNullStatus(NullStatus nullStatus) {
		for (NamedType m: many)
		{
			m.setNullStatus(nullStatus);
		}
	}
	
}



