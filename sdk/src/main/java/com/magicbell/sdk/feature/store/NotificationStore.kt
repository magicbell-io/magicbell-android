package com.magicbell.sdk.feature.store

import com.magicbell.sdk.common.error.MagicBellError
import com.magicbell.sdk.common.network.graphql.CursorPredicate
import com.magicbell.sdk.common.network.graphql.CursorPredicate.Cursor.Next
import com.magicbell.sdk.common.network.graphql.Edge
import com.magicbell.sdk.common.query.UserQuery
import com.magicbell.sdk.feature.notification.Notification
import com.magicbell.sdk.feature.notification.data.NotificationActionQuery
import com.magicbell.sdk.feature.notification.data.NotificationActionQuery.Action.ARCHIVE
import com.magicbell.sdk.feature.notification.data.NotificationActionQuery.Action.MARK_ALL_AS_READ
import com.magicbell.sdk.feature.notification.data.NotificationActionQuery.Action.MARK_AS_READ
import com.magicbell.sdk.feature.notification.data.NotificationActionQuery.Action.MARK_AS_UNREAD
import com.magicbell.sdk.feature.notification.data.NotificationActionQuery.Action.UNARCHIVE
import com.magicbell.sdk.feature.notification.interactor.ActionNotificationInteractor
import com.magicbell.sdk.feature.notification.interactor.DeleteNotificationInteractor
import com.magicbell.sdk.feature.store.interactor.FetchStorePageInteractor
import java.util.Date

