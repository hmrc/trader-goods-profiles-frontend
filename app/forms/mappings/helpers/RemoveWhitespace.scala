package forms.mappings.helpers

object RemoveWhitespace {

  def removeWhitespace: String => String = _.filterNot(_.isWhitespace)

}
