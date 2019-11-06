@file:Suppress("UNCHECKED_CAST")

package de.mannodermaus.gradle.plugins.junit5.internal

import java.lang.reflect.Method

/**
 * Safely obtain the reference to a method to the caller.
 * If the method with the given signature does not exist on the target object's class,
 * return null.
 *
 * @param named Method name
 * @param paramTypes Parameter types of the method's signature
 * @return A reference to the method with that signature, or null if it doesn't exist
 */
fun Any.reflectiveMethod(named: String, vararg paramTypes: Class<*>): Method? =
    try {
      val targetClass = if (this is Class<*>) {
        this
      } else {
        this.javaClass
      }
      targetClass.getMethod(named, *paramTypes)
    } catch (e: NoSuchMethodException) {
      null
    }

/**
 * Invoke the method on the given target with the provided parameters,
 * casting the result to the return type.
 *
 * @param target Target to call the method on.
 *               As with the Java Reflection APIs, use null to call a static method
 * @param params Parameters to call the method with
 * @return Result, cast to the desired type
 */
fun <T> Method.invokeTyped(target: Any? = null, vararg params: Any?): T? {
  return this.invoke(target, *params) as? T?
}
