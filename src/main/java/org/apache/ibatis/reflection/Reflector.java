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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.ReflectPermission;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ibatis.reflection.invoker.GetFieldInvoker;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.reflection.invoker.MethodInvoker;
import org.apache.ibatis.reflection.invoker.SetFieldInvoker;
import org.apache.ibatis.reflection.property.PropertyNamer;

/**
 * This class represents a cached set of class definition information that
 * allows for easy mapping between property names and getter/setter methods. 每个
 * Reflector 对应一个类。Reflector 会缓存反射操作需要的类的信息，例如：构造方法、属性名、setting / getting 方法
 * 
 * 进度：主要方法，也就是大部分方法都看完了，后面还要再次学习一下。
 * @author Clinton Begin
 */
public class Reflector {

	// 对应类字节码
	private final Class<?> type;
	// 可读属性数组
	private final String[] readablePropertyNames;
	// 可写属性数组
	private final String[] writeablePropertyNames;
	// set方法 key是 属性名，value是对应的invoke对象
	private final Map<String, Invoker> setMethods = new HashMap<>();
	// get方法 key是 属性名，value是对应的invoke对象
	private final Map<String, Invoker> getMethods = new HashMap<>();
	// set方法的参数类型 key是属性名，value是对应的 字节码对象
	private final Map<String, Class<?>> setTypes = new HashMap<>();
	// get方法的返回值的类型 key是属性名，value是对应的 字节码对象
	private final Map<String, Class<?>> getTypes = new HashMap<>();
	// 默认的构造方法
	private Constructor<?> defaultConstructor;
	// 不区分大小写的属性
	private Map<String, String> caseInsensitivePropertyMap = new HashMap<>();

	// 构造方法
	public Reflector(Class<?> clazz) {
		// 被反射对应的类
		type = clazz;
		// 对应类的的默认构造方法
		addDefaultConstructor(clazz);
		// 获取所有的 get 方法
		addGetMethods(clazz);
		// 获取所有的 set 方法
		addSetMethods(clazz);
		// 获取所有的字段
		addFields(clazz);
		// 初始化 readablePropertyNames、writeablePropertyNames、caseInsensitivePropertyMap
		// 三个属性
		// getMethods 里面的key转换成 readablePropertyNames //TODO 为什么要这样，看什么地方用
		readablePropertyNames = getMethods.keySet().toArray(new String[getMethods.keySet().size()]);
		// setMethods 里面的key转换成 writeablePropertyNames //TODO 为什么要这样，看什么地方用
		writeablePropertyNames = setMethods.keySet().toArray(new String[setMethods.keySet().size()]);
		// 可读可写的属性全部转换成大写，并且存入到 caseInsensitivePropertyMap 里面。 //TODO 为什么要这样，看什么地方用
		for (String propName : readablePropertyNames) {
			caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
		}
		for (String propName : writeablePropertyNames) {
			caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
		}
	}

	// 对应类的的默认构造方法
	private void addDefaultConstructor(Class<?> clazz) {
		// 获取所有非私有的构造方法
		Constructor<?>[] consts = clazz.getDeclaredConstructors();
		for (Constructor<?> constructor : consts) {
			// 参数个数是 0 的构造方法是默认构造方法
			if (constructor.getParameterTypes().length == 0) {
				this.defaultConstructor = constructor;
			}
		}
	}

	// 初始化 getMethods 和 getTypes
	private void addGetMethods(Class<?> cls) {
		// 属性对应的 get方法。List<Method> 可能父子类都有getting方法
		Map<String, List<Method>> conflictingGetters = new HashMap<>();
		// 获取 类 所有的方法
		Method[] methods = getClassMethods(cls);
		// 遍历
		for (Method method : methods) {
			// 排除参数大于 0 的，为什么？参数大于0 说明不是getting方法
			if (method.getParameterTypes().length > 0) {
				continue;
			}
			// 获取 方法名
			String name = method.getName();

			if ((name.startsWith("get") && name.length() > 3) || (name.startsWith("is") && name.length() > 2)) {
				// 截取到 属性 名
				name = PropertyNamer.methodToProperty(name);
				// 添加到 conflictingGetters 中
				addMethodConflict(conflictingGetters, name, method);
			}
		}
		// 解决 getter方法冲突
		resolveGetterConflicts(conflictingGetters);
	}

