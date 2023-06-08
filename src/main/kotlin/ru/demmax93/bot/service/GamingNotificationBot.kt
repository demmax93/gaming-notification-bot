package ru.demmax93.bot.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Calendar
import java.util.TimeZone
import java.util.Timer
import kotlin.concurrent.timerTask

@Service
class GamingNotificationBot(@Value("\${telegram.token}") token: String) : TelegramLongPollingBot(token) {
    private final val myGroupId = -615013028L
    private final val users = setOf("@demmax93", "@Welcome_LjAPb", "@yurazavrazhnov")
    private final val dayOff = "Не играем!"
    private final val gamingTimeMessage = "%s\nСегодня играем в %s!"
    private final val gamingOffMessage = "%s\nСегодня не играем!"
    private final val optionsWorkDays = listOf("20:00", "21:00", "22:00", dayOff)
    private final val optionsWeekDays = listOf("18:00", "19:00", "20:00", "21:00", "22:00", dayOff)
    private final val questions = setOf(
        "Привет, ребята, готовы ли вы сегодня поиграть?",
        "Приветствую, товарищи, кто готов провести сегодняшний вечер за игрой?",
        "Доброго времени суток, друзья, хотите сыграть сегодня?",
        "Здравствуйте, уважаемые игроманы, кто желает сегодня поиграть?",
        "Привет, командированные виртуальные воины, будем ли мы играть сегодня?",
        "Доброго дня, геймеры, кто готов провести время за игрой сегодня?",
        "Здравствуйте, фанаты компьютерных игр, кто хочет поиграть сегодня?",
        "Приветствую, господа любители игр, готовы ли вы сегодня присоединиться?",
        "Добрый вечер, увлеченные игроки, есть ли желающие сегодня сыграть?",
        "Приветствую, друзья геймеры, будем ли мы играть в этот вечер?",
        "Друзья, готовы ли мы сегодня заняться игрой?",
        "Доброго времени суток! Кто готов поиграть сегодня?",
        "Эй, ребята, кому интересно поиграть сегодня?",
        "Здравствуйте, уважаемые геймеры! Желаете ли вы провести игровой вечерок?",
        "Друзья, возьмёмся за игру сегодня?",
        "Приветствую, народ! Готовы ли мы сегодня вступить в игровой мир?",
        "Привет, фанаты игр! Кто хочет поиграть сегодня вместе?",
        "Добро пожаловать, геймеры! Кто готов сегодня сыграть вместе?",
        "Эй, любители игр! Кто сегодня настроен поиграть?",
        "Приветствую, друзья геймеры! Кто хочет сегодня поиграть вместе?"
    )

    @Value("\${telegram.botName}")
    private val botName: String = ""

    override fun getBotUsername(): String = botName

    override fun onUpdateReceived(update: Update) {
        if (update.hasPoll()) {
            val poll = update.poll
            if (poll.totalVoterCount == users.size) {
                if (poll.options.stream().anyMatch { option -> dayOff == option.text && option.voterCount > 0 }) {
                    sendMessage(gamingOffMessage.format(users.joinToString()))
                } else {
                    val gamingTime = poll.options.stream()
                        .filter { option -> dayOff != option.text && option.voterCount > 0 }
                        .max { option1, option2 ->
                            option1.text.replace(":00", "").compareTo(option2.text.replace(":00", ""))
                        }
                    if (gamingTime.isPresent) {
                        sendMessage(gamingTimeMessage.format(users.joinToString(), gamingTime.get().text))
                        val gamingHour = gamingTime.get().text.replace(":00", "").toInt()
                        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC+4"))
                        calendar.set(Calendar.HOUR_OF_DAY, gamingHour)
                        Timer().schedule(timerTask { sendMessage(users.joinToString()) }, calendar.time)
                    }
                }
            }
        }
    }

    @Scheduled(cron = "0 0 18 * * ?", zone = "Europe/Samara")
    fun sendPollByCron() {
        val now = LocalDate.now()
        val poll = SendPoll.builder()
            .chatId(myGroupId)
            .question(questions.random())
            .options(if (DayOfWeek.SATURDAY == now.dayOfWeek || DayOfWeek.SUNDAY == now.dayOfWeek) optionsWeekDays else optionsWorkDays)
            .isAnonymous(false)
            .build()
        sendMessage(users.joinToString())
        execute(poll)
    }

    fun sendMessage(responseText: String) {
        val message = SendMessage(myGroupId.toString(), responseText)
        execute(message)
    }
}