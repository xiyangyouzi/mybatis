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

import java.lang.reflect.Field;

import org.apache.ibatis.reflection.Reflector;

/**
 * @author Clinton Begin
 */
// 属性复制器
public final class PropertyCopier {
	// 私有构造方法
	private PropertyCopier() {
		// Prevent Instantiation of Static Class
		// 防止静态类实例化。理解：这个类就是用来放一个静态方法的，直接调用，不需要实例化。
	}
	
	// 将 sourceBean 的属性，复制到 destinationBean 中 TODO 这里只有一个 值，为什么所有字段都加上，推测 sourceBean 是一个对象通过某种方式自动找到。
	public static void copyBeanProperties(Class<?> type, Object sourceBean, Object destinationBean) {
		// 保存 当前 Type
		Class<?> parent = type;
		// 循环遍历 不但复制到父类，直到父类不存在。
		while (parent != null) {
			// 获取 当前 parent 类定义的属性
			final Field[] fields = parent.getDeclaredFields();
			// 遍历
			for (Field field : fields) {
				try {
					try {
						// sourceBean 复制到 destinationBean 中去
						field.set(destinationBean, field.get(sourceBean));
					} catch (IllegalAccessException e) {
						// 设置属性可以访问，然后赋值
						if (Reflector.canControlMemberAccessible()) {
							field.setAccessible(true);
							field.set(destinationBean, field.get(sourceBean));
						} else {
							throw e;
						}
					}
				} catch (Exception e) {
					// Nothing useful to do, will only fail on final fields, which will be ignored.
				}
			}
			// 获取父类
			parent = parent.getSuperclass();
		}
	}

}
