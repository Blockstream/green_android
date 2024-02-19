package com.blockstream.common.looks.transaction



sealed interface TransactionStatus

class Unconfirmed(val confirmationsRequired: Int = 6) : TransactionStatus
class Confirmed(val confirmations: Int, val confirmationsRequired : Int = 6) : TransactionStatus
object Completed : TransactionStatus
class Failed(val error: String = "") : TransactionStatus