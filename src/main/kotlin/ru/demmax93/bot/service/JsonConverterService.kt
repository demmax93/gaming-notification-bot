package ru.demmax93.bot.service

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Service
import ru.demmax93.bot.entity.PollDetails
import java.io.File
import java.io.IOException

@Service
class JsonConverterService {

    fun writeToFile(pollDetails: PollDetails) {
        val path = "./details.json"
        try {
            val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            mapper.writeValue(File(path), pollDetails)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun readFromFile(): PollDetails {
        val path = "./details.json"
        try {
            val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
            return mapper.readValue(File(path))
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return PollDetails(0, null, 0)
    }

}