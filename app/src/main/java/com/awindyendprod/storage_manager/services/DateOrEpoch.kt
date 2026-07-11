package com.awindyendprod.storage_manager.services

import java.util.Date

fun Date?.orEpoch(): Date = this ?: Date(0)
