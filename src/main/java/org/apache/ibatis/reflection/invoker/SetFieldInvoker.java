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
// 实现 Invoker 接口，设置 Field 调用者。
public class SetFieldInvoker implements Invoker {
	// Field对象
	private final Field field;

	public SetFieldInvoker(Field field) {
		this.field = field;
	}

	// 设置 Field 属性，这里和  GetFieldInvoker 的invoke对应
	@Override
	public Object invoke(Object target, Object[] args) throws IllegalAccessException {
		try {
			// 设置 Field 属性的值，就是private String str = "abc" 这里就是 field.set("abc")
			field.set(target, args[0]);
		} catch (IllegalAccessException e) {
			if (Reflector.canControlMemberAccessible()) {
				field.setAccessible(true);
				field.set(target, args[0]);
			} else {
				throw e;
			}
		}
		return null;
	}
	// 返回 属性 类型
	@Override
	public Class<?> getType() {
		return field.getType();
	}
}
