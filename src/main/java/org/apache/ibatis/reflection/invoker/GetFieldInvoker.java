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
package org.apache.ibatis.reflection.invoker;

import java.lang.reflect.Field;

import org.apache.ibatis.reflection.Reflector;

/**
 * @author Clinton Begin
 */
/**
 * GetFieldInvoker
 * 
 * 实现 Invoker 接口，获得 Field 调用者
 * 
 * @author 陈康明
 * @since 2020年06月14日 16:27
 */
public class GetFieldInvoker implements Invoker {
	// Field对象
	private final Field field;
	// 有参构造方法
	public GetFieldInvoker(Field field) {
		this.field = field;
	}

	// 获得属性
	@Override
	public Object invoke(Object target, Object[] args) throws IllegalAccessException {
		try {
			// 获取 field 属性的值，field.get("abc")
			return field.get(target);
		} catch (IllegalAccessException e) {
			// 如果抛出异常则 设置 private 的访问私有权限，如果是非 private 修饰的，直接抛出异常。
			if (Reflector.canControlMemberAccessible()) {
				field.setAccessible(true);
				return field.get(target);
			} else {
				throw e;
			}
		}
	}
	
	// 返回 属性 类型
	@Override
	public Class<?> getType() {
		return field.getType();
	}
}
