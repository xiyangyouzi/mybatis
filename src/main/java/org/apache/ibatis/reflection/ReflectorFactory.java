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
/**
 * ReflectorFactory
 * 
 * @author 陈康明
 * @since 2020年06月14日 16:15
 */
public interface ReflectorFactory {
	/**
	 * <B>方法名称：</B> isClassCacheEnabled <BR>
	 * <B>概要说明：</B> 是否开启缓存 <BR>
	 * 
	 * @author 陈康明
	 * 
	 * @since 2020年06月14 16:11
	 * @return
	 * @return boolean
	 */
	boolean isClassCacheEnabled();

	/**
	 * <B>方法名称：</B> setClassCacheEnabled <BR>
	 * <B>概要说明：</B> 是否开启缓存 <BR>
	 * 
	 * @author 陈康明
	 * 
	 * @since 2020年06月14 16:11
	 * @return
	 * @return void
	 */
	void setClassCacheEnabled(boolean classCacheEnabled);
	/**
	 * <B>方法名称：</B> findForClass <BR>
	 * <B>概要说明：</B> 通过 Class 查找Reflector  <BR>
	 * 
	 * @author 陈康明
	 * 
	 * @since 2020年06月14 16:12
	 * @return
	 * @return Reflector
	 */
	Reflector findForClass(Class<?> type);
}