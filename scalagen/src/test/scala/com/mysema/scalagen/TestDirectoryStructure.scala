package com.mysema.scalagen

import java.io.File

object TestDirectoryStructure {
  val SCALA_TEST_DIR_NAME = s"${new java.io.File(".").getCanonicalPath}/src/test/scala"
  val EXAMPLE_FILE_DIR = new File(s"$SCALA_TEST_DIR_NAME/com/mysema/examples")
}
