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

package controllers.profile

import cats.data
import cats.data.EitherNec
import connectors.TraderProfileConnector
import controllers.actions._
import controllers.{BaseController, routes}
import models.{NormalMode, TraderProfile, ValidationError}
import navigation.profile.Navigator
import pages._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AuditService
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.profile.CyaNewUkimsNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CyaNewUkimsNumberController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  view: CyaNewUkimsNumberView,
  traderProfileConnector: TraderProfileConnector,
  navigator: Navigator,
  auditService: AuditService,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val errorMessage: String = "Unable to update new Ukims number in Trader profile."

  def onPageLoad(): Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData) {
    implicit request =>
      // TODO validateUkimsNumber validate the UkimsNumberUpdatePage. Needs a new method with NewUkimsNumberPage TGP-2699
      TraderProfile.validateUkimsNumber(request.userAnswers) match {
        case Right(_)     =>
          val list = SummaryListViewModel(
            // TODO This needs to be changed to NewUkimsNumberPageSummary.row TGP-2699
            rows = Seq(UkimsNumberSummary.rowUpdate(request.userAnswers)).flatten
          )
          Ok(view(list))
        case Left(errors) =>
          logErrorsAndContinue(errorMessage, routes.HomePageController.onPageLoad(), errors)
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    (for {
      ukimsNumber                <- handleValidateError(TraderProfile.validateUkimsNumber(request.userAnswers))
      oldTraderProfile           <- traderProfileConnector.getTraderProfile(request.eori)
      newTraderProfile           <- Future.successful(oldTraderProfile.copy(ukimsNumber = ukimsNumber))
      _                           = auditService.auditMaintainProfile(oldTraderProfile, newTraderProfile, request.affinityGroup)
      _                          <- traderProfileConnector.submitTraderProfile(newTraderProfile, request.eori)
      // TODO UkimsNumberUpdatePage needs to be updated with NewUkimsNumberPage TGP-2699
      updatedAnswersRemovedUkims <-
        Future.fromTry(request.userAnswers.remove(UkimsNumberUpdatePage))
      _                          <- sessionRepository.set(updatedAnswersRemovedUkims)
    } yield Redirect(navigator.nextPage(CyaNewUkimsNumberPage, NormalMode, request.userAnswers))).recover {
      case e: TraderProfileBuildFailure =>
        logErrorsAndContinue(e.getMessage, routes.HomePageController.onPageLoad())
    }
  }

  private def handleValidateError[T](result: EitherNec[ValidationError, T]): Future[T] =
    result match {
      case Right(value) => Future.successful(value)
      case Left(errors) => Future.failed(TraderProfileBuildFailure(errors))
    }

  private case class TraderProfileBuildFailure(errors: data.NonEmptyChain[ValidationError]) extends Exception {
    private val errorsAsString      = errors.toChain.toList.map(_.message).mkString(", ")
    override def getMessage: String = s"$errorMessage Missing pages: $errorsAsString"
  }
}