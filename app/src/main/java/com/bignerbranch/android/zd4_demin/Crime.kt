package com.bignerbranch.android.zd4_demin

import java.util.Date
import java.util.UUID

data class Crime (val id: UUID = UUID.randomUUID(), val date: Date){
    var title = ""
    var isSolved = false
    var suspect = ""
}