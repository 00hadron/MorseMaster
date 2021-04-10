package ru.hadron.morsemaster.repositories

import kotlinx.coroutines.*
import ru.hadron.morsemaster.db.entity.*
import ru.hadron.morsemaster.util.CurrentLesson

import ru.hadron.morsemaster.util.Question
import timber.log.Timber
import java.sql.SQLException
import java.util.*
import javax.inject.Inject

class Storage @Inject constructor(
    val repository: DefaultRepository
) {

    private var adv_level = 75
    private var adv_max = 3

    inner class AdvItem (
        initSymbol: String,
        initShuffle: Double = Math.random()
    ) {
        var symbol: String = initSymbol
        var shuffle = initShuffle

    }

    inner class AdvItemShuffle
        : Comparator<AdvItem> {
        override fun compare(a: AdvItem, b: AdvItem): Int = when {
            a.shuffle < b.shuffle -> 1
            else -> -1
        }
    }

    fun getCode(text: String): String {
        var res = ""
        for (c: Char in text.toCharArray()) {
            if (c.equals(' ')) {
               res += "|"
            } else {
                try {
                    runBlocking {
                        GlobalScope.async(Dispatchers.IO) {
                            val code = repository.getStmCode(symbol = c.toString())
                            if (!code.isEmpty()) {
                                res += "$code "
                            }
                        }.await()
                    }
                } catch (e: SQLException) {
                    e.printStackTrace()
                    return res
                }

            }
        }
        Timber.e("========================================= get code res>>>>>> $res")
        return res
    }

    fun loadLesson(info: String): CurrentLesson? {
        var symbols: String? = null
        runBlocking {
            GlobalScope.async(Dispatchers.IO) {
                symbols = repository.getStmSymbolsFromLesson(info = info)
            }.await()
        }

        return if (symbols!!.isNotEmpty()) {
            var s = symbols?.replace(" ", "")?.replace(",", "")
            Timber.e("-load lesson--return symbols--${s}----")
            // Lesson(info = info, symbols = s)
            CurrentLesson(this, currsymbols = s)
        } else {
            Timber.e("-load lesson----return null!!!!----")
            null
        }
    }

    fun clearStat() {
        GlobalScope.launch(Dispatchers.IO) {
            repository.deleteStat()
        }
    }
    fun insertStat(stat: Stat) {
        GlobalScope.launch(Dispatchers.IO) {
            repository.insertStat(stat)
        }
    }
    fun insertOrIgnoreStat(symbol: String, lastseen: Long) {
        GlobalScope.launch(Dispatchers.IO) {
            repository.insertOrIgnoreStat(symbol, lastseen)
        }
    }

    fun initStat(symbols: String) {
        if (symbols != null) {
            for (symbol in symbols) {
                insertOrIgnoreStat(symbol = symbol.toString(), lastseen = System.currentTimeMillis() / (30 * 1000) )
            }
        } else {
            Timber.e("-------symbols didnt loaded (:")
        }
    }

    fun updateStat(symbol: String, correct: Boolean) {
        if (correct) {
            GlobalScope.launch(Dispatchers.IO) {
                repository.updateStatIfAnswerCorrect(
                    lastseen = System.currentTimeMillis() / (30*1000) - (Math.random() * 3.0f).toInt(), //?
                    symbol = symbol
                )
            }
        } else {
            GlobalScope.launch(Dispatchers.IO) {
                repository.updateStatIfAnswerNotCorrect(
                    lastseen = System.currentTimeMillis() / (30*1000) - (Math.random() * 3.0f).toInt(),
                    symbol = symbol
                )
            }
        }
    }

    fun getNextSymbol(remain: Int): Question {
        var rs: List<StatForStmNextSymbol> = mutableListOf()

        runBlocking {
            GlobalScope.async(Dispatchers.IO) {
               rs = repository.getStmNextSymbol(adv_level)
              //  rs = rs + repository.getStmNextSymbol(adv_level)
            }.await()
        }

        var symbol = rs.first().symbol
        var correct = rs.first().correct

        if (remain > 1 && Math.random() > 0.5) {
            symbol = rs.last().symbol
            correct = rs.last().correct
        }

        Timber.e("getNextSymbol===========> ${symbol} ")
        return Question(symbol = symbol, correct = correct) //test
    }

    fun getCountAdv(): Int {
        var result: Int = 0
        runBlocking {
            GlobalScope.async(Dispatchers.IO) {
                result = repository.getStmCountAdv(ratio = adv_level)._count
            }.await()
        }
        Timber.e("stmcountadv count======>>> ${result}")
        return result
    }

    fun getNextAdv(adv: Int): Question {
        var rs: List<StatForStmNextAdv>? = null

        runBlocking {
            GlobalScope.async(Dispatchers.IO) {
               rs = repository.getStmNextAdv(adv_level)
            }.await()
        }

        var items = mutableListOf<AdvItem>()
        var question = ""
        var min_ratio = 99

        for (i in rs!!) {
            Timber.e("$i symbol = ${i.symbol}")
            min_ratio = Math.min(min_ratio, i.ratio)
            items.add(AdvItem(i.symbol))
        }

        val count = 2 + (adv_max - 1) * (min_ratio - adv_level) / (100 - adv_level)

        if (items.size > count) {
            items = Vector(items.subList(0, count))
        } else {
            var ii = items.size - 1
            while (ii < count) {
              var item = items[ii % adv]
                items.add(AdvItem(item.symbol))
                Timber.e("++++++++++item.symbol+++++++++${item.symbol}++++++++++++++ ...}")
                ii++
            }
        }

        items.sortedWith(AdvItemShuffle())
        for (i in 0 until count) question += items.get(i).symbol

        return Question(question, 999)
    }

/*    fun getLessons(): List<String>? {
        var items : List<String>? = null
        runBlocking {
            GlobalScope.async(Dispatchers.IO) {
                items = repository.getInfoFromLesson().value
            }.await()
        }
        return items
    }*/

    fun getWorth() = repository.getStmWorth()

    fun setAdvLevel(value: Int) { adv_level = value }
    fun setAdvMax(value: Int) {adv_max = value }

    //-------

    fun getInfoFromLesson()  = repository.getInfoFromLesson()
    fun insertCvsLesson(lesson: Lesson) {
        GlobalScope.launch(Dispatchers.IO) {
            repository.insertCvsLesson(lesson)
        }
    }


    fun insertCvsCodes(codes: Codes) {
        GlobalScope.launch(Dispatchers.IO) {
            repository.insertCvsCodes(codes)
        }
    }
    fun insertCvsCodesGroup(codesGroup: CodesGroup) {
        GlobalScope.launch(Dispatchers.IO) {
            repository.insertCvsCodesGroup(codesGroup)
        }
    }
}