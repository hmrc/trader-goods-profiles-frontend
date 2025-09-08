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

import connectors.{DownloadDataConnector, TraderProfileConnector}
import controllers.BaseController
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ProfileAuthenticateAction}
import models.{NormalMode, UserAnswers}
import pages.profile.niphl.{HasNiphlUpdatePage, NiphlNumberUpdatePage, RemoveNiphlPage}
import pages.profile.nirms.{HasNirmsUpdatePage, NirmsNumberUpdatePage, RemoveNirmsPage}
import pages.profile.ukims.UkimsNumberUpdatePage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.TraderProfileQuery
import repositories.SessionRepository
import utils.SessionData.{dataAdded, dataRemoved, dataUpdated, pageUpdated}
import viewmodels.checkAnswers.profile.*
import viewmodels.govuk.summarylist.*
import views.html.profile.ProfileView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProfileController @Inject() (
  override val messagesApi: MessagesApi,
  traderProfileConnector: TraderProfileConnector,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  downloadDataConnector: DownloadDataConnector,
  val controllerComponents: MessagesControllerComponents,
  view: ProfileView
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad(): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      downloadDataConnector.updateSeenStatus

      cleanseProfileData(request.userAnswers).flatMap { _ =>
        traderProfileConnector.getTraderProfile.map { profile =>
          val detailsList = SummaryListViewModel(
            rows = Seq(
              Some(UkimsNumberSummary.row(profile.ukimsNumber)),
              Some(HasNirmsSummary.row(profile.nirmsNumber.isDefined, NormalMode)),
              NirmsNumberSummary.row(profile.nirmsNumber),
              Some(HasNiphlSummary.row(profile.niphlNumber.isDefined, NormalMode)),
              NiphlNumberSummary.row(profile.niphlNumber, NormalMode)
            ).flatten
          )

          Ok(
            view(
              detailsList,
              request.session.get(dataUpdated).contains("true"),
              request.session.get(pageUpdated).getOrElse(""),
              request.session.get(dataRemoved).contains("true"),
              request.session.get(dataAdded).contains("true")
            )
          ).removingFromSession(dataUpdated, pageUpdated, dataRemoved, dataAdded)
        }
      }
    }

  private def cleanseProfileData(answers: UserAnswers): Future[UserAnswers] =
    for {
      updatedAnswersRemovedUkims            <-
        Future.fromTry(answers.remove(UkimsNumberUpdatePage))
      updatedAnswersRemovedHasNirms         <-
        Future.fromTry(updatedAnswersRemovedUkims.remove(HasNirmsUpdatePage))
      updatedAnswersRemovedRemoveNirms      <-
        Future.fromTry(updatedAnswersRemovedHasNirms.remove(RemoveNirmsPage))
      updatedAnswersRemovedNirmsNumber      <-
        Future.fromTry(updatedAnswersRemovedRemoveNirms.remove(NirmsNumberUpdatePage))
      updatedAnswersRemovedHasNiphl         <-
        Future.fromTry(updatedAnswersRemovedNirmsNumber.remove(HasNiphlUpdatePage))
      updatedAnswersRemovedRemoveNiphl      <-
        Future.fromTry(updatedAnswersRemovedHasNiphl.remove(RemoveNiphlPage))
      updatedAnswersRemoveNiphlNumberUpdate <-
        Future.fromTry(updatedAnswersRemovedRemoveNiphl.remove(NiphlNumberUpdatePage))
      updatedAnswers                        <-
        Future.fromTry(updatedAnswersRemoveNiphlNumberUpdate.remove(TraderProfileQuery))
      _                                     <- sessionRepository.set(updatedAnswers)
    } yield updatedAnswers
}
