package com.blockstream.common.data

class ExceptionWithErrorReport(throwable: Throwable, val errorReport: ErrorReport) : Exception(throwable)