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
package org.apache.ibatis.reflection.factory;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.ibatis.reflection.ReflectionException;
import org.apache.ibatis.reflection.Reflector;

/**
 * @author Clinton Begin
 */
// 默认的 ObjectFactory 实现，实现了 ObjectFactory 和 Serializable
public class DefaultObjectFactory implements ObjectFactory, Serializable {

	private static final long serialVersionUID = -8855120656740914948L;
	
	/**
	 * 通过无参构造方法创建对象
	 */
	@Override
	public <T> T create(Class<T> type) {
		return create(type, null, null);
	}

	/**
	 * 通过指定的构造函数创建对象
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
		// 获取创建需要的类，TODO
		Class<?> classToCreate = resolveInterface(type);
		// we know types are assignable
		// 创建指定 的类对象
		return (T) instantiateClass(classToCreate, constructorArgTypes, constructorArgs);
	}

	// 空方法
	@Override
	public void setProperties(Properties properties) {
		// no props for default
	}

	/**
	 * 
	 * <B>方法名称：</B> instantiateClass <BR>
	 * <B>概要说明：</B> 创建指定类的对象 <BR>
	 * 
	 * @author 陈康明
	 * 
	 * @since 2020年06月14 18:18
	 * @return
	 * @return T
	 */
	private <T> T instantiateClass(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
		try {
			Constructor<T> constructor;
			// 没有参数， 通过默认的无参构造方法创建指定的对象
			if (constructorArgTypes == null || constructorArgs == null) {
				constructor = type.getDeclaredConstructor();
				try {
					return constructor.newInstance();
				} catch (IllegalAccessException e) {
					if (Reflector.canControlMemberAccessible()) {
						constructor.setAccessible(true);
						return constructor.newInstance();
					} else {
						throw e;
					}
				}
			}
			// 通过特定的构造方法，创建指定类的对象。
			constructor = type
					.getDeclaredConstructor(constructorArgTypes.toArray(new Class[constructorArgTypes.size()]));
			try {
				return constructor.newInstance(constructorArgs.toArray(new Object[constructorArgs.size()]));
			} catch (IllegalAccessException e) {
				if (Reflector.canControlMemberAccessible()) {
					constructor.setAccessible(true);
					return constructor.newInstance(constructorArgs.toArray(new Object[constructorArgs.size()]));
				} else {
					throw e;
				}
			}
		} catch (Exception e) {
			// 拼接 参数类型
			StringBuilder argTypes = new StringBuilder();
			if (constructorArgTypes != null && !constructorArgTypes.isEmpty()) {
				for (Class<?> argType : constructorArgTypes) {
					argTypes.append(argType.getSimpleName());
					argTypes.append(",");
				}
				argTypes.deleteCharAt(argTypes.length() - 1); // remove trailing ,
			}
			// 拼接 参数值
			StringBuilder argValues = new StringBuilder();
			if (constructorArgs != null && !constructorArgs.isEmpty()) {
				for (Object argValue : constructorArgs) {
					argValues.append(String.valueOf(argValue));
					argValues.append(",");
				}
				argValues.deleteCharAt(argValues.length() - 1); // remove trailing ,
			}
			// 抛出 ReflectionException 异常，说明情况。
			throw new ReflectionException("Error instantiating " + type + " with invalid types (" + argTypes
					+ ") or values (" + argValues + "). Cause: " + e, e);
		}
	}
	// 对集合类型做特殊处理，TODO 不太理解这四种类型
	protected Class<?> resolveInterface(Class<?> type) {
		Class<?> classToCreate;
		// List Collection Iterable 三种类型
		if (type == List.class || type == Collection.class || type == Iterable.class) {
			classToCreate = ArrayList.class;
			// map 类型的处理
		} else if (type == Map.class) {
			classToCreate = HashMap.class;
			// Sortedset 类型的处理
		} else if (type == SortedSet.class) { // issue #510 Collections Support
			classToCreate = TreeSet.class;
			// Set
		} else if (type == Set.class) {
			classToCreate = HashSet.class;
		} else {
			// 原本的类型
			classToCreate = type;
		}
		return classToCreate;
	}
	
	/**
	 * 判断 是否是  Collection 类型
	 */
	@Override
	public <T> boolean isCollection(Class<T> type) {
		return Collection.class.isAssignableFrom(type);
	}

}
