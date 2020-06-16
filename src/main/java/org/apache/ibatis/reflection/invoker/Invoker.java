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

import java.lang.reflect.InvocationTargetException;

/**
 * @author Clinton Begin
 */
/**
 * Invoker 调用者
 * 
 * @author 陈康明
 * @since 2020年06月14日 16:20
 */
public interface Invoker {
	/**
	 * <B>方法名称：</B> invoke <BR>
	 * <B>概要说明：</B> 执行调用，具体什么调用，由子类实现 <BR>
	 * 
	 * @author 陈康明
	 * 
	 * @since 2020年06月14 16:20
	 * @return
	 * @return Object
	 */
	Object invoke(Object target, Object[] args) throws IllegalAccessException, InvocationTargetException;
	/**
	 * <B>方法名称：</B> getType <BR>
	 * <B>概要说明：</B> 获取 类  <BR>
	 * 
	 * @author 陈康明
	 * 
	 * @since 2020年06月14 16:21
	 * @return
	 * @return Class<?>
	 */
	Class<?> getType();
}