	/**
	 *  
	 * <B>方法名称：</B> resolveGetterConflicts <BR>
	 * <B>概要说明：</B> 解决 getter方法冲突 <BR>
	 * 
	 * 为什么会有相同的方法？
	 * 调用处，已经是 通过 方法签名，保证了子类重写父类方法，返回值相同的时候只会有子类的方法。
	 * 这里作用：子类的重写的返回值是和父类不一样，子类的返回值可以是父类的返回值的子类。
	 * 
	 * @author 陈康明
	 * 
	 * @since 2020年06月13 23:34
	 * @return
	 * @return void
	 */
	private void resolveGetterConflicts(Map<String, List<Method>> conflictingGetters) {
		for (Entry<String, List<Method>> entry : conflictingGetters.entrySet()) {
			Method winner = null;
			String propName = entry.getKey();
			for (Method candidate : entry.getValue()) {
				// winner 为空，说明 candidate 为最匹配的方法
				if (winner == null) {
					winner = candidate;
					continue;
				}
				// 基于返回值类型比较
				Class<?> winnerType = winner.getReturnType();
				Class<?> candidateType = candidate.getReturnType();
				// 类型相同
				if (candidateType.equals(winnerType)) {
					// 返回值类型 在 getClassMethods 就应该要匹配相同的，不添加父类进去，要报错。
					if (!boolean.class.equals(candidateType)) {
						throw new ReflectionException(
								"Illegal overloaded getter method with ambiguous type for property " + propName
										+ " in class " + winner.getDeclaringClass()
										+ ". This breaks the JavaBeans specification and can cause unpredictable results.");
					} else if (candidate.getName().startsWith("is")) {
						// TODO 这里不知道为什么 is 可以相同
						winner = candidate;
					}
				} else if (candidateType.isAssignableFrom(winnerType)) {
					// candidateType 是 winnerType 的父类，那么 就用 winnerType 子类，不用改变
					// OK getter type is descendant
				} else if (winnerType.isAssignableFrom(candidateType)) {
					// winnerType 是父类 ，要换成子类
					winner = candidate;
				} else {
					// 没有匹配出结果，那说明 重写有问题，抛出异常。
					throw new ReflectionException("Illegal overloaded getter method with ambiguous type for property "
							+ propName + " in class " + winner.getDeclaringClass()
							+ ". This breaks the JavaBeans specification and can cause unpredictable results.");
				}
			}
			// 添加到 getMethods 和 getTypes 中
			addGetMethod(propName, winner);
		}
	}
	// 添加到 getMethods 和 getTypes 中 TODO 方法里面的细节处理还要再思考一遍。
	private void addGetMethod(String name, Method method) {
		// 判断变量名是否合理
		if (isValidPropertyName(name)) {
			// 添加 到 getMethods 中
			getMethods.put(name, new MethodInvoker(method));
			// 获取返回的类型 getTypes 中
			Type returnType = TypeParameterResolver.resolveReturnType(method, type);
			// 添加到 getTypes 中
			getTypes.put(name, typeToClass(returnType));
		}
	}

	// 添加处理所有的 setting 方法
	private void addSetMethods(Class<?> cls) {
		// 属性 和 Setting 方法的映射
		Map<String, List<Method>> conflictingSetters = new HashMap<>();
		// 获取所有的 Method
		Method[] methods = getClassMethods(cls);
		for (Method method : methods) {
			// 获取方法名
			String name = method.getName();
			// set 开头，并且参数 只有 1 个 才是 setting 方法
			if (name.startsWith("set") && name.length() > 3) {
				if (method.getParameterTypes().length == 1) {
					// 获取属性名
					name = PropertyNamer.methodToProperty(name);
					// 属性和对应的method 添加到 conflictingSetters 里面
					addMethodConflict(conflictingSetters, name, method);
				}
			}
		}
		// 解决 setting 冲突方法。解决 子类重写父类，返回值不一样，选择子类重写的方法。
		resolveSetterConflicts(conflictingSetters);
	}

