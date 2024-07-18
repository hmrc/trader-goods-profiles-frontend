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
import models.UserAnswers
import pages.{HasNiphlUpdatePage, HasNirmsUpdatePage, NiphlNumberUpdatePage, NirmsNumberUpdatePage, RemoveNiphlPage, RemoveNirmsPage, UkimsNumberUpdatePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.ProfileView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProfileController @Inject() (
  override val messagesApi: MessagesApi,
  traderProfileConnector: TraderProfileConnector,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ProfileView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    cleanseProfileData(request.userAnswers).flatMap { _ =>
      traderProfileConnector.getTraderProfile(request.eori).map { profile =>
        val detailsList = SummaryListViewModel(
          rows = Seq(
            Some(UkimsNumberSummary.row(profile.ukimsNumber)),
            Some(HasNirmsSummary.row(profile.nirmsNumber.isDefined)),
            NirmsNumberSummary.row(profile.nirmsNumber),
            Some(HasNiphlSummary.row(profile.niphlNumber.isDefined)),
            NiphlNumberSummary.row(profile.niphlNumber)
          ).flatten
        )

        Ok(view(detailsList))
      }
    }
  }

  def cleanseProfileData(answers: UserAnswers): Future[UserAnswers] =
    for {
      updatedAnswersRemovedUkims       <-
        Future.fromTry(answers.remove(UkimsNumberUpdatePage))
      updatedAnswersRemovedHasNirms    <-
        Future.fromTry(updatedAnswersRemovedUkims.remove(HasNirmsUpdatePage))
      updatedAnswersRemovedRemoveNirms <-
        Future.fromTry(updatedAnswersRemovedHasNirms.remove(RemoveNirmsPage))
      updatedAnswersRemovedNirmsNumber <-
        Future.fromTry(updatedAnswersRemovedRemoveNirms.remove(NirmsNumberUpdatePage))
      updatedAnswersRemovedHasNiphl    <-
        Future.fromTry(updatedAnswersRemovedNirmsNumber.remove(HasNiphlUpdatePage))
      updatedAnswersRemovedRemoveNiphl <-
        Future.fromTry(updatedAnswersRemovedHasNiphl.remove(RemoveNiphlPage))
      updatedAnswers                   <-
        Future.fromTry(updatedAnswersRemovedRemoveNiphl.remove(NiphlNumberUpdatePage))
      _                                <- sessionRepository.set(updatedAnswers)
    } yield updatedAnswers
}
