/*
 * Copyright (C) 2011, Mysema Ltd
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

import java.io.{ ByteArrayInputStream, File }
import java.util.ArrayList

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.{ CompilationUnit, ImportDeclaration }
import org.apache.commons.io.FileUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

object Converter {
  
  /**
   * default instance for Converter type
   */
  lazy val instance = instance29
  
  /**
   * Converter targeting scala 2.9
   */
  lazy val instance29 = createConverter(Scala29)
  
  /**
   * Converter targeting scala 2.10
   */
  lazy val instance210 = createConverter(Scala210)
  
  /**
   * Converter targeting scala 2.11
   */
  lazy val instance211 = createConverter(Scala211)
  
  def getInstance(version: ScalaVersion) = version match {
    case Scala29 => instance29
    case Scala210 => instance210
    case Scala211 => instance211
  }
  
  /**
   * Converter for the current runtime scala version
   */
  def getInstance(): Converter = {
    getInstance(ScalaVersion.current)
  }
  
  private def createConverter(version: ScalaVersion) = {
    new Converter("UTF-8",List[UnitTransformer](
      Rethrows,
      VarToVal,
      Synchronized,
      RemoveAsserts, 
      new Annotations(version),
      Enums,
      Primitives,
      SerialVersionUID,
      ControlStatements, 
      CompanionObject,
      Underscores,
      Setters,
      new BeanProperties(version), 
      Properties,
      Constructors, 
      Initializers,
      SimpleEquals))
  }
  
}

/**
 * Converter converts Java sources into Scala sources
 */
class Converter(encoding: String, transformers: List[UnitTransformer]) {
    
  def convert(inFolder: File, outFolder: File) {
    val inFolderLength = inFolder.getPath.length + 1
    val outFolderLength = outFolder.getPath.length + 1
    val targetFiles = getFilesWithExtension(inFolder, ".java")
    val completeFilePaths = getFilesWithExtension(outFolder, ".scala")
      .map(_.getAbsolutePath.substring(outFolderLength))
    val remainingFiles = targetFiles
      .filterNot(file => completeFilePaths.contains(toOut(inFolderLength, outFolder, file).getAbsolutePath.substring(outFolderLength)))
    val inToOut = remainingFiles
      .map(file => (file, toOut(inFolderLength, outFolder, file)))

    // create out folders
    inToOut.foreach(_._2.getParentFile.mkdirs() )  
    inToOut.foreach{ case (in,out) => convertFile(in,out, true) }
  }
  
  def convertFile(in: File, out: File, continueAfterError: Boolean = true) {
    System.out.print(s"${in.getAbsolutePath} -> ${out.getAbsolutePath}")
    try {
      val compilationUnit = JavaParser.parse(in, encoding)
      System.out.print(" .")
      val sources = toScala(compilationUnit)
      System.out.print(".")
      FileUtils.writeStringToFile(out, sources, "UTF-8")
      System.out.println(".")
    } catch {
      case e: Exception =>
        if (continueAfterError) {
          System.out.println("")
          System.out.println(s"Skipping ${in.getPath} due to ${e.getMessage}")
        } else {
          throw new RuntimeException("Caught Exception for " + in.getPath, e)
        }
    }
  }
  
  def convert(javaSource: String, settings: ConversionSettings = ConversionSettings()): String = {
    val compilationUnit = JavaParser.parse(new ByteArrayInputStream(javaSource.getBytes(encoding)), encoding)
    try {
      toScala(compilationUnit, settings)
    } catch {
      case e: Exception => throw new RuntimeException("Caught Exception", e)
    }
  }
  
  def toScala(unit: CompilationUnit, settings: ConversionSettings = ConversionSettings()): String = {
    if (unit.getImports == null) {
      unit.setImports(new ArrayList[ImportDeclaration]())  
    }    
    val transformed = transformers.foldLeft(unit) { case (u,t) => t.transform(u) }
    val visitor = new ScalaStringVisitor(settings)
    val convertedCode = transformed.accept(visitor, new ScalaStringVisitor.Context())

    runWithTimeout(5) { org.scalafmt.Scalafmt.format(convertedCode).get }
      .getOrElse(throw new RuntimeException("Scalagen failed."))
  }
  
  private def toOut(inFolderLength: Int, outFolder: File, in: File): File = {
    val offset = if (in.getName == "package-info.java") 10 else 5
    new File(outFolder, in.getPath.substring(inFolderLength, in.getPath.length-offset)+".scala")
  }
  
  private def getFilesWithExtension(file: File, ext: String): Seq[File] = {
    if (file.isDirectory) {
      file.listFiles.toSeq
        .filter(f => f.isDirectory || f.getName.endsWith(ext))
        .flatMap(f => getFilesWithExtension(f, ext))
    } else {
      if (file.exists) file :: Nil else Nil
    }
  }

  def runWithTimeout[T](timeout: Long)(f: => T): Option[T] = {
    Option(Await.result(Future(f), timeout seconds))
  }

}
