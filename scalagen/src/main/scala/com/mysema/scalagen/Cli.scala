/*
 * Copyright (C) 2011, James McMahon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mysema.scalagen

import java.io.File
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{ Level, LoggerContext }
import ch.qos.logback.core.ConsoleAppender
import org.slf4j.LoggerFactory

/**
 * Simple harness to facilitate running scalagen from the command line
 */
object Cli {
  val usage = "USAGE: scalagen <src-directory> <target-directory>"

  def main(args: Array[String]) {
    val logger = createLoggerFor("ldap-test")
    if (args.length != 2) {
      println(usage)
      return
    }

    val in = new File(args(0))
    if (in.exists) {
      val out = new File(args(1))
      Converter.instance.convert(in, out)
    }
  }

  def createLoggerFor(string: String, file: Option[String] = None): ch.qos.logback.classic.Logger = {
    val ple = new PatternLayoutEncoder()
    val lc = LoggerFactory.getILoggerFactory().asInstanceOf[LoggerContext]
    ple.setPattern("%date %level [%thread] %logger{10} [%file:%line]\t%msg%n")
    ple.setContext(lc)
    ple.start()

    val appender = new ConsoleAppender[ILoggingEvent]()
    appender.setEncoder(ple)
    appender.setContext(lc)
    appender.start()

    val logger = LoggerFactory.getLogger(string).asInstanceOf[ch.qos.logback.classic.Logger]
    logger.addAppender(appender)
    logger.setLevel(Level.DEBUG)
    logger.setAdditive(false)

    logger
  }

}
