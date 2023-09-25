package ru.demmax93.bot.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll
import org.telegram.telegrambots.meta.api.methods.polls.StopPoll
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import ru.demmax93.bot.entity.PollDetails
import java.time.*
import java.util.*

@Service
class GamingNotificationBot(
    @Value("\${telegram.token}") token: String,
    val manualTaskScheduleService: ManualTaskScheduleService,
    val jsonConverterService: JsonConverterService
) : TelegramLongPollingBot(token) {
    private final val myGroupId = -615013028L
    private final val users = setOf("@demmax93", "@Welcome_LjAPb", "@yurazavrazhnov")
    private final val dayOff = "Не играем!"
    private final val gamingTimeMessage = "%s\nСегодня играем в %s!"
    private final val gamingOffMessage = "%s\nСегодня не играем!"
    private final val gamingDelayMessage = "%s\nНачинаем играть на %s минут позже!"
    private final val optionsWorkDays = listOf("20:00", "21:00", "22:00", dayOff)
    private final val optionsWeekDays = listOf("16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", dayOff)
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
    private final val birthDayCongratulation = "%s, С Днём Рождения!) \n%s"
    private final val birthDayCongratulations = setOf(
        "Желаем каток победных больше, киллов сочных побольше, винрейт чтобы рос экспотанциально))",
        "Мы надеемся, что ваш килл-счет будет расти, сочные убийства будут прибавляться, и ваш винрейт будет стремительно расти!))",
        "Пожелаем больше побед в катках, более значительных убийств и винрейт, который будет только возрастать))",
        "Желаем вам много побед в катках, огромное количество успешных убийств и рост вашего винрейта в геометрической прогрессии!))",
        "Пожелаем вам больше побед в рейтинге, много крутых убийств и экспоненциальный рост ваших показателей винрейта))",
        "Пусть рандом приносит вам все больше побед, а киллы становятся более впечатляющими, при этом винрейт стремительно растет))",
        "Желаем вам больше побед в катках, максимум сочных убийств и экспоненциальный рост вашей эффективности))",
        "Пусть у вас будет больше победных каток, убийств с насыщенным опытом и винрейт, который растет в геометрической прогрессии))",
        "Мы надеемся, что вы будете чаще побеждать в катках, совершать более впечатляющие убийства и набирать винрейт экспоненциально))",
        "Пожелаем вам больше побед в катках, более качественных убийств и рост ваших показателей винрейта, идущий вверх в геометрической прогрессии))"
    )

    @Value("\${telegram.botName}")
    private val botName: String = ""

    override fun getBotUsername(): String = botName

    override fun onUpdateReceived(update: Update) {
        if (update.hasPoll()) {
            val poll = update.poll
            if (!poll.isClosed && poll.totalVoterCount == users.size) {
                val details = jsonConverterService.readFromFile()
                if (poll.options.stream().anyMatch { option -> dayOff == option.text && option.voterCount > 0 }) {
                    sendMessage(gamingOffMessage.format(users.joinToString()))
                } else {
                    val gamingTime = poll.options.stream()
                        .filter { option -> dayOff != option.text && option.voterCount > 0 }
                        .max { option1, option2 -> extractHours(option1.text).compareTo(extractHours(option2.text)) }
                    if (gamingTime.isPresent) {
                        sendMessage(gamingTimeMessage.format(users.joinToString(), gamingTime.get().text))
                        var gameTime = LocalDateTime.now(ZoneId.of("Europe/Samara"))
                        val gameHours = extractHours(gamingTime.get().text).toInt()
                        gameTime = gameTime.withHour(gameHours).withMinute(0).withSecond(0)
                        val taskId = manualTaskScheduleService.addNewTask({ sendMessageAndClosePoll(users.joinToString()) },
                            Date.from(gameTime.toInstant(ZoneOffset.of("+4"))))
                        if (details != null) {
                            if (details.taskId != 0) {
                                manualTaskScheduleService.removeTaskFromScheduler(details.taskId)
                            }
                            details.scheduledTime = gameTime
                            details.taskId = taskId
                            jsonConverterService.writeToFile(details)
                        }
                    }
                }
            }
        }
        if (update.hasMessage()) {
            val message = update.message
            val responseText = if (message.hasText()) {
                val messageText = message.text
                when {
                    messageText.startsWith("/help") -> "Доступтные команды:\n" +
                            "/cancel - отменена игровой сессии сегодня\n" +
                            "/delay {number of minutes} - перенос сегодняшней игровой сессии на количество минут указанные после команды (доступно значения от 1 дл 59)."
                    messageText.startsWith("/cancel") -> cancelTodayGame()
                    messageText.startsWith("/delay") -> delayTodayGame(messageText)
                    else -> ""
                }
            } else ""
            if (responseText.isNotEmpty()) {
                sendMessage(responseText)
            }
        }
    }

    @Scheduled(cron = "0 0 18 ? * MON-FRI", zone = "Europe/Samara")
    fun sendWorkDaysPollByCron() {
        sendPoll(optionsWorkDays)
    }

    @Scheduled(cron = "0 0 14 ? * SAT,SUN", zone = "Europe/Samara")
    fun sendWeekEndDaysPollByCron() {
        sendPoll(optionsWeekDays)
    }
    
    @Scheduled(cron = "0 15 12 25 9 ?", zone = "Europe/Samara")
    fun sendBirthDayForRoman() {
        sendMessage(birthDayCongratulation.format("@Welcome_LjAPb", birthDayCongratulations.random()))
    }

    @Scheduled(cron = "0 0 12 18 11 ?", zone = "Europe/Samara")
    fun sendBirthDayForYurii() {
        sendMessage(birthDayCongratulation.format("@yurazavrazhnov", birthDayCongratulations.random()))
    }

    @Scheduled(cron = "0 0 12 1 4 ?", zone = "Europe/Samara")
    fun sendBirthDayForMaks() {
        sendMessage(birthDayCongratulation.format("@demmax93", birthDayCongratulations.random()))
    }

    private fun sendPoll(options: List<String>) {
        val poll = SendPoll.builder()
            .chatId(myGroupId)
            .question(questions.random())
            .options(options)
            .isAnonymous(false)
            .build()
        sendMessage(users.joinToString())
        val response = execute(poll)
        val details = PollDetails(response.messageId, null, 0)
        jsonConverterService.writeToFile(details)
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "Europe/Samara")
    fun cleanUpPollDetails() {
        val details = jsonConverterService.readFromFile()
        if (details != null) {
            closePoll(details)
        }
        jsonConverterService.writeToFile(PollDetails(0, null, 0))
    }

    fun sendMessageAndClosePoll(responseText: String) {
        val details = jsonConverterService.readFromFile()
        if (details != null) {
            closePoll(details)
        }
        sendMessage(responseText)
    }

    private fun closePoll(details: PollDetails) {
        if (details.messageId != 0) {
            val stopPoll = StopPoll(myGroupId.toString(), details.messageId)
            execute(stopPoll)
            details.messageId = 0
            jsonConverterService.writeToFile(details)
        }
    }

    fun sendMessage(responseText: String) {
        val message = SendMessage(myGroupId.toString(), responseText)
        execute(message)
    }

    private fun extractHours(option: String): String {
        return option.replace(":00", "")
    }

    private fun cancelTodayGame(): String {
        val details = jsonConverterService.readFromFile()
            ?: return "Не могу найти что нужно закенцелить, похоже и так не играем!"
        closePoll(details)
        if (details.taskId != 0) {
            manualTaskScheduleService.removeTaskFromScheduler(details.taskId)
            details.taskId = 0
            jsonConverterService.writeToFile(details)
            return gamingOffMessage.format(users.joinToString())
        }
        return "Не могу найти что нужно закенцелить, похоже и так не играем!"
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
        val details = jsonConverterService.readFromFile()
        if (details != null) {
            if (details.taskId != 0) {
                manualTaskScheduleService.removeTaskFromScheduler(details.taskId)
            }
            if (details.scheduledTime != null) {
                val gameTime = details.scheduledTime!!.withMinute(minutesToDelay)
                val newTaskId = manualTaskScheduleService.addNewTask({ sendMessageAndClosePoll(users.joinToString()) },
                    Date.from(gameTime.toInstant(ZoneOffset.of("+4"))))
                details.scheduledTime = gameTime
                details.taskId = newTaskId
                jsonConverterService.writeToFile(details)
            }
        }
        return gamingDelayMessage.format(users.joinToString(), minutesToDelay)
    }
}