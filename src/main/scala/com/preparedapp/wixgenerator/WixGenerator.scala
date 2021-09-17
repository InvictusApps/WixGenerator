package com.preparedapp.wixgenerator

import java.io.File
import java.util.UUID
import WixGenerator._

import java.nio.file.Files
import scala.io.Source

case class WixGenerator(appName: String,
                        appId: UUID,
                        organization: String,
                        directory: File,
                        upgradeCode: UUID = uuid(),
                        files: List[WixFile]) {
  private val absolutePath: String = directory.getAbsolutePath
  private val fileMap: Map[String, WixFile] = files.map { f =>
    f.path -> f
  }.toMap

  private var xml: String = XML.Base
  private var componentRefs: List[String] = Nil
  private var idCounter: Int = 0

  private def nextId: String = {
    idCounter += 1
    s"id$idCounter"
  }

  def generate(): Unit = {
    deleteWixFiles()

    val directories = process(directory, depth = 4)
    update("files", directories)
    update("appName", appName)
    update("appId", appId.s)
    update("uuid", uuid().s)
    update("organization", organization)

    val refs = componentRefs.reverse.map { refId =>
      var s = XML.ComponentRef
      s = update("componentId", refId, Some(s))
      s
    }
    update("components", refs.mkString("\n"))

    save()
  }

  private def deleteWixFiles(): Unit = {
    val extensions = Set("msi", "wixobj", "wxs", "wixpdb")
    val fileNames = extensions.map(ext => s"$appName.$ext")
    directory.listFiles().foreach { f =>
      if (fileNames.contains(f.getName)) {
        f.delete()
      }
    }
  }

  private def process(directory: File, depth: Int): String = {
    val padding = "".padTo(depth, '\t')
    var xml = s"$padding${XML.Directory}"
    val directoryId = if (directory == this.directory) "INSTALLDIR" else nextId
    xml = update("directoryId", directoryId, Some(xml))
    xml = update("directoryName", directory.getName, Some(xml))
    xml = xml.replace("\n", s"\n$padding")
    val content = directory.listFiles().filter(exclusions).map { file =>
      if (file.isDirectory) {
        process(file, depth + 1)
      } else {
        component(file, depth + 1)
      }
    }.mkString("\n") match {
      case s if s.nonEmpty => s"\n$s"
      case _ => ""
    }
    xml = update("content", content, Some(xml))
    xml
  }

  private def exclusions(file: File): Boolean = file.getName.toLowerCase match {
    case s if s.endsWith(".wxs") || s.endsWith(".wixobj") => false
    case _ => true
  }

  private def component(file: File, depth: Int): String = {
    val padding = "".padTo(depth, '\t')
    var xml = s"$padding${XML.Component}"
    val relativePath = file.relativePath
    val wixFile = fileMap.getOrElse(relativePath, WixFile(relativePath))
    val componentId = nextId
    val fileId = nextId
    xml = update("componentId", componentId, Some(xml))
    xml = update("uuid", uuid().s, Some(xml))
    xml = update("win64", if (wixFile.win64) "yes" else "no", Some(xml))
    xml = update("fileId", fileId, Some(xml))
    xml = update("fileName", file.getName, Some(xml))
    xml = update("fileRelativePath", relativePath.replace("\\", "\\\\"), Some(xml))
    var fileContent = List.empty[String]

    def shortcut(shortcut: Shortcut, location: String): String = {
      var s = s"\t\t${XML.Shortcut}"
      s = update("id", nextId, Some(s))
      s = update("directory", location, Some(s))
      s = update("name", shortcut.name, Some(s))
      s = update("icon", shortcut.icon, Some(s))
      s
    }

    wixFile.startMenuShortcut.foreach { s =>
      fileContent = shortcut(s, "ProgramMenuDir") :: fileContent
    }
    wixFile.desktopShortcut.foreach { s =>
      fileContent = shortcut(s, "DesktopFolder") :: fileContent
    }
    if (fileContent.nonEmpty) {
      xml = update("fileContent", fileContent.reverse.mkString("\n", "\n", "\n\t"), Some(xml))
    } else {
      xml = update("fileContent", "", Some(xml))
    }

    var componentContent = List.empty[String]
    if (wixFile.autoRun) {
      var s = XML.RegistryKey
      s = update("root", "HKCU", Some(s))
      s = update("key", "Software\\Microsoft\\Windows\\CurrentVersion\\Run", Some(s))
      s = update("type", "string", Some(s))
      s = update("name", file.getName, Some(s))
      s = update("value", s"[#${file.getName}]", Some(s))
      componentContent = s :: componentContent
    }
    if (componentContent.nonEmpty) {
      xml = update("componentContent", componentContent.reverse.mkString("\n", "\n", ""), Some(xml))
    } else {
      xml = update("componentContent", "", Some(xml))
    }

    xml = xml.replace("\n", s"\n$padding")
    componentRefs = componentId :: componentRefs
    xml
  }

  private def update(key: String, value: => String, updating: Option[String] = None): String = try {
    val updated = ("[\\$]" + key).r.replaceAllIn(updating.getOrElse(xml), _ => {
      value
    })
    if (updating.isEmpty) xml = updated
    updated
  } catch {
    case t: Throwable => throw new RuntimeException(s"Error Replacing: $key in ${updating.getOrElse(xml)} -> $value", t)
  }

  private def save(): Unit = {
    val file = new File(directory, s"$appName.wxs")
    Files.write(file.toPath, xml.getBytes("UTF-8"))
  }

  private implicit class FileExtras(file: File) {
    def relativePath: String = file.getAbsolutePath.substring(absolutePath.length + 1)
  }
}

object WixGenerator {
  object XML {
    lazy val Base: String = load("base.xml")
    lazy val Directory: String = load("directory.xml")
    lazy val Component: String = load("component.xml")
    lazy val ComponentRef: String = load("component-ref.xml")
    lazy val Shortcut: String = load("shortcut.xml")
    lazy val RegistryKey: String = load("registry-key.xml")
  }

  def uuid(): UUID = UUID.randomUUID()

  private def load(fileName: String): String = {
    val source = Source.fromURL(getClass.getClassLoader.getResource(fileName))
    try {
      source.mkString
    } finally {
      source.close()
    }
  }

  implicit class UUIDExtras(uuid: UUID) {
    def s: String = uuid.toString.toUpperCase
  }
}

case class WixFile(path: String,
                   startMenuShortcut: Option[Shortcut] = None,
                   desktopShortcut: Option[Shortcut] = None,
                   autoRun: Boolean = false,
                   win64: Boolean = true)

case class Shortcut(name: String, icon: String)