package com.concurnas.compiler.visitors.algos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.concurnas.compiler.ast.FuncType;
import com.concurnas.compiler.ast.GenericType;
import com.concurnas.compiler.ast.NamedType;
import com.concurnas.compiler.ast.Type;
import com.concurnas.compiler.visitors.ErrorRaiseable;
import com.concurnas.compiler.visitors.ScopeAndTypeChecker;
import com.concurnas.compiler.visitors.TypeCheckUtils;
import com.concurnas.compiler.visitors.util.ErrorRaiseableSupressErrorsAndLogProblem;


public class GetMostSpecificAndTestAmbig {

	/*
	 * this class essentially solves the problem of ambigious type declarations
	 * e.g. fun abb(a String, b Object)
	 *      fun abb(a Object, b String)
	 *      
	 *      abb("", "") <- this would be ambigious
	 *
	 * 		
	 * /*to be ambigious there must be two type islands
		//e.g. (String, Object) | (Object, String) <- two type islands
		//but (Object, Object) <- (String, String) <- guys are related
		//but (Object, Object) <- (String, String) |  (String, Object) < would be a direct match on string
		//class A, class B < A, class C < B:
		//(A, B) | (B, A) | (A,B), (B, A) <- (B,B)  <- this is a problem
		// 1 | 2 | 1,2 <- 3
		 mark 1 and 2 as being problematic as they conflict, 3 is ok as child of both, so when lowest thingy chcker chooses 3 its not a problem
		
		
		//now we build a graph
		
		//here int is not considered subtype of Integer etc
		/*
		 * algo:
		 * 1.build parent table graph and track root nodes
		 * 2. traverse the graph look and report all dudes with no kids as end points
		 * 3. if 1 end point -> return, if more than one fail because ambigious
	*/
	 
	
	private static class Node
	{
		public FuncType func;
		public List<Type> args;
		public ArrayList<Node> children = new ArrayList<Node>();

		public Node(FuncType func, List<Type> args)
		{
			this.func = func;
			this.args = args;
		}
		
	}
	
	public static FuncType go(ErrorRaiseable invoker, ArrayList<FuncType> choices, ArrayList<Boolean> prioritizeRef)
	{
		ArrayList<Node> nodes = new ArrayList<Node>();
		
		for(FuncType fun : choices)
		{
			//List<Type> args = fun.hackCalledArgumets!=null?fun.hackCalledArgumets:fun.getInputs();
			List<Type> args = fun.getInputs();
			nodes.add( new Node(fun, args) );
		}
		
		for(int n = 0; n < nodes.size(); n++)
		{
			Node toCheck = nodes.get(n);
			for(int m = 0; m < nodes.size(); m++)
			{//vs all others
				if(m == n)
					continue;
				
				Node potentialParent = nodes.get(m);
				
				ArrayList<Boolean> prioritizeRefForInstance;
				if(toCheck.func.hackCalledArgumets != null){
					prioritizeRefForInstance = new ArrayList<Boolean>(toCheck.func.hackCalledArgumets.size());
					for(Type arg: toCheck.func.hackCalledArgumets){
						prioritizeRefForInstance.add(TypeCheckUtils.hasRefLevelsAndIsLocked(arg));
					}
				}
				else{
					prioritizeRefForInstance = prioritizeRef;
				}
				
				if(checkAllArgsSubType(invoker, potentialParent.args, toCheck.args, prioritizeRefForInstance))
				{//all arguments must be equal to or subtypes of the argument
					potentialParent.children.add(toCheck);
				}
			}
			
		}
		
		Set<Node> endpoints = new HashSet<Node>();
		
		for(Node node : nodes)
		{
			if(node.children.isEmpty())
			{
				endpoints.add(node);
			}
		}
		
		if(endpoints.size() != 1)
		{//must be more than one or empty(?-how can be empty?)
			return null;
		}
		else
		{//but Object is parent of Object: | unlesss arg is Object: ref - then thats what u want only
			FuncType fr = endpoints.iterator().next().func; 
			return fr;
		}
	}
	
	private static boolean checkAllArgsSubType(ErrorRaiseable invoker, List<Type> parents, List<Type> kids, List<Boolean> prioritizeRefs)
	{
	/*	if(parents.size() != kids.size()){
			return false;
		}*/
		
		ErrorRaiseableSupressErrorsAndLogProblem er = new ErrorRaiseableSupressErrorsAndLogProblem(invoker);
		
		for(int n = 0; n < parents.size(); n++ )
		{
			Type lhs = parents.get(n);
			
			if(lhs instanceof GenericType) {
				GenericType asg = (GenericType)lhs;
				if(asg.upperBound != null) {
					lhs = asg.upperBound;
				}
					
			}
			
			Type rhs = kids.get(n);
			boolean prioritizeRef = prioritizeRefs.get(n);
			
			/*
			 * next block avoid ambigioty check being triggered in the following case:
			 * fun xxx(a Object){ }
				fun xxx(a :){ } //these are not ambigious
				xxx( "" )
				
				~~~~~~~~~~~~~~~~
				oh, but these are:
				fun xxx(a String, b String:){ }
				fun xxx(a String:, b String ){ }
				
					xxx("", "")
			 */
			
			if(prioritizeRef){
				if(TypeCheckUtils.hasRefLevelsAndNotLocked(lhs)){
					NamedType asNamed = ((NamedType)lhs).copyTypeSpecific();
					asNamed.setLockedAsRef(true);
					lhs = asNamed;
				}
				
				if(TypeCheckUtils.hasRefLevels(rhs)){
					rhs = TypeCheckUtils.getRefType(rhs);
				}
			}
			else{
				if(lhs.equals(ScopeAndTypeChecker.const_object) && rhs.equals(ScopeAndTypeChecker.const_object_ref)){
					//special case to deal with ambiguity concerning Object vs Object: (because Object is the supertype of Object:) - however, in this special case we force the child type to be Object - i.e. we force the
					//calling of the non ref variant
					return false;
				}
				
				
				if(TypeCheckUtils.hasRefLevelsAndNotLocked(rhs)){
					NamedType asNamed = ((NamedType)rhs).copyTypeSpecific();
					asNamed.setLockedAsRef(true);
					rhs = asNamed;
				}
				
				if(TypeCheckUtils.hasRefLevels(lhs)){
					lhs = TypeCheckUtils.getRefType(lhs);
				}
			}
			Type got = TypeCheckUtils.checkSubType(er, lhs, rhs, true, 0, 0, 0, 0);
			
			if(er.isHasErrored() || got == null)//if err then not subtype
				return false;
		}
		return true;
	}
	
}
