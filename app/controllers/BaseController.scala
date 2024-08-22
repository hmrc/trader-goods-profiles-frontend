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
import models.helper.Journey
import models.requests.DataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{AnyContent, Call, Result}
import services.DataCleansingService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

trait BaseController extends FrontendBaseController with I18nSupport with Logging {

  def logErrorsAndContinue(
    errorMessage: String,
    continueUrl: Call,
    errors: data.NonEmptyChain[ValidationError],
    journeyToCleanse: Journey
  )(implicit
    request: DataRequest[AnyContent],
    dataCleansingService: DataCleansingService
  ): Result = {

    val errorsAsString = errors.toChain.toList.map(_.message).mkString(", ")
    logger.error(s"$errorMessage Missing pages: $errorsAsString")

    dataCleansingService.deleteMongoData(request.userAnswers.id, journeyToCleanse)

    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(continueUrl.url))))
  }

  def logErrorsAndContinue(
    errorMessage: String,
    continueUrl: Call,
    errors: data.NonEmptyChain[ValidationError]
  ): Result = {

    val errorsAsString = errors.toChain.toList.map(_.message).mkString(", ")
    logger.error(s"$errorMessage Missing pages: $errorsAsString")

    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(continueUrl.url))))
  }

}
