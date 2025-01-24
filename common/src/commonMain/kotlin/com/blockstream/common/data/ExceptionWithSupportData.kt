package com.blockstream.common.data

class ExceptionWithSupportData(throwable: Throwable, val supportData: SupportData) : Exception(throwable)