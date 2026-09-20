package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.GeneralTabList.GeneralTabListResponse
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaUnknownException
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

object GeneralTabListRepository {
    var lastHash: String = ""
    var lastResponse: GeneralTabListResponse? = null

    fun generalTabList(
        forumId: Long,
        forumName: String,
        tabId: Int,
        tabType: Int,
        tabName: String,
        isGeneralTab: Int,
        pn: Int = 1,
        sortType: Int = -1,
        lastThreadId: Long = 0,
        isDefaultNavTab: Int = 0,
        forceNew: Boolean = false,
    ): Flow<GeneralTabListResponse> {
        val hash = "${forumId}_${tabId}_${pn}_${sortType}_${lastThreadId}_${tabType}"
        if (!forceNew && lastResponse != null && lastHash == hash) {
            return flowOf(lastResponse!!)
        }
        lastHash = hash
        return TiebaApi.getInstance().generalTabList(
            forumId, forumName, tabId, tabType, tabName, isGeneralTab,
            pn, sortType, lastThreadId, isDefaultNavTab
        ).map { response ->
            if (response.data_ == null) throw TiebaUnknownException
            val userList = response.data_.user_list
            val threadList = response.data_.general_list
                .map { threadInfo ->
                    threadInfo.copy(author = userList.find { it.id == threadInfo.authorId })
                }
                .filter { !App.INSTANCE.appPreferences.blockVideo || it.videoInfo == null }
                .filter { it.ala_info == null }
            response.copy(data_ = response.data_.copy(general_list = threadList))
        }.onEach { lastResponse = it }
    }
}
