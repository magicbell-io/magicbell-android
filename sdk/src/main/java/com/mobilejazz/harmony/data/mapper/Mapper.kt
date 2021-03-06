package com.mobilejazz.harmony.data.mapper

/**
 * Interface to map an object type to another object type
 */
interface Mapper<in From, out To> {

  fun map(from: From): To
}

/**
 * BlankMapper returns the same value
 */
class BlankMapper<T> : Mapper<T, T> {

  override fun map(from: T): T = from
}

class ClosureMapper<in From, out To>(val closure: (from: From) -> To) : Mapper<From, To> {

  override fun map(from: From): To = closure(from)
}

/**
 * Mapping method for lists
 */
fun <From, To> Mapper<From, To>.map(values: List<From>): List<To> = values.map { map(it) }

/**
 * Mapping method for Maps
 *
 * @param value A Map<K, From> of ket-value, where value is typed as "From"
 * @return A Map<K, To> of mapped values
 */
fun <From, To, K> Mapper<From, To>.map(value: Map<K, From>): Map<K, To> {
  return value.mapValues { map(it.value) }
}
