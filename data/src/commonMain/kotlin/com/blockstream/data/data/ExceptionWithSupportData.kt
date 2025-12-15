package com.blockstream.data.data

class ExceptionWithSupportData(throwable: Throwable, val supportData: SupportData) : Exception(throwable)