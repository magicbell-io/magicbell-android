package com.mobilejazz.harmony.data.repository

import com.mobilejazz.harmony.data.mapper.Mapper
import com.mobilejazz.harmony.data.operation.DefaultOperation
import com.mobilejazz.harmony.data.operation.Operation
import com.mobilejazz.harmony.data.query.IdQuery
import com.mobilejazz.harmony.data.query.IdsQuery
import com.mobilejazz.harmony.data.query.Query

interface Repository {

  fun notSupportedQuery(): Nothing = throw UnsupportedOperationException("Query not supported")

  fun notSupportedOperation(): Nothing = throw UnsupportedOperationException("Operation not defined")
}

// Repositories
interface GetRepository<V> : Repository {
  suspend fun get(query: Query, operation: Operation = DefaultOperation): V
  suspend fun getAll(query: Query, operation: Operation = DefaultOperation): List<V>
}

interface PutRepository<V> : Repository {
  suspend fun put(query: Query, value: V?, operation: Operation = DefaultOperation): V
  suspend fun putAll(query: Query, value: List<V>? = emptyList(), operation: Operation = DefaultOperation): List<V>
}

interface DeleteRepository : Repository {
  suspend fun delete(query: Query, operation: Operation = DefaultOperation)
}

// Extensions

suspend fun <K, V> GetRepository<V>.get(id: K, operation: Operation = DefaultOperation): V = get(IdQuery(id), operation)

suspend fun <K, V> GetRepository<V>.getAll(ids: List<K>, operation: Operation = DefaultOperation): List<V> = getAll(IdsQuery(ids), operation)

suspend fun <K, V> PutRepository<V>.put(id: K, value: V?, operation: Operation = DefaultOperation): V = put(IdQuery(id), value, operation)

suspend fun <K, V> PutRepository<V>.putAll(ids: List<K>, values: List<V>? = emptyList(), operation: Operation = DefaultOperation) = putAll(
  IdsQuery(ids), values,
  operation
)

suspend fun <K> DeleteRepository.delete(id: K, operation: Operation = DefaultOperation) = delete(IdQuery(id), operation)

fun <K, V> GetRepository<K>.withMapping(mapper: Mapper<K, V>): GetRepository<V> = GetRepositoryMapper(this, mapper)

fun <K, V> PutRepository<K>.withMapping(toMapper: Mapper<K, V>, fromMapper: Mapper<V, K>): PutRepository<V> = PutRepositoryMapper(this, toMapper, fromMapper)
