package com.mysema.scalagen

import org.junit.Test
import org.junit.Assert._
import java.util.Collections

class ScalaVersionTest {
  
  @Test
  def VersionOrdering {
    assertTrue(Scala213 > Scala212)
    assertTrue(Scala212 > Scala211)
    assertTrue(Scala211 > Scala210)
    assertTrue(Scala210 > Scala29)
  }
  
}