/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pages.profile.nirms

import models.UserAnswers
import pages.QuestionPage
import play.api.libs.json.JsPath
import utils.Constants.hasNirmsKey

import scala.util.Try

case object HasNirmsPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = hasNirmsKey

  override def cleanup(
    value: Option[Boolean],
    updatedUserAnswers: UserAnswers,
    originalUserAnswers: UserAnswers
  ): Try[UserAnswers] =
    updatedUserAnswers.get(HasNirmsPage) match {
      case Some(false) => updatedUserAnswers.remove(NirmsNumberPage)
      case _           => super.cleanup(value, updatedUserAnswers, originalUserAnswers)
    }

}
