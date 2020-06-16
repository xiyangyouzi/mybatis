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
package org.apache.ibatis.reflection.property;

import java.util.Locale;

import org.apache.ibatis.reflection.ReflectionException;

/**
 * @author Clinton Begin
 */
// 属性名相关的工具类方法
public final class PropertyNamer {

	private PropertyNamer() {
		// Prevent Instantiation of Static Class
		// 工具类，私有化构造方法，防止被创建
	}
	// TODO property的意思是属性
	// 通过方法名获取对应的属性名
	public static String methodToProperty(String name) {
		// is 开头，切割两个字符
		if (name.startsWith("is")) {
			name = name.substring(2);
		// get、set 开头获取三个
		} else if (name.startsWith("get") || name.startsWith("set")) {
			name = name.substring(3);
		} else {
			// 都不是，报错， 不是正常的参数解析，因为只能处理 is、set、get 方法
			throw new ReflectionException(
					"Error parsing property name '" + name + "'.  Didn't start with 'is', 'get' or 'set'.");
		}
		// name = "" 的情况会跳过了，直接返回 "";
		// 属性长度是 1 一个直接转换，属性首字母小写。
		// 属性长度大于 1 判断 属性第二个 字母是否是大写，如果是也不仅如此，TODO 为什么第二个字母大写不转换第一个字母小写？什么业务逻辑？
		if (name.length() == 1 || (name.length() > 1 && !Character.isUpperCase(name.charAt(1)))) {
			name = name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
		}
		
		return name;
	}
	// 判断是否是 get、 set、is 开头
	public static boolean isProperty(String name) {
		return name.startsWith("get") || name.startsWith("set") || name.startsWith("is");
	}
	// 判断是否是 get 方法
	public static boolean isGetter(String name) {
		return name.startsWith("get") || name.startsWith("is");
	}
	// 判断是否 是 set 方法
	public static boolean isSetter(String name) {
		return name.startsWith("set");
	}

}
