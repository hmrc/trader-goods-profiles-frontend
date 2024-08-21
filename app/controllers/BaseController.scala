package controllers

import cats.data
import com.google.inject.Inject
import models.ValidationError
import models.helper.Journey
import models.requests.DataRequest
import play.api.i18n.Lang.logger
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Call, Result}
import services.DataCleansingService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

trait BaseController {

  @Inject var dataCleansingService: DataCleansingService = _

  def logErrorsAndContinue(
    errorMessage: String,
    continueUrl: Call,
    errors: data.NonEmptyChain[ValidationError],
    journeyToCleanse: Journey
  )(implicit request: DataRequest[AnyContent]): Result = {

    val errorsAsString = errors.toChain.toList.map(_.message).mkString(", ")
    logger.error(s"$errorMessage Missing pages: $errorsAsString")

    dataCleansingService.deleteMongoData(request.userAnswers.id, journeyToCleanse)

    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(continueUrl.url))))
  }

  def logErrorsAndContinue(
    errorMessage: String,
    continueUrl: Call,
    errors: data.NonEmptyChain[ValidationError],
  ): Result = {

    val errorsAsString = errors.toChain.toList.map(_.message).mkString(", ")
    logger.error(s"$errorMessage Missing pages: $errorsAsString")

    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(RedirectUrl(continueUrl.url))))
  }

}