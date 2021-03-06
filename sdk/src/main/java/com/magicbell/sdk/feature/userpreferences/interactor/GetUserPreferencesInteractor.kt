package com.magicbell.sdk.feature.userpreferences.interactor

import com.mobilejazz.harmony.domain.interactor.GetInteractor
import com.magicbell.sdk.common.query.UserQuery
import com.magicbell.sdk.feature.userpreferences.UserPreferences
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class GetUserPreferencesInteractor(
  private val coroutineContext: CoroutineContext,
  private val getUserPreferencesInteractor: GetInteractor<UserPreferences>,
) {

  suspend operator fun invoke(userQuery: UserQuery): UserPreferences {
    return withContext(coroutineContext) {
      getUserPreferencesInteractor(userQuery)
    }
  }
}