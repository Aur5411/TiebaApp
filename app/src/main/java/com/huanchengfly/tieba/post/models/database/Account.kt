package com.huanchengfly.tieba.post.models.database

import androidx.compose.runtime.Stable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Stable
@Entity(tableName = "account")
data class Account @JvmOverloads constructor(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var uid: String = "",
    var name: String = "",
    var bduss: String = "",
    var tbs: String = "",
    var portrait: String = "",
    @ColumnInfo(name = "stoken") var sToken: String = "",
    var cookie: String = "",
    @ColumnInfo(name = "nameshow") var nameShow: String? = null,
    var intro: String? = null,
    var sex: String? = null,
    @ColumnInfo(name = "fansnum") var fansNum: String? = null,
    @ColumnInfo(name = "postnum") var postNum: String? = null,
    @ColumnInfo(name = "threadnum") var threadNum: String? = null,
    @ColumnInfo(name = "concernnum") var concernNum: String? = null,
    @ColumnInfo(name = "tbage") var tbAge: String? = null,
    var age: String? = null,
    @ColumnInfo(name = "birthdayshowstatus") var birthdayShowStatus: String? = null,
    @ColumnInfo(name = "birthdaytime") var birthdayTime: String? = null,
    var constellation: String? = null,
    @ColumnInfo(name = "tiebauid") var tiebaUid: String? = null,
    @ColumnInfo(name = "loadsuccess") var loadSuccess: Boolean = false,
    var uuid: String? = "",
    var zid: String? = "",
)
