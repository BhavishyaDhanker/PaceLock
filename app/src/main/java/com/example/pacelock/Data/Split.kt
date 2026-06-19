package com.example.pacelock.Data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class Split(
    val kilometerNo: Int,
    val seconds: Long,
    val cumulativeSeconds: Long
) : Parcelable
