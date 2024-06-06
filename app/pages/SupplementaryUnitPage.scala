package pages

import play.api.libs.json.JsPath

case object SupplementaryUnitPage extends QuestionPage[Int] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "supplementaryUnit"
}
