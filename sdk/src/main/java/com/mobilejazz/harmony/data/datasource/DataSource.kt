package com.mobilejazz.harmony.data.datasource

import com.mobilejazz.harmony.data.error.QueryNotSupportedException
import com.mobilejazz.harmony.data.mapper.Mapper
import com.mobilejazz.harmony.data.query.IdQuery
import com.mobilejazz.harmony.data.query.IdsQuery
import com.mobilejazz.harmony.data.query.Query
import com.mobilejazz.harmony.data.repository.GetRepository
import com.mobilejazz.harmony.data.repository.PutRepository
import com.mobilejazz.harmony.data.repository.SingleDeleteDataSourceRepository
import com.mobilejazz.harmony.data.repository.SingleGetDataSourceRepository
import com.mobilejazz.harmony.data.repository.SinglePutDataSourceRepository
import com.mobilejazz.harmony.data.repository.withMapping

interface DataSource {

  fun notSupportedQuery(): Nothing = throw QueryNotSupportedException("Query not supported")
}

// DataSources
interface GetDataSource<V> : DataSource {
  suspend fun get(query: Query): V

  suspend fun getAll(query: Query): List<V>
}

interface PutDataSource<V> : DataSource {
  suspend fun put(query: Query, value: V?): V

  suspend fun putAll(query: Query, value: List<V>? = emptyList()): List<V>
}

interface DeleteDataSource : DataSource {
  suspend fun delete(query: Query)
}

// Extensions
suspend fun <K, V> GetDataSource<V>.get(id: K): V = get(IdQuery(id))

suspend fun <K, V> GetDataSource<V>.getAll(ids: List<K>): List<V> = getAll(IdsQuery(ids))

suspend fun <K, V> PutDataSource<V>.put(id: K, value: V?): V = put(IdQuery(id), value)

suspend fun <K, V> PutDataSource<V>.putAll(ids: List<K>, values: List<V>?) = putAll(IdsQuery(ids), values)

suspend fun <K> DeleteDataSource.delete(id: K) = delete(IdQuery(id))

suspend fun <K> DeleteDataSource.delete(ids: List<K>) = delete(IdsQuery(ids))

// Extensions to create
fun <V> GetDataSource<V>.toGetRepository() = SingleGetDataSourceRepository(this)

fun <K, V> GetDataSource<K>.toGetRepository(mapper: Mapper<K, V>): GetRepository<V> = toGetRepository().withMapping(mapper)

fun <V> PutDataSource<V>.toPutRepository() = SinglePutDataSourceRepository(this)

fun <K, V> PutDataSource<K>.toPutRepository(toMapper: Mapper<K, V>, fromMapper: Mapper<V, K>): PutRepository<V> =
  toPutRepository().withMapping(toMapper, fromMapper)

fun DeleteDataSource.toDeleteRepository() = SingleDeleteDataSourceRepository(this)