	// 添加到 conflictingGetters 中
	private void addMethodConflict(Map<String, List<Method>> conflictingMethods, String name, Method method) {
		// 有就直接取。没有，key对应的value为空，会将第二个参数的返回值存入并返回。
		List<Method> list = conflictingMethods.computeIfAbsent(name, k -> new ArrayList<>());
		list.add(method);
	}

	// 解决冲突方法，解决 子类重写父类，返回值不一样，选择子类重写的方法, setting 方法也可以有返回值，没有说不能有。
	private void resolveSetterConflicts(Map<String, List<Method>> conflictingSetters) {
		// 遍历每个属性，查找最适合的方法。
		for (String propName : conflictingSetters.keySet()) {
			// 获取 属性名
			List<Method> setters = conflictingSetters.get(propName);
			// 获取 属性 类型
			Class<?> getterType = getTypes.get(propName);
			Method match = null;
			ReflectionException exception = null;
			// 遍历 属性 对应的 setting 方法
			for (Method setter : setters) {
				Class<?> paramType = setter.getParameterTypes()[0];
				// 和 getterType相同，直接使用。TODO 不理解，这个时候父类和子类的参数都相同，如何确定的子类和父类一样？
				// 解释 和 实际的 getting 方法不太一样， 首先考虑的是和 getterType 的优先级最高, 没有才会到下面找一个最匹配的。
				if (paramType.equals(getterType)) {
					// should be the best match
					match = setter;
					break;
				}
				if (exception == null) {
					try {
						// 选择一个更加匹配的，这一步原理和 getting 方法获取才是相同的
						match = pickBetterSetter(match, setter, propName);
					} catch (ReflectionException e) {
						// there could still be the 'best match'
						match = null;
						exception = e;
					}
				}
			}
			// 添加到 setMethods 和 SetTypes 中
			if (match == null) {
				throw exception;
			} else {
				addSetMethod(propName, match);
			}
		}
	}
	
	// 找一个适合的 方法 TODO 为什么只有重载情况，没有返回值不一样的情况？
	private Method pickBetterSetter(Method setter1, Method setter2, String property) {
		// 进来 先给个默认匹配的
		if (setter1 == null) {
			return setter2;
		}
		Class<?> paramType1 = setter1.getParameterTypes()[0];
		Class<?> paramType2 = setter2.getParameterTypes()[0];
		// paramType1 是 paramType2 的父类，重载了，用范围小的。
		if (paramType1.isAssignableFrom(paramType2)) {
			return setter2;
			// 和上面一样的原理
		} else if (paramType2.isAssignableFrom(paramType1)) {
			return setter1;
		}
		throw new ReflectionException(
				"Ambiguous setters defined for property '" + property + "' in class '" + setter2.getDeclaringClass()
						+ "' with types '" + paramType1.getName() + "' and '" + paramType2.getName() + "'.");
	}

