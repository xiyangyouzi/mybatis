/**
 *    Copyright ${license.git.copyrightYears} the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.reflection;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 
 * DefaultReflectorFactory
 * 
 * @author 陈康明
 * @since 2020年06月14日 16:16
 */
public class DefaultReflectorFactory implements ReflectorFactory {
	// 开启缓存
	private boolean classCacheEnabled = true;
	// reflectorMap 缓存映射，多线程安全
	private final ConcurrentMap<Class<?>, Reflector> reflectorMap = new ConcurrentHashMap<>();
	// 默认的构造方法
	public DefaultReflectorFactory() {
	}
	
	@Override
	public boolean isClassCacheEnabled() {
		return classCacheEnabled;
	}

	// 设置 开启缓存
	@Override
	public void setClassCacheEnabled(boolean classCacheEnabled) {
		this.classCacheEnabled = classCacheEnabled;
	}

	@Override
	public Reflector findForClass(Class<?> type) {
		// 开启缓存，则从 reflectorMap 中获取
		if (classCacheEnabled) {
			// synchronized (type) removed see issue #461
			// 先从缓存里面取，第一次没有就 通过 Type创建一个，存放到 map 里面去
			return reflectorMap.computeIfAbsent(type, Reflector::new);
		} else {
			return new Reflector(type);
		}
	}

}
