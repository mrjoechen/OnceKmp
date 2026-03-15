package io.github.mrjoechen

object Amount {
    fun exactly(numberOfTimes: Int): CountChecker = CountChecker { count ->
        numberOfTimes == count
    }

    fun moreThan(numberOfTimes: Int): CountChecker = CountChecker { count ->
        count > numberOfTimes
    }

    fun lessThan(numberOfTimes: Int): CountChecker = CountChecker { count ->
        count < numberOfTimes
    }
}
