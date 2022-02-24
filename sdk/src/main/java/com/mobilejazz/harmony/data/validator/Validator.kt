package com.mobilejazz.harmony.data.validator

interface Validator<in T> {

  fun isValid(value: T): Boolean

  fun isValid(values: List<T>): Boolean {
    if (values.isEmpty()) return false

    values.forEach { if (!isValid(it)) return false }

    return true
  }
}
