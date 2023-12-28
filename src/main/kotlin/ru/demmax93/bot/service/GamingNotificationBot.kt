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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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
    private final val gamingDelayMessage = "%s\nИгра перенесена! Начинаем играть в %s!"
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
    private final val happyNewYearCongratulation = "%s, С Новым Годом!) \n%s"
    private final val congratulations = setOf(
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
    private final val hourAndMinutesFormatter = DateTimeFormatter.ofPattern("HH:mm")

    @Value("\${telegram.botName}")
    private val botName: String = ""

    override fun getBotUsername(): String = botName

    override fun onUpdateReceived(update: Update) {
        if (update.hasPoll()) {
            val poll = update.poll
            if (!poll.isClosed && poll.totalVoterCount == users.size) {
                val details = jsonConverterService.readFromFile()
                if (poll.options.stream().anyMatch { option -> dayOff == option.text && option.voterCount > 0 }) {
                    removeTask(details)
                    updateJsonFile(details, details.messageId, details.scheduledTime, 0)
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
                        val taskId = manualTaskScheduleService.addNewTask(
                            { sendMessage(users.joinToString()) },
                            Date.from(gameTime.toInstant(ZoneOffset.of("+4")))
                        )
                        removeTask(details)
                        updateJsonFile(details, details.messageId, gameTime, taskId)
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
                            "/delay {number of minutes} - перенос сегодняшней игровой сессии на количество минут указанные после команды (значение должно быть не равно 0)." +
                            "При нескольких переносах, время будет суммироваться, т.е. перенос на 10 минит и ещё на 15 минут, датут в общей сложности перенос на 25 минут." +
                            "Так же можно вычитать минуты."
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

    @Scheduled(cron = "0 0 12 25 9 ?", zone = "Europe/Samara")
    fun sendBirthDayForRoman() {
        sendMessage(birthDayCongratulation.format("@Welcome_LjAPb", congratulations.random()))
    }

    @Scheduled(cron = "0 0 12 18 11 ?", zone = "Europe/Samara")
    fun sendBirthDayForYurii() {
        sendMessage(birthDayCongratulation.format("@yurazavrazhnov", congratulations.random()))
    }

    @Scheduled(cron = "0 0 12 1 4 ?", zone = "Europe/Samara")
    fun sendBirthDayForMaks() {
        sendMessage(birthDayCongratulation.format("@demmax93", congratulations.random()))
    }

    @Scheduled(cron = "0 0 0 1 1 ?", zone = "Europe/Samara")
    fun sendHappyNewYear() {
        sendMessage(happyNewYearCongratulation.format(users.joinToString(), congratulations.random()))
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
        updateJsonFile(jsonConverterService.readFromFile(), response.messageId, null, 0)
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "Europe/Samara")
    fun cleanUpPollDetails() {
        val details = jsonConverterService.readFromFile()
        if (details.messageId != 0) {
            val stopPoll = StopPoll(myGroupId.toString(), details.messageId)
            execute(stopPoll)
        }
        updateJsonFile(details,0, null, 0)
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
        if (details.taskId == 0) {
            return "Не могу найти что нужно закенцелить, похоже и так не играем!"
        }
        removeTask(details)
        updateJsonFile(details, details.messageId, details.scheduledTime, 0)
        return gamingOffMessage.format(users.joinToString())
    }

    private fun delayTodayGame(text: String): String {
        val minutesToDelay = try {
            text.removePrefix("/delay").trim().toLong()
        } catch (exception: Exception) {
            return "Не смог разобрать число которое вы ввели, попробуйте ещё раз"
        }
        if (minutesToDelay == 0L) {
            return "Введенное число не подходит, оно должно быть не равно 0"
        }
        val details = jsonConverterService.readFromFile()
        if (details.scheduledTime == null) {
            return "Не могу найти время которое нужно перенести, похоже ещё не определились!"
        }
        removeTask(details)
        val gameTime = details.scheduledTime!!.plusMinutes(minutesToDelay)
        val newTaskId = manualTaskScheduleService.addNewTask(
            { sendMessage(users.joinToString()) },
            Date.from(gameTime.toInstant(ZoneOffset.of("+4")))
        )
        updateJsonFile(details, details.messageId, gameTime, newTaskId)
        return gamingDelayMessage.format(users.joinToString(), gameTime.format(hourAndMinutesFormatter))
    }

    private fun updateJsonFile(
        details: PollDetails,
        newMessageId: Int,
        newScheduledTime: LocalDateTime?,
        newTaskId: Int
    ) {
        details.messageId = newMessageId
        details.scheduledTime = newScheduledTime
        details.taskId = newTaskId
        jsonConverterService.writeToFile(details)
    }

    private fun removeTask(details: PollDetails) {
        if (details.taskId == 0) {
            return
        }
        manualTaskScheduleService.removeTaskFromScheduler(details.taskId)
    }
}