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

package controllers

import cats.data
import logging.Logging
import models.ValidationError
import models.requests.DataRequest
import pages.QuestionPage
import pages.categorisation.ReassessmentPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.Reads
import play.api.mvc.{AnyContent, Call, Result}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

trait BaseController extends FrontendBaseController with I18nSupport with Logging {

  def logErrorsAndContinue(
    errorMessage: String,
    continueCall: Call,
    errors: data.NonEmptyChain[ValidationError]
  ): Result = {

    val errorsAsString = errors.toChain.toList.map(_.message).mkString(", ")
    val completeError  = s"$errorMessage Missing pages: $errorsAsString"

    logErrorsAndContinue(completeError, continueCall)
  }

  def logErrorsAndContinue(
    errorMessage: String,
    continueCall: Call
  ): Result = {

    logger.warn(s"$errorMessage")

    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(continueCall.url))))
  }

  def prepareForm[T, T2](page: QuestionPage[T], form: Form[T2])(implicit
    request: DataRequest[AnyContent],
    reads: Reads[T2]
  ): Form[T2] =
    if (page.isInstanceOf[ReassessmentPage]) {
      request.userAnswers.get(page.asInstanceOf[ReassessmentPage]) match {
        case Some(value) => form.fill(value.answer.asInstanceOf[T2])
        case None        => form
      }
    } else {
      request.userAnswers.get(page.asInstanceOf[QuestionPage[T2]]).map(form.fill).getOrElse(form)
    }

  def getMessage(key: String)(implicit messages: Messages): String = messages(key)
}
