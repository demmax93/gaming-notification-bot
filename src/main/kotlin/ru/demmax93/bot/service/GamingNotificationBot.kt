package ru.demmax93.bot.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import java.time.*
import java.util.*

@Service
class GamingNotificationBot(
    @Value("\${telegram.token}") token: String,
    val manualTaskScheduleService: ManualTaskScheduleService
) : TelegramLongPollingBot(token) {
    private final val myGroupId = -615013028L
    private final val users = setOf("@demmax93", "@Welcome_LjAPb", "@yurazavrazhnov")
    private final val dayOff = "Не играем!"
    private final val gamingTimeMessage = "%s\nСегодня играем в %s!"
    private final val gamingOffMessage = "%s\nСегодня не играем!"
    private final val gamingDelayMessage = "%s\nНачинаем играть на %s минут позже!"
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
                        .max { option1, option2 -> extractHours(option1.text).compareTo(extractHours(option2.text)) }
                    if (gamingTime.isPresent) {
                        sendMessage(gamingTimeMessage.format(users.joinToString(), gamingTime.get().text))
                        var gameTime = LocalDateTime.now(ZoneId.of("Europe/Samara"))
                        gameTime = gameTime.withHour(extractHours(gamingTime.get().text).toInt()).withMinute(0).withSecond(0)
                        manualTaskScheduleService.addNewTask({ sendMessage(users.joinToString()) },
                            Date.from(gameTime.toInstant(ZoneOffset.of("+4"))))
                    }
                }
            }
        }
        if (update.hasMessage()) {
            val message = update.message
            val responseText = if (message.hasText()) {
                val messageText = message.text
                when {
                    messageText == "/help" -> "Доступтные команды:\n" +
                            "/cancel - отменена игровой сессии сегодня\n" +
                            "/delay {number of minutes} - перенос сегодняшней игровой сессии на количество минут указанные после команды (доступно значения от 1 дл 59)."
                    messageText == "/cancel" -> cancelTodayGame()
                    messageText.startsWith("/delay") -> delayTodayGame(messageText)
                    else -> ""
                }
            } else ""
            if (responseText.isNotEmpty()) {
                sendMessage(responseText)
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

    private fun extractHours(option: String): String {
        return option.replace(":00", "")
    }

    private fun cancelTodayGame(): String {
        val taskId = manualTaskScheduleService.taskId.get()
        manualTaskScheduleService.removeTaskFromScheduler(taskId)
        return gamingOffMessage.format(users.joinToString())
    }

    private fun delayTodayGame(text: String): String {
        val minutesToDelay = try {
            text.removePrefix("/delay").trim().toInt()
        } catch (exception: Exception) {
            return "Не смог разобрать число которое вы ввели, попробуйте ещё раз"
        }
        if (minutesToDelay < 1 || minutesToDelay > 59) {
            return "Введенное число не подходит, оно должно быть от 1 до 59"
        }
        return gamingDelayMessage.format(users.joinToString(), minutesToDelay)
    }
}