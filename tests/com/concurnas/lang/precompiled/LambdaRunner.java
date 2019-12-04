package com.concurnas.lang.precompiled;

import java.util.List;
import java.util.stream.Collectors;

public class LambdaRunner {

	public static List<Integer> lambdaRunner(List<Integer> applyto) {
		return (List<Integer>) applyto.stream().map(a -> a+ 10).collect(Collectors.toList());
	}
}