	private void addSetMethod(String name, Method method) {
		// 判断是否合法
		if (isValidPropertyName(name)) {
			// 添加到 setMethods 里面
			setMethods.put(name, new MethodInvoker(method));
			// 添加到 setTypes 中
			Type[] paramTypes = TypeParameterResolver.resolveParamTypes(method, type);
			// 添加到 Type 里面
			setTypes.put(name, typeToClass(paramTypes[0]));
		}
	}
	// Type 转换成 class 字节码对象，寻找真正的类 TODO Type 还要再学习理解
	private Class<?> typeToClass(Type src) {
		Class<?> result = null;
		// 普通的类型，直接使用类	
		if (src instanceof Class) {
			result = (Class<?>) src;
			// 泛型类型，使用泛型
		} else if (src instanceof ParameterizedType) {
			result = (Class<?>) ((ParameterizedType) src).getRawType();
			// 泛型数组，获得具体的类
		} else if (src instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType) src).getGenericComponentType();
			if (componentType instanceof Class) {// 普通类型
				result = Array.newInstance((Class<?>) componentType, 0).getClass();
			} else {
				Class<?> componentClass = typeToClass(componentType);
				result = Array.newInstance(componentClass, 0).getClass();
			}
		}
		// 都不符合 使用 Object类
		if (result == null) {
			result = Object.class;
		}
		return result;
	}

	// 获取所有的字段
	private void addFields(Class<?> clazz) {
		// 获取自己所有的属性
		Field[] fields = clazz.getDeclaredFields();
		// 遍历 所有 属性
		for (Field field : fields) {
			// 不包含 key 进入
			if (!setMethods.containsKey(field.getName())) {
				// issue #379 - removed the check for final because JDK 1.5 allows
				// modification of final fields through reflection (JSR-133). (JGB)
				// pr #16 - final static can only be set by the classloader
				// 字段的 修饰符
				int modifiers = field.getModifiers();
				// 代码再看一下，多思考
				if (!(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers))) {
					// 不是 final 也不是 static 加入到里面
					addSetField(field);
				}
			}
			// 添加 getMethods 和 getTypes 中
			if (!getMethods.containsKey(field.getName())) {
				addGetField(field);
			}
		}
		// 获取父类的字段
		if (clazz.getSuperclass() != null) {
			addFields(clazz.getSuperclass());
		}
	}

	private void addSetField(Field field) {
		// 判断属性是否合理
		if (isValidPropertyName(field.getName())) {
			// 添加到 setMethods 中
			setMethods.put(field.getName(), new SetFieldInvoker(field));
			Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
			setTypes.put(field.getName(), typeToClass(fieldType));
		}
	}

	private void addGetField(Field field) {
		// 判断是否合理的属性
		if (isValidPropertyName(field.getName())) {
			getMethods.put(field.getName(), new GetFieldInvoker(field));
			Type fieldType = TypeParameterResolver.resolveFieldType(field, type);
			getTypes.put(field.getName(), typeToClass(fieldType));
		}
	}
	/**
	 * <B>方法名称：</B> isValidPropertyName <BR>
	 * <B>概要说明：</B> 判断是否是有效的属性名。"$"开头、 "serialVersionUID" 和 "class"三种名称不做添加 <BR>
	 * 
	 * @author 陈康明
	 * 
	 * @since 2020年06月14 09:51
	 * @return
	 * @return boolean
	 */
	private boolean isValidPropertyName(String name) {
		return !(name.startsWith("$") || "serialVersionUID".equals(name) || "class".equals(name));
	}

	/**
	 * This method returns an array containing all methods declared in this class
	 * and any superclass. We use this method, instead of the simpler
	 * Class.getMethods(), because we want to look for private methods as well.
	 * 此方法返回一个数组，其中包含在此类中声明的所有方法 和任何超类。 我们使用这种方法，而不是更简单
	 * Class.getMethods（），因为我们也希望查找私有方法。
	 * 
	 * @param cls
	 *            The class
	 * @return An array containing all methods in this class
	 */
	private Method[] getClassMethods(Class<?> cls) {
		// 每个方法签名 和 该方法的映射
		Map<String, Method> uniqueMethods = new HashMap<>();
		// 类 class
		Class<?> currentClass = cls;
		// 类 class 不是null 也不是顶级父类 object就继续循环
		while (currentClass != null && currentClass != Object.class) {
			// 记录当前类定义的方法，getDeclaredMethods只会拿自己的所有方法进去
			addUniqueMethods(uniqueMethods, currentClass.getDeclaredMethods());

			// we also need to look for interface methods -
			// because the class may be abstract
			// 记录接口中定义的方法
			Class<?>[] interfaces = currentClass.getInterfaces();
			for (Class<?> anInterface : interfaces) {
				addUniqueMethods(uniqueMethods, anInterface.getMethods());
			}
			// 获取父类，继续循环
			currentClass = currentClass.getSuperclass();
		}
		// 获取所有的方法名，这里是唯一的方法，子类重写了父类的方法， 就只会有父类的方法。
		Collection<Method> methods = uniqueMethods.values();
		// 转换成 Method 数组返回
		return methods.toArray(new Method[methods.size()]);
	}
	// 添加 唯一方法
	private void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
		for (Method currentMethod : methods) {
			// 不是桥接方法
			if (!currentMethod.isBridge()) {
				// signature(签名) returnType#方法名:参数名1,参数名2,参数名3
				String signature = getSignature(currentMethod);
				// check to see if the method is already known
				// if it is known, then an extended class must have
				// overridden a method
				// uniqueMethods 不存在该值，才添加方法，上层调用是先调用子类，然后父类，子类重写了父类方法，这个时候判断有，就不会添加父类的方法进去了。
				if (!uniqueMethods.containsKey(signature)) {
					uniqueMethods.put(signature, currentMethod);
				}
			}
		}
	}

	/**
	 * 
	 * <B>方法名称：</B> getSignature <BR>
	 * <B>概要说明：</B> 获取方法签名 <BR>
	 * 
	 * @author 陈康明
	 * 
	 * @since 2020年06月13 23:23
	 * @return
	 * @return returnType#方法名:参数名1,参数名2,参数名3
	 */
	private String getSignature(Method method) {
		StringBuilder sb = new StringBuilder();
		Class<?> returnType = method.getReturnType();
		if (returnType != null) {
			sb.append(returnType.getName()).append('#');
		}
		sb.append(method.getName());
		Class<?>[] parameters = method.getParameterTypes();
		for (int i = 0; i < parameters.length; i++) {
			if (i == 0) {
				sb.append(':');
			} else {
				sb.append(',');
			}
			sb.append(parameters[i].getName());
		}
		return sb.toString();
	}

	/**
	 * Checks whether can control member accessible.
	 *
	 * @return If can control member accessible, it return {@literal true}
	 * @since 3.5.0
	 */
	public static boolean canControlMemberAccessible() {
		try {
			SecurityManager securityManager = System.getSecurityManager();
			if (null != securityManager) {
				securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
			}
		} catch (SecurityException e) {
			return false;
		}
		return true;
	}

	/**
	 * Gets the name of the class the instance provides information for
	 *
	 * @return The class name
	 */
	public Class<?> getType() {
		return type;
	}

	public Constructor<?> getDefaultConstructor() {
		if (defaultConstructor != null) {
			return defaultConstructor;
		} else {
			throw new ReflectionException("There is no default constructor for " + type);
		}
	}

	public boolean hasDefaultConstructor() {
		return defaultConstructor != null;
	}

	public Invoker getSetInvoker(String propertyName) {
		Invoker method = setMethods.get(propertyName);
		if (method == null) {
			throw new ReflectionException(
					"There is no setter for property named '" + propertyName + "' in '" + type + "'");
		}
		return method;
	}

	public Invoker getGetInvoker(String propertyName) {
		Invoker method = getMethods.get(propertyName);
		if (method == null) {
			throw new ReflectionException(
					"There is no getter for property named '" + propertyName + "' in '" + type + "'");
		}
		return method;
	}

	/**
	 * Gets the type for a property setter
	 *
	 * @param propertyName
	 *            - the name of the property
	 * @return The Class of the property setter
	 */
	public Class<?> getSetterType(String propertyName) {
		Class<?> clazz = setTypes.get(propertyName);
		if (clazz == null) {
			throw new ReflectionException(
					"There is no setter for property named '" + propertyName + "' in '" + type + "'");
		}
		return clazz;
	}

	/**
	 * Gets the type for a property getter
	 *
	 * @param propertyName
	 *            - the name of the property
	 * @return The Class of the property getter
	 */
	public Class<?> getGetterType(String propertyName) {
		Class<?> clazz = getTypes.get(propertyName);
		if (clazz == null) {
			throw new ReflectionException(
					"There is no getter for property named '" + propertyName + "' in '" + type + "'");
		}
		return clazz;
	}

	/**
	 * Gets an array of the readable properties for an object
	 *
	 * @return The array
	 */
	public String[] getGetablePropertyNames() {
		return readablePropertyNames;
	}

	/**
	 * Gets an array of the writable properties for an object
	 *
	 * @return The array
	 */
	public String[] getSetablePropertyNames() {
		return writeablePropertyNames;
	}

	/**
	 * Check to see if a class has a writable property by name
	 *
	 * @param propertyName
	 *            - the name of the property to check
	 * @return True if the object has a writable property by the name
	 */
	public boolean hasSetter(String propertyName) {
		return setMethods.keySet().contains(propertyName);
	}

	/**
	 * Check to see if a class has a readable property by name
	 *
	 * @param propertyName
	 *            - the name of the property to check
	 * @return True if the object has a readable property by the name
	 */
	public boolean hasGetter(String propertyName) {
		return getMethods.keySet().contains(propertyName);
	}

	public String findPropertyName(String name) {
		return caseInsensitivePropertyMap.get(name.toUpperCase(Locale.ENGLISH));
	}
}
