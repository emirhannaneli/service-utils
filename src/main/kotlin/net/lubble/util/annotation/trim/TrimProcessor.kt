package net.lubble.util.annotation.trim

import com.fasterxml.jackson.databind.util.StdConverter
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

class TrimProcessor: StdConverter<String, String?>(){
    companion object {
        fun handle(value: String) = value.trim()
    }

    override fun convert(p0: String?): String? {
        return p0?.let { handle(it) }
    }
}