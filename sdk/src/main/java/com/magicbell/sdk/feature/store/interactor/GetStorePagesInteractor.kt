package com.magicbell.sdk.feature.store.interactor

import com.mobilejazz.harmony.domain.interactor.GetInteractor
import com.magicbell.sdk.common.error.MagicBellError
import com.magicbell.sdk.common.network.graphql.CursorPredicate
import com.magicbell.sdk.common.query.UserQuery
import com.magicbell.sdk.feature.store.StoreContext
import com.magicbell.sdk.feature.store.StorePage
import com.magicbell.sdk.feature.store.StorePredicate
import com.magicbell.sdk.feature.store.data.StoreQuery
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class GetStorePagesInteractor(
  private val coroutineContext: CoroutineContext,
  private val getStoreNotificationInteractor: GetInteractor<Map<String, StorePage>>,
) {

  suspend operator fun invoke(
    storePredicate: StorePredicate,
    cursorPredicate: CursorPredicate,
    userQuery: UserQuery,
  ): StorePage {
    return withContext(coroutineContext) {
      val storePages = invoke(listOf(StoreContext("data", storePredicate, cursorPredicate)), userQuery)
      if (storePages.containsKey("data")) {
        storePages["data"]!!
      } else {
        throw  MagicBellError("Server didn't response correct data")
      }
    }
  }

  suspend operator fun invoke(
    contexts: List<StoreContext>,
    userQuery: UserQuery,
  ): Map<String, StorePage> {
    return withContext(coroutineContext) {
      getStoreNotificationInteractor(StoreQuery(contexts, userQuery))
    }
  }

}
