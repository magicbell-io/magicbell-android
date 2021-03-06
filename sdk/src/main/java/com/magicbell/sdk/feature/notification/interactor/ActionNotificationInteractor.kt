package com.magicbell.sdk.feature.notification.interactor

import com.mobilejazz.harmony.domain.interactor.PutInteractor
import com.magicbell.sdk.common.query.UserQuery
import com.magicbell.sdk.feature.notification.data.NotificationActionQuery
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal interface ActionNotificationInteractor {
  suspend operator fun invoke(
    action: NotificationActionQuery.Action,
    notificationId: String? = null,
    userQuery: UserQuery
  )
}

internal class ActionNotificationDefaultInteractor(
  private val coroutineContext: CoroutineContext,
  private val actionInteractor: PutInteractor<Unit>,
) : ActionNotificationInteractor {

  override suspend operator fun invoke(action: NotificationActionQuery.Action, notificationId: String?, userQuery: UserQuery) {
    return withContext(coroutineContext) {
      val query = NotificationActionQuery(action, notificationId ?: "", userQuery)
      actionInteractor.invoke(null, query)
    }
  }
}