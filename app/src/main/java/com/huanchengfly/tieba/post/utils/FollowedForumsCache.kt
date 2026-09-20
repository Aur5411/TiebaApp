package com.huanchengfly.tieba.post.utils

import com.huanchengfly.tieba.post.api.models.ForumGuideBean.LikeForum

object FollowedForumsCache {
    @Volatile
    private var forumMap: Map<Long, LikeForum> = emptyMap()

    fun updateAll(forums: List<LikeForum>?) {
        forumMap = forums?.associateBy { it.forumId } ?: emptyMap()
    }

    fun isFollowed(id: Long?): Boolean {
        if (id == null || id == 0L) return false
        return forumMap.containsKey(id)
    }

    fun updateOrAddFollowedForum(forum: LikeForum?) {
        if (forum == null || forum.forumId == 0L) return
        synchronized(this) {
            val newMap = forumMap.toMutableMap()
            newMap[forum.forumId] = forum
            forumMap = newMap
        }
    }

    fun removeFollowedForum(id: Long?) {
        if (id == null || id == 0L || !forumMap.containsKey(id)) return
        synchronized(this) {
            val newMap = forumMap.toMutableMap()
            newMap.remove(id)
            forumMap = newMap
        }
    }

    fun getFollowedForum(id: Long?): LikeForum? {
        if (id == null || id == 0L) return null
        return forumMap[id]
    }

    fun getAllFollowedForums(): List<LikeForum> = forumMap.values.toList()
}