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
                        upgradeCode: UUID = uuid()) {
  private var xml: String = BaseXML

  def generate(): Unit = {
    deleteWixFiles()

    update("appName", appName)
    update("appId", appId.s)
    update("uuid", uuid().s)
    update("organization", organization)
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

  private def update(key: String, value: => String): Unit = xml = ("[\\$]" + key).r.replaceAllIn(xml, _ => {
    value
  })

  private def save(): Unit = {
    val file = new File(directory, s"$appName.wxs")
    Files.write(file.toPath, xml.getBytes("UTF-8"))
  }
}

object WixGenerator {
  lazy val BaseXML: String = load("base.xml")

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
                   startMenuShortcut: Boolean = false,
                   desktopShortcut: Boolean = false,
                   autoRun: Boolean = false,
                   win64: Boolean = true)