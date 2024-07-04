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

import controllers.actions._
import forms.HasCorrectGoodsFormProvider
import models.ott.CategorisationInfo
import models.requests.DataRequest
import models.{Mode, UserAnswers}
import navigation.Navigator
import pages.{AssessmentPage, HasCorrectGoodsLongerCommodityCodePage, HasCorrectGoodsPage, InconsistentUserAnswersException}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{CommodityQuery, LongerCommodityQuery, RecordCategorisationsQuery}
import repositories.SessionRepository
import services.CategorisationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Constants.firstAssessmentIndex
import views.html.HasCorrectGoodsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class HasCorrectGoodsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: HasCorrectGoodsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: HasCorrectGoodsView,
  categorisationService: CategorisationService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent]                                      = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(HasCorrectGoodsPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    val submitAction = routes.HasCorrectGoodsController.onSubmit(mode)
    request.userAnswers.get(CommodityQuery) match {
      case Some(commodity) => Ok(view(preparedForm, commodity, submitAction))
      case None            => Redirect(routes.JourneyRecoveryController.onPageLoad().url)
    }
  }
  // TODO - this is still not functional, it is just to create the url. Implement this properly
  def onPageLoadLongerCommodityCode(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(HasCorrectGoodsLongerCommodityCodePage(recordId)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      val submitAction = routes.HasCorrectGoodsController.onSubmitLongerCommodityCode(mode, recordId)
      request.userAnswers.get(LongerCommodityQuery(recordId)) match {
        case Some(commodity) => Ok(view(preparedForm, commodity, submitAction))
        case None            => Redirect(routes.JourneyRecoveryController.onPageLoad().url)
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent]                                      = (identify andThen getData andThen requireData).async {
    implicit request =>
      val submitAction = routes.HasCorrectGoodsController.onSubmit(mode)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(CommodityQuery) match {
              case Some(commodity) => Future.successful(BadRequest(view(formWithErrors, commodity, submitAction)))
              case None            => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HasCorrectGoodsPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasCorrectGoodsPage, mode, updatedAnswers))
        )
  }
  // TODO - this is still not functional, it is just to create the url. Implement this properly
  def onSubmitLongerCommodityCode(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val submitAction = routes.HasCorrectGoodsController.onSubmitLongerCommodityCode(mode, recordId)

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            //TODO - use the UpdateCommdityQuery (with recordId) here when available
            request.userAnswers.get(LongerCommodityQuery(recordId)) match {
              case Some(commodity) => Future.successful(BadRequest(view(formWithErrors, commodity, submitAction)))
              case None            => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
            },
          value =>
            if (value) {
              processNewCommodityCode(mode, recordId, request, value)
            } else {
              for {
                updatedAnswers <-
                  Future.fromTry(request.userAnswers.set(HasCorrectGoodsLongerCommodityCodePage(recordId), value))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(
                navigator.nextPage(HasCorrectGoodsLongerCommodityCodePage(recordId), mode, updatedAnswers)
              )
            }
        )
    }

  private def processNewCommodityCode(mode: Mode, recordId: String, request: DataRequest[AnyContent], value: Boolean)(
    implicit hc: HeaderCarrier
  ) =
    for {
      // Save the user's answer
      updatedAnswers               <-
        Future.fromTry(request.userAnswers.set(HasCorrectGoodsLongerCommodityCodePage(recordId), value))

      // Find out what the assessment details for the old (shorter) code was
      oldCommodityCategorisation   <-
        Future.fromTry(Try(updatedAnswers.get(RecordCategorisationsQuery).get.records(recordId)))

      // Update the categorisation with the new value details for future categorisation attempts
      updatedCategorisationAnswers <-
        categorisationService
          .updateCategorisationWithNewCommodityCode(request.copy(userAnswers = updatedAnswers), recordId)

      // And then get the new assessment details
      newCommodityCategorisation   <-
        Future
          .fromTry(Try(updatedCategorisationAnswers.get(RecordCategorisationsQuery).get.records(recordId)))

      // We then have both assessments so can decide if to recategorise or not
      needToRecategorise            = isRecategorisationNeeded(oldCommodityCategorisation, newCommodityCategorisation)

      // If we are recategorising we need to remove the old assessments so they don't prepopulate / break CYA
      updatedAnswersCleanedUp <-
        Future
          .fromTry(cleanupOldAssessmentAnswers(updatedCategorisationAnswers, recordId, needToRecategorise))

      _ <- sessionRepository.set(updatedAnswersCleanedUp)
    } yield Redirect(
      navigator.nextPage(
        HasCorrectGoodsLongerCommodityCodePage(recordId, needToRecategorise = needToRecategorise),
        mode,
        updatedAnswersCleanedUp
      )
    )

  private def isRecategorisationNeeded(
    oldCommodityCategorisation: CategorisationInfo,
    newCommodityCategorisation: CategorisationInfo
  ) =
    !oldCommodityCategorisation.categoryAssessments.equals(newCommodityCategorisation.categoryAssessments) ||
      oldCommodityCategorisation.measurementUnit.isDefined || newCommodityCategorisation.measurementUnit.isDefined

  private def cleanupOldAssessmentAnswers(
    userAnswers: UserAnswers,
    recordId: String,
    needToRecategorise: Boolean
  ): Try[UserAnswers] =
    if (needToRecategorise) {
      (for {
        recordQuery        <- userAnswers.get(RecordCategorisationsQuery)
        categorisationInfo <- recordQuery.records.get(recordId)
        count               = categorisationInfo.categoryAssessments.size
        //Go backwards to avoid recursion issues
        rangeToRemove       = (firstAssessmentIndex to count + 1).reverse
      } yield rangeToRemove.foldLeft[Try[UserAnswers]](Success(userAnswers)) { (acc, currentIndexToRemove) =>
        acc.flatMap(_.remove(AssessmentPage(recordId, currentIndexToRemove)))
      }).getOrElse(
        Failure(new InconsistentUserAnswersException(s"Could not find category assessments"))
      )
    } else {
      Success(userAnswers)
    }

}