// TODO: Methods for collection
class NotificationStore internal constructor(
  val predicate: StorePredicate,
  private val userQuery: UserQuery,
  private val fetchStorePageInteractor: FetchStorePageInteractor,
  private val actionNotificationInteractor: ActionNotificationInteractor,
  private val deleteNotificationInteractor: DeleteNotificationInteractor,
) {
  private val pageSize = 20

  private val edges: MutableList<Edge<Notification>> = mutableListOf()

  var totalCount: Int = 0
    private set
  var unreadCount: Int = 0
    private set
  var unseenCount: Int = 0
    private set

  var hasNextPage: Boolean = true
    private set

  private var nextPageCursor: String? = null

  suspend fun refresh(): Result<List<Notification>> {
    return runCatching {
      val cursorPredicate = CursorPredicate(size = pageSize)
      val storePage = fetchStorePageInteractor(predicate, cursorPredicate, userQuery)
      clear()
      configurePagination(storePage)
      configureCount(storePage)
      val newEdges = storePage.edges
      edges.addAll(newEdges)
      val notifications = newEdges.map { it.node }
      // TODO: notify changes
      notifications
    }
  }

  suspend fun fetch(): Result<List<Notification>> {
    return runCatching {
      if (!hasNextPage) {
        listOf<Notification>()
      }
      val cursorPredicate: CursorPredicate = nextPageCursor?.let { after ->
        CursorPredicate(Next(after), pageSize)
      } ?: run {
        CursorPredicate(size = pageSize)
      }

      val storePage = fetchStorePageInteractor(predicate, cursorPredicate, userQuery)
      configurePagination(storePage)
      configureCount(storePage)

      val oldCount = edges.count()
      val newEdges = storePage.edges
      val notifications = newEdges.map { it.node }
      //TODO: Notify observers
      notifications
    }
  }

  suspend fun delete(notification: Notification): Result<Unit> {
    return runCatching {
      deleteNotificationInteractor(notification.id, userQuery)
      val notificationIndex = edges.indexOfFirst { it.node.id == notification.id }
      if (notificationIndex != -1) {
        updateCountersWhenDelete(edges[notificationIndex].node, predicate)
        edges.removeAt(notificationIndex)
        //TODO: Notify
      }
    }
  }

  suspend fun markAsRead(notification: Notification): Result<Notification> {
    return runCatching {
      executeNotificationAction(
        notification,
        MARK_AS_READ,
        modifications = { notification ->
          markNotificationAsRead(notification, predicate)
        }).getOrThrow()
    }
  }

  suspend fun markAsUnread(notification: Notification): Result<Notification> {
    return runCatching {
      executeNotificationAction(
        notification,
        MARK_AS_UNREAD,
        modifications = { notification ->
          markNotificationAsUnread(notification, predicate)
        }).getOrThrow()
    }
  }

  suspend fun archive(notification: Notification): Result<Notification> {
    return runCatching {
      executeNotificationAction(
        notification,
        ARCHIVE,
        modifications = { notification ->
          archiveNotification(notification, predicate)
        }).getOrThrow()
    }
  }

  suspend fun unarchive(notification: Notification): Result<Notification> {
    return runCatching {
      executeNotificationAction(
        notification,
        UNARCHIVE,
        modifications = { notification ->
          notification.archivedAt = null
        }).getOrThrow()
    }
  }

  suspend fun markAllNotificationAsRead(): Result<Unit> {
    return runCatching {
      executeAllNotificationsAction(
        MARK_ALL_AS_READ,
        modifications = { notification ->
          markNotificationAsRead(notification, predicate)
        }
      )
    }
  }

  suspend fun markAllNotificationAsSeen(): Result<Unit> {
    return runCatching {
      executeAllNotificationsAction(
        MARK_ALL_AS_READ,
        modifications = { notification ->
          if (notification.seenAt == null) {
            notification.seenAt = Date()
            unseenCount -= 1
          }
        }
      )
    }
  }

  private suspend fun executeAllNotificationsAction(
    action: NotificationActionQuery.Action,
    modifications: (Notification) -> Unit,
  ): Result<Unit> {
    return runCatching {
      actionNotificationInteractor(action, userQuery = userQuery)
      for (i in edges.indices) {
        modifications(edges[i].node)
      }
    }
  }

  private fun markNotificationAsRead(notification: Notification, storePredicate: StorePredicate) {
    if (notification.seenAt == null) {
      unseenCount - 1
    }

    if (notification.readAt == null) {
      unreadCount -= 1

      storePredicate.read?.also {
        if (it) {
          totalCount -= 1
        } else {
          totalCount -= 1
        }
      }
    }

    val now = Date()
    notification.readAt = now
    notification.seenAt = now
  }

  private fun markNotificationAsUnread(notification: Notification, storePredicate: StorePredicate) {
    storePredicate.read?.also {
      if (it) {
        totalCount -= 1
        unreadCount = 0
      } else {
        totalCount += 1
        unreadCount += 1
      }
    } ?: run {
      unreadCount + 1
    }

    notification.readAt = null
  }

  private fun archiveNotification(notification: Notification, storePredicate: StorePredicate) {
    if (notification.archivedAt != null) {
      return
    }

    if (notification.seenAt == null) {
      unseenCount -= 1
    }

    if (notification.readAt == null) {
      unreadCount -= 1
    }

    if (notification.archivedAt == null) {
      if (!storePredicate.archived) {
        totalCount -= 1
      }
    }

    notification.archivedAt = Date()
  }

  private suspend fun executeNotificationAction(
    notification: Notification,
    action: NotificationActionQuery.Action,
    modifications: (Notification) -> Unit,
  ): Result<Notification> {
    return runCatching {
      actionNotificationInteractor(action, notification.id, userQuery)
      val notificationIndex = edges.indexOfFirst { it.node.id == notification.id }
      if (notificationIndex != -1) {
        modifications(notification)
        notification
      } else {
        throw MagicBellError("Notification not found in Store")
      }
    }
  }

  private fun updateCountersWhenDelete(notification: Notification, predicate: StorePredicate) {

  }

  private fun configurePagination(storePage: StorePage) {
    val pageInfo = storePage.pageInfo
    nextPageCursor = pageInfo.endCursor
    hasNextPage = pageInfo.hasNextPage
  }

  private fun configureCount(storePage: StorePage) {
    totalCount = storePage.totalCount
    unreadCount = storePage.unreadCount
    unseenCount = storePage.unseenCount
  }

  private fun clear() {
    edges.clear()
    totalCount = 0
    unreadCount = 0
    unseenCount = 0
    nextPageCursor = null
    hasNextPage = true
    // TODO: Notify changes
  }
}