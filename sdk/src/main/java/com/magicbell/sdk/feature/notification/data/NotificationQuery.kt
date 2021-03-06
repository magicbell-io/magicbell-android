package com.magicbell.sdk.feature.notification.data

import com.mobilejazz.harmony.data.query.Query
import com.magicbell.sdk.common.query.UserQuery


internal open class NotificationQuery(val notificationId: String, val userQuery: UserQuery) : Query()

internal class NotificationActionQuery(val action: Action, notificationId: String, userQuery: UserQuery) : NotificationQuery(notificationId, userQuery) {
  enum class Action {
    MARK_AS_READ,
    MARK_AS_UNREAD,
    ARCHIVE,
    UNARCHIVE,
    MARK_ALL_AS_READ,
    MARK_ALL_AS_SEEN
  }
}