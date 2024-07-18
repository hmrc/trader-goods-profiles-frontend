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

import connectors.TraderProfileConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.AssessmentFormProvider
import logging.Logging
import models.AssessmentAnswer.Exemption
import models.ott.CategorisationInfo
import models.requests.DataRequest
import models.{Mode, UserAnswers, ott}
import navigation.Navigator
import pages.AssessmentPage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.RecordCategorisationsQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AssessmentView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AssessmentController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: AssessmentFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: AssessmentView,
  traderProfileConnector: TraderProfileConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode, recordId: String, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val categorisationResult = for {
        traderProfile      <- traderProfileConnector.getTraderProfile(request.eori)
        recordQuery         = request.userAnswers.get(RecordCategorisationsQuery)
        categorisationInfo <- Future.fromTry(Try(recordQuery.get.records(recordId)))
        userHasNiphls       = traderProfile.niphlNumber.isDefined
        isNiphlsAnAnswer    = categorisationInfo.categoryAssessments(index).isNiphlsAnswer
        updatedAnswers     <-
          setAssessmentAnswerToBeNiphls(recordId, index, request.userAnswers, isNiphlsAnAnswer, userHasNiphls)
        _                  <- sessionRepository.set(updatedAnswers)
      } yield {
        val exemptions                  = categorisationInfo.categoryAssessments(index).exemptions
        val isNiphlsCategory2Assessment = categorisationInfo.categoryAssessments(index).isEmptyCat2Assessment &&
          categorisationInfo.isNiphls && userHasNiphls

        (userHasNiphls, isNiphlsAnAnswer, isNiphlsCategory2Assessment, exemptions.isEmpty) match {
          case (true, true, _, _)     =>
            handleAppliedNiphlsAssessment(mode, recordId, index, updatedAnswers)
          case (true, false, true, _) =>
            redirectToCya(mode, recordId, index, updatedAnswers)
          case (false, true, _, _)    =>
            // Should have not been allowed to get this far. Should not have loaded this page
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          case (false, _, _, true)    =>
            redirectToCya(mode, recordId, index, updatedAnswers)
          case _                      =>
            displayPage(
              mode,
              recordId,
              index,
              updatedAnswers,
              categorisationInfo,
              exemptions
            )
        }

      }

      categorisationResult.flatMap(identity).recover { case _ =>
        Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }

  private def redirectToCya(mode: Mode, recordId: String, index: Int, updatedAnswers: UserAnswers) =
    Future.successful(
      Redirect(
        navigator.nextPage(
          AssessmentPage(recordId, index, shouldRedirectToCya = true),
          mode,
          updatedAnswers
        )
      )
    )

  private def handleAppliedNiphlsAssessment(mode: Mode, recordId: String, index: Int, updatedAnswers: UserAnswers) =
    Future.successful(Redirect(navigator.nextPage(AssessmentPage(recordId, index), mode, updatedAnswers)))

  private def displayPage(
    mode: Mode,
    recordId: String,
    index: Int,
    updatedAnswers: UserAnswers,
    categorisationInfo: CategorisationInfo,
    exemptions: Seq[ott.Exemption]
  )(implicit messages: Messages, request: DataRequest[_]) = {

    val listItems     = categorisationInfo.categoryAssessments(index).getExemptionListItems
    val commodityCode = categorisationInfo.commodityCode

    val form         = formProvider(exemptions.size)
    val preparedForm = updatedAnswers.get(AssessmentPage(recordId, index)) match {
      case Some(value) => form.fill(value)
      case None        => form
    }

    Future.successful(Ok(view(preparedForm, mode, recordId, index, listItems, commodityCode)(request, messages)))
  }

  private def setAssessmentAnswerToBeNiphls(
    recordId: String,
    index: Int,
    userAnswers: UserAnswers,
    isNiphlsAnAnswer: Boolean,
    userHasNiphls: Boolean
  ) =
    if (isNiphlsAnAnswer && userHasNiphls)
      Future.fromTry(userAnswers.set(AssessmentPage(recordId, index), Exemption("true")))
    else Future.successful(userAnswers)

  def onSubmit(mode: Mode, recordId: String, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      {
        for {
          recordQuery        <- request.userAnswers.get(RecordCategorisationsQuery)
          categorisationInfo <- recordQuery.records.get(recordId)
          listItems           = categorisationInfo.categoryAssessments(index).getExemptionListItems
          commodityCode       = categorisationInfo.commodityCode
          exemptions          = categorisationInfo.categoryAssessments(index).exemptions
          form                = formProvider(exemptions.size)
        } yield form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, mode, recordId, index, listItems, commodityCode))),
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(AssessmentPage(recordId, index), value))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(AssessmentPage(recordId, index), mode, updatedAnswers))
          )
      }.getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad())))
    }
}
