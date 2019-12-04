package com.concurnas.compiler.visitors.algos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.concurnas.runtime.Pair;

/**
 * e.g. input list of: [1,2,3,4]
 *
 * [[1, 2, 3, 4]] [[1], [2, 3, 4]] [[1, 2], [3, 4]] [[1, 2, 3], [4]] [[1], [2],
 * [3, 4]] [[1], [2, 3], [4]] [[1, 2], [3], [4]]
 * 
 * 
 * or: [0,1], 1 => [0], [0,1]
 * 
 * [] are returned as ranges so as to avoid copying of expensive expressions by caller
 *
 */
public class ListSegemtizer {

	/**
	 * ListSegemtizer.sublistCombs(3, 2)
	 * => [[(0, 0), (1, 1)], [(0, 0), (1, 2)], [(0, 1), (2, 2)]]
	 */
	public static ArrayList<ArrayList<Pair<Integer, Integer>>> sublistCombs(int size, int slots){
		ArrayList<ArrayList<Pair<Integer, Integer>>>  restults = new ArrayList<ArrayList<Pair<Integer, Integer>>>();
		ArrayList<Pair<Integer, Integer>> inst = new ArrayList<Pair<Integer, Integer>>();
		sublistCombs(size, slots, 0, restults, inst);
		return restults;
	}
	
	private static void sublistCombs(int size, int slots, int offset, ArrayList<ArrayList<Pair<Integer, Integer>>> restults, ArrayList<Pair<Integer, Integer>> inst){
		for(int n=offset; n < size; n++){
			//System.err.println(String.format("{%s}, {%s}", offset, n));
			inst.add(new Pair<>(offset, n));
			
			int sz = inst.size();
			if(sz == slots){
				restults.add(new ArrayList<Pair<Integer, Integer>>(inst));
			}else if(n < size){
				sublistCombs(size, slots, n+1,  restults,  inst);
			}
			
			inst.remove(sz-1);
		}
	}
	
	
	private final static ArrayList<ArrayList<Pair<Integer, Integer>>>[] segCache = new ArrayList[20];
	
	public static ArrayList<ArrayList<Pair<Integer, Integer>>> getSegments(int size) {
		ArrayList<ArrayList<Pair<Integer, Integer>>> ret;
		boolean toCahce = size <= 20;
		if(toCahce){
			ArrayList<ArrayList<Pair<Integer, Integer>>> got = segCache[size-1];
			if(null == got){
				got = gennerateSegments(size);
				segCache[size-1] = got;
			}
			return got;
		}else{
			return gennerateSegments(size);
		}
		
	}
	
	private static ArrayList<ArrayList<Pair<Integer, Integer>>> gennerateSegments(int size){
		ArrayList<ArrayList<Pair<Integer, Integer>>> ret = new ArrayList<ArrayList<Pair<Integer, Integer>>>();
		//add individual
		ArrayList<Pair<Integer, Integer>> indiv = new ArrayList<Pair<Integer, Integer>>();
		indiv.add(new Pair<Integer, Integer>(0, size-1));
		ret.add(indiv);
		
		for(int nn=2; nn <= size; nn++){
			ret.addAll(generate(size, nn));
		}
		return ret;
	}
	
	private static ArrayList<ArrayList<Pair<Integer, Integer>>> generate(int size, int subcut){
		ArrayList<ArrayList<Pair<Integer, Integer>>> ret = new ArrayList<ArrayList<Pair<Integer, Integer>>>();
		
		ArrayList<ArrayList<Integer>> combs =combinations(size-1, subcut-1); 
		
		for(ArrayList<Integer> comb : combs){
			ret.add(cut(comb, size));
		}
		
		return ret;
	}

	private static ArrayList<Pair<Integer, Integer>> cut(ArrayList<Integer> idxes, int size){
		int last = 0;
		ArrayList<Pair<Integer, Integer>> phase = new ArrayList<Pair<Integer, Integer>>();
		for(Integer i : idxes){
			phase.add(new Pair<Integer, Integer>(last, i-1));
			last = i;
		}
		phase.add(new Pair<Integer, Integer>(last, size-1));
		
		return phase;
	}
	
	private static ArrayList<ArrayList<Integer>> combinations(int n, int k) {
		ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
	 
		if (n <= 0 || n < k){
			return result;
		}
	 
		ArrayList<Integer> item = new ArrayList<Integer>();
		dfs(n, k, 1, item, result); 
	 
		return result;
	}
	 
	private static void dfs(int n, int k, int start, ArrayList<Integer> item, ArrayList<ArrayList<Integer>> res) {
		if (item.size() == k) {
			res.add(new ArrayList<Integer>(item));
			return;
		}

		for (int i = start; i <= n; i++) {
			item.add(i);
			dfs(n, k, i + 1, item, res);
			item.remove(item.size() - 1);
		}
	}
	
	public static <T> ArrayList<ArrayList<T>> getCombinations(ArrayList<ArrayList<T>> lists) {
		ArrayList<ArrayList<T>> combinations = new ArrayList<ArrayList<T>>();
		ArrayList<ArrayList<T>> newCombinations;

		
		
		int index = 0;

		for (T i : lists.get(0)) {
			ArrayList<T> newList = new ArrayList<T>();
			newList.add(i);
			combinations.add(newList);
		}
		index++;
		while (index < lists.size()) {
			List<T> nextList = lists.get(index);
			newCombinations = new ArrayList<ArrayList<T>>();
			for (List<T> first : combinations) {
				for (T second : nextList) {
					ArrayList<T> newList = new ArrayList<T>();
					newList.addAll(first);
					newList.add(second);
					newCombinations.add(newList);
				}
			}
			combinations = newCombinations;

			index++;
		}

		return combinations;
	}
	
}
