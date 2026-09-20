package com.huanchengfly.tieba.post.api.models

import com.google.gson.annotations.SerializedName
import com.huanchengfly.tieba.post.models.BaseBean

/**
 * 粉丝列表（`/c/u/fans/page`）返回体。
 *
 * 接口来源：aiotieba `get_fans`，返回 JSON（非 protobuf）。
 */
data class FansPageBean(
    @SerializedName("error_code")
    val errorCode: Int = 0,
    @SerializedName("error_msg")
    val errorMsg: String? = null,
    @SerializedName("user_list")
    val userList: List<FanBean> = emptyList(),
    @SerializedName("page")
    val page: FansPageInfoBean? = null,
) : BaseBean() {

    /**
     * 单个粉丝信息。
     */
    data class FanBean(
        val id: Long = 0,
        val portrait: String? = null,
        val name: String? = null,
        @SerializedName("name_show")
        val nameShow: String? = null,
    ) {

        /**
         * 头像 portrait 可能带 `?t=xxx` 后缀，去掉后拼接头像地址。
         */
        val cleanPortrait: String?
            get() = portrait?.substringBefore("?")
    }

    /**
     * 分页信息。
     */
    data class FansPageInfoBean(
        @SerializedName("page_size")
        val pageSize: Int = 0,
        @SerializedName("current_page")
        val currentPage: Int = 1,
        @SerializedName("total_page")
        val totalPage: Int = 0,
        @SerializedName("total_count")
        val totalCount: Int = 0,
        @SerializedName("has_more")
        val hasMore: Int = 0,
        @SerializedName("has_prev")
        val hasPrev: Int = 0,
    )
}
