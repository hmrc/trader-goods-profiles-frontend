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
import play.api.i18n.I18nSupport
import play.api.mvc.Result
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

trait BaseController extends FrontendBaseController with I18nSupport with Logging {

  def logErrorsAndContinue(
    errorMessage: String,
    continueUrl: String,
    errors: data.NonEmptyChain[ValidationError]
  ): Result = {

    val errorsAsString = errors.toChain.toList.map(_.message).mkString(", ")
    val completeError  = s"$errorMessage Missing pages: $errorsAsString"

    logErrorsAndContinue(completeError, continueUrl)
  }

  def logErrorsAndContinue(
    errorMessage: String,
    continueUrl: String
  ): Result = {

    logger.error(s"$errorMessage")

    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(continueUrl))))
  }

}
