package com.concurnas.compiler.utils;

import java.util.ArrayList;
import java.util.HashMap;

public class StringTrie<T> {

	private final String separator;

	private class Node{
		private HashMap<String, Node> kids = new HashMap<String, Node>();
		public T mapsTo = null;
		
		/*@Override
		public String toString() {
			if(mapsTo != null) {
				return "!";
			}else {
				StringBuilder sb = new StringBuilder();
				
				for(String item : kids.keySet()) {
					"." + kids.get(item).toString()
				}
				
				
				return sb.toString();
			}
			
		}*/
	}
	
	private HashMap<String, Node> roots = new HashMap<String, Node>();
	
	public StringTrie(String separator) {
		this.separator = separator;
	}
	
	public void add(String packageAndName, T mapsTo) {
		String[] elements = packageAndName.split(separator);
		
		Node curNode= null;
		
		for(int n=0; n < elements.length; n++) {
			String element = elements[n];
			HashMap<String, Node> nmap = curNode == null?roots:curNode.kids;
			
			if(nmap.containsKey(element)) {
				curNode = nmap.get(element);
			}else {
				curNode = new Node();
				nmap.put(element, curNode);
			}
			
			if(n == elements.length -1) {
				curNode.mapsTo = mapsTo;
			}
		}
	}
	public T get(String packageAndName) {
		String[] elements = packageAndName.split(separator);
		
		Node curNode= null;
		
		for(int n=0; n < elements.length; n++) {
			String element = elements[n];
			HashMap<String, Node> nmap = curNode == null?roots:curNode.kids;
			if(nmap.containsKey(element)) {
				curNode = nmap.get(element);
			}else {
				break;
			}
			
			if(null != curNode.mapsTo) {
				return curNode.mapsTo;
			}
		}
		
		return null;
	}
	
	private void dfs(String prefix, ArrayList<String> res, Node node) {
		if(node.mapsTo != null) {
			res.add(prefix + ".!");
		}
		
		for(String name : node.kids.keySet()) {
			Node nextNode = node.kids.get(name);
			dfs(prefix + "." + name, res, nextNode);
		}
	}
	
	@Override
	public String toString() {
		ArrayList<String> res = new ArrayList<String>();
		for(String name : roots.keySet()) {
			Node node = roots.get(name);
			dfs(name, res, node);
		}
		
		return String.join(",", res);
	}
}
