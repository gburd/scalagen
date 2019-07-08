package com.mysema.scalagen

case class ConversionSettings(
                               splitLongLines: Boolean = true,
                               lineMaxLength: Integer = 50
                             )

object ConversionSettings {
  def defaultSettings = ConversionSettings()
}