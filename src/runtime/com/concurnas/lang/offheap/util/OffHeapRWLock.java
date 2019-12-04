/*
 * Copyright 2015 Odnoklassniki Ltd, Mail.Ru Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Modifications copyright (C) 2017 Concurnas
 */

package com.concurnas.lang.offheap.util;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OffHeapRWLock  {
	private final ReentrantReadWriteLock lock;
	
	/*public RWLock() {
		lock = new ReentrantReadWriteLock();
	}*/
	public OffHeapRWLock(boolean useFairLockingStrategy) {
		lock = new ReentrantReadWriteLock(useFairLockingStrategy);
	}
	
	public final OffHeapRWLock lockRead() {
		lock.readLock().lock();
		return this;
	}
	
	
	public final void unlockRead() {
		lock.readLock().unlock();
	}
	
	public final OffHeapRWLock lockWrite() {
		lock.writeLock().lock();
		return this;
	}
	
	public final void unlockWrite() {
		lock.writeLock().unlock();
	}
}
