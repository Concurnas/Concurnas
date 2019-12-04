package com.concurnas.lang.precompiled;

import java.util.ArrayList;
import java.util.List;

public class ListMaker {
	public static List<Double> doubleList(double... items) {
		List<Double> ret = new ArrayList<Double>();
		for(double d : items) {
			ret.add(d);
		}
		return ret;
	}
	
	public static List<Integer> intList(int... items) {
		List<Integer> ret = new ArrayList<Integer>();
		for(int d : items) {
			ret.add(d);
		}
		return ret;
	}
	
	public static List<Boolean> booleanList(boolean... items) {
		List<Boolean> ret = new ArrayList<Boolean>();
		for(boolean d : items) {
			ret.add(d);
		}
		return ret;
	}
	
	public static List<List<Integer>> intMatrix(List<Integer>... items) {
		List<List<Integer>> ret = new ArrayList<List<Integer>>();
		for(List<Integer> d : items) {
			ret.add(d);
		}
		return ret;
	}
	
	public static List<List<Double>> doubleMatrix(List<Double>... items) {
		List<List<Double>> ret = new ArrayList<List<Double>>();
		for(List<Double> d : items) {
			ret.add(d);
		}
		return ret;
	}
	public static List<List<List<Double>>> tripMatrix(List<List<Double>>... items) {
		List<List<List<Double>>> ret = new ArrayList<List<List<Double>>>();
		for(List<List<Double>> d : items) {
			ret.add(d);
		}
		return ret;
	}
	
	public static List<String> stringList(String... items) {
		List<String> ret = new ArrayList<String>();
		for(String d : items) {
			ret.add(d);
		}
		return ret;
	}
	
	
	
	
	public static List<Object> objectList(Object... items) {
		List<Object> ret = new ArrayList<Object>();
		for(Object d : items) {
			ret.add(d);
		}
		return ret;
	}
	
	public static List<List<Object>> objectMatrix(List<Object>... items) {
		List<List<Object>> ret = new ArrayList<List<Object>>();
		for(List<Object> d : items) {
			ret.add(d);
		}
		return ret;
	}
	
}
