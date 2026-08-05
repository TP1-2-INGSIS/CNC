package org.lexer

import java.io.File
import java.io.BufferedReader

interface ContentManager {
  fun getNextLine() : String; // podria llegar a tener mas metodos
  fun allRead() : Boolean;
}

class FileContent(
  val path: String
) : ContentManager {
  val buffer : BufferedReader

  init { 
    val file = File(path)
    require(file.exists()) {"The path provided does not exists."}
    require(file.isFile()) {"The path provided is not a file."}
    require(file.canRead()) {"The file provided is not available to read."}
    buffer = File(path).bufferedReader()
  }

  override fun getNextLine() : String {
    return buffer.readLine();
  }

  override fun allRead() = buffer.ready()
}

class StrContent(
  var content: String
) : ContentManager {

  override fun getNextLine() : String {
    val result = content;
    content = ""
    return result
  }

  override fun allRead() = content.length == 0
}
