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
import pages._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{CommodityQuery, CommodityUpdateQuery, LongerCommodityQuery, RecategorisingQuery, RecordCategorisationsQuery}
import repositories.SessionRepository
import services.CategorisationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.HasCorrectGoodsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

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

  def onPageLoadCreate(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(HasCorrectGoodsPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      val submitAction = routes.HasCorrectGoodsController.onSubmitCreate(mode)
      request.userAnswers.get(CommodityQuery) match {
        case Some(commodity) => Ok(view(preparedForm, commodity, submitAction))
        case None            => Redirect(routes.JourneyRecoveryController.onPageLoad().url)
      }
  }

  def onPageLoadUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val preparedForm = request.userAnswers.get(HasCorrectGoodsCommodityCodeUpdatePage(recordId)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      val submitAction = routes.HasCorrectGoodsController.onSubmitUpdate(mode, recordId)
      request.userAnswers.get(CommodityUpdateQuery(recordId)) match {
        case Some(commodity) => Ok(view(preparedForm, commodity, submitAction))
        case None            => Redirect(routes.JourneyRecoveryController.onPageLoad().url)
      }
    }

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

  def onSubmitCreate(mode: Mode): Action[AnyContent]                                = (identify andThen getData andThen requireData).async {
    implicit request =>
      val submitAction = routes.HasCorrectGoodsController.onSubmitCreate(mode)
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
  def onSubmitLongerCommodityCode(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val submitAction = routes.HasCorrectGoodsController.onSubmitLongerCommodityCode(mode, recordId)

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
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

  def onSubmitUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val submitAction = routes.HasCorrectGoodsController.onSubmitUpdate(mode, recordId)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(CommodityUpdateQuery(recordId)) match {
              case Some(commodity) => Future.successful(BadRequest(view(formWithErrors, commodity, submitAction)))
              case None            => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
            },
          value =>
            for {
              updatedAnswers <-
                Future.fromTry(request.userAnswers.set(HasCorrectGoodsCommodityCodeUpdatePage(recordId), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HasCorrectGoodsCommodityCodeUpdatePage(recordId), mode, updatedAnswers))
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

      areWeRecategorisingAlready    = updatedCategorisationAnswers.get(RecategorisingQuery(recordId)).getOrElse(false)
      // We then have both assessments so can decide if to recategorise or not
      // If already recategorising then they've probably gone back and we need it to behave when going forward again
      needToRecategorise            =
        isRecategorisationNeeded(oldCommodityCategorisation, newCommodityCategorisation) || areWeRecategorisingAlready

      // If we are recategorising we need to remove the old assessments so they don't prepopulate / break CYA
      updatedAnswersCleanedUp      <-
        Future
          .fromTry(
            cleanupOldAssessmentAnswersForRecategorisation(
              updatedCategorisationAnswers,
              recordId,
              needToRecategorise,
              oldCommodityCategorisation,
              newCommodityCategorisation
            )
          )
      // Any answered Supplementary Unit needs to be removed if different on new commodity
      updatedAnswersSuppUnit       <- Future.fromTry(
                                        cleanupSupplementaryUnit(
                                          updatedAnswersCleanedUp,
                                          recordId,
                                          oldCommodityCategorisation.measurementUnit,
                                          newCommodityCategorisation.measurementUnit
                                        )
                                      )
      updatedAnswersRecategorising <-
        Future.fromTry(updatedAnswersSuppUnit.set(RecategorisingQuery(recordId), needToRecategorise))
      _                            <- sessionRepository.set(updatedAnswersRecategorising)
    } yield Redirect(
      navigator.nextPage(
        HasCorrectGoodsLongerCommodityCodePage(recordId, needToRecategorise = needToRecategorise),
        mode,
        updatedAnswersRecategorising
      )
    )

  private def isRecategorisationNeeded(
    oldCommodityCategorisation: CategorisationInfo,
    newCommodityCategorisation: CategorisationInfo
  ) =
    !oldCommodityCategorisation.categoryAssessments.equals(newCommodityCategorisation.categoryAssessments)

  private def cleanupOldAssessmentAnswersForRecategorisation(
    userAnswers: UserAnswers,
    recordId: String,
    needToRecategorise: Boolean,
    oldCommodityCategorisation: CategorisationInfo,
    newCommodityCategorisation: CategorisationInfo
  ): Try[UserAnswers] =
    if (needToRecategorise) {
      categorisationService.updatingAnswersForRecategorisation(
        userAnswers,
        recordId,
        oldCommodityCategorisation,
        newCommodityCategorisation
      )
    } else {
      Success(userAnswers)
    }

  private def cleanupSupplementaryUnit(
    userAnswers: UserAnswers,
    recordId: String,
    oldMeasurementUnit: Option[String],
    newMeasurementUnit: Option[String]
  ): Try[UserAnswers] =
    if (oldMeasurementUnit != newMeasurementUnit) {
      userAnswers.remove(HasSupplementaryUnitPage(recordId))
    } else {
      Success(userAnswers)
    }

}
