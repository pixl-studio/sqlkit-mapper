package sqlkit.mapper


object StringHelper {

  implicit class StringUtil(s:String) {

    def toCamelCase(sep:Char='_') = {
      s"${sep}([a-z\\d])".r.replaceAllIn(s, {m =>
        m.group(1).toUpperCase()
      })
    }

    def toPascalCase(sep:Char='_') = {
      s.split(sep).map(_.toLowerCase.capitalize).mkString
    }
  }
}
