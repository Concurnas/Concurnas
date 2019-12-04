package com.concurnas.compiler.utils;

import java.util.ArrayList;
import java.util.Comparator;

import com.concurnas.runtime.Pair;

public class Profiler {
	private double start = System.currentTimeMillis();
	private double lastEvent = start;
	private String name;
	private ArrayList<Pair<String, Double>> events = new  ArrayList<Pair<String, Double>>();
	
	public Profiler(String name) {
		this.name = name;
	}
	
	public void mark(String event) {
		double ctime = System.currentTimeMillis();

		double parseTime = (System.currentTimeMillis() - lastEvent)/1000.;
		events.add(new Pair<String, Double>(String.format("%s: %s:", name, event), parseTime));
		lastEvent = ctime;
	}

	public void end() {
		double parseTime = (System.currentTimeMillis() - start)/1000.;
		events.add(new Pair<String, Double>(String.format("%s: Total Time: ", name), parseTime));
	}
	
	public void printEvents() {
		int longest = events.stream().map(a -> a.getA().length()).max(Comparator.naturalOrder()).get();
		String fmtString = "%-"+longest+"s";
		for(Pair<String, Double> strAndDur : events) {
			String padded = String.format(fmtString, strAndDur.getA());
			
			String fmtrh = String.format("%11s", String.format("%.3fsec", strAndDur.getB()));
			System.out.println(String.format("%s%s", padded, fmtrh));
		}
	}//%.3fsec
}
