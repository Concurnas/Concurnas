package com.concurnas.conc;

import java.nio.file.Path;

import com.concurnas.runtime.ConcurnasClassLoader;

public interface ClassLoaderProvider {
	public ConcurnasClassLoader apply(Path[] classes, Path[] bootstrap);
}
