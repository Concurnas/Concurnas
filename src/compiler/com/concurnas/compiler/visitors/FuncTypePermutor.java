package com.concurnas.compiler.visitors;

import java.util.ArrayList;
import java.util.List;

import com.concurnas.compiler.ast.Type;

public class FuncTypePermutor {
	public static List<ArrayList<Type>> getTheFunctTypePermutations(List<List<Type>> manyParams)
	{
		List<ArrayList<Type>> results = new ArrayList<ArrayList<Type>>(manyParams.get(0).size());
		
		/* A,B,C | A,B,C
		 * 
		 * A, A
		 * A, B
		 * A, C
		 * B, A
		 * B, B
		 * B, C
		 * C, A
		 * C, B
		 * C, C
		 * 
		 */
		getTheFunctTypePermutations(manyParams, 0, new ArrayList<Type>(), results);
		return results;
	}
	
	private static void getTheFunctTypePermutations(List<List<Type>> manyParams, int n, List<Type> currentLine, List<ArrayList<Type>> results)
	{
		List<Type> stuff = manyParams.get(n);
		
		for(Type t : stuff)
		{
			ArrayList<Type> thisLine = cloneA(currentLine);
			thisLine.add(t);
			if(n == manyParams.size()-1)
			{
				//terminal case
				results.add(thisLine);
			}
			else
			{
				//iterative case...
				getTheFunctTypePermutations(manyParams, n+1, thisLine, results);
			}
		}
	}
	
	private static ArrayList<Type> cloneA(List<Type> input)
	{
		ArrayList<Type> ret = new ArrayList<Type>(input.size());
		
		for(Type t: input)
		{
			ret.add(t);
		}
		
		return ret;
		
	}
}
