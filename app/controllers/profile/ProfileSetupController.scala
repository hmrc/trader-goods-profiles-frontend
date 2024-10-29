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

import config.FrontendAppConfig
import connectors.TraderProfileConnector
import controllers.BaseController
import controllers.actions._
import models.{HistoricProfileData, NormalMode, UserAnswers}
import navigation.profile.Navigator
import pages.{ProfileSetupPage, UkimsNumberPage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.HistoricProfileDataQuery
import repositories.SessionRepository
import views.html.profile.ProfileSetupView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ProfileSetupController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  val controllerComponents: MessagesControllerComponents,
  view: ProfileSetupView,
  navigator: Navigator,
  requireData: DataRequiredAction,
  getOrCreate: DataRetrievalOrCreateAction,
  checkProfile: ProfileCheckAction,
  sessionRepository: SessionRepository,
  traderProfileConnector: TraderProfileConnector,
  config: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad: Action[AnyContent] = (identify andThen checkProfile andThen getOrCreate) { implicit request =>
    Ok(view())
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    if (config.getHistoricProfileEnabled) {
      for {
        historicProfileData <- traderProfileConnector.getHistoricProfileData(request.eori)
        updatedUserAnswers  <- updateUserAnswersWithProfileData(request.userAnswers, historicProfileData)
        _                   <- sessionRepository.set(updatedUserAnswers)
      } yield Redirect(navigator.nextPage(ProfileSetupPage, NormalMode, updatedUserAnswers))
    } else {
      Future.successful(Redirect(navigator.nextPage(ProfileSetupPage, NormalMode, request.userAnswers)))
    }
  }

  private def updateUserAnswersWithProfileData(
    userAnswers: UserAnswers,
    historicProfileData: Option[HistoricProfileData]
  ) = {
    val ukimsNumber = historicProfileData.flatMap(_.ukimsNumber)

    if (ukimsNumber.isDefined) {
      for {
        answersWithProfileData <- Future.fromTry(userAnswers.set(HistoricProfileDataQuery, historicProfileData.get))
        answersWithUkimsNumber <- Future.fromTry(answersWithProfileData.set(UkimsNumberPage, ukimsNumber.get))
      } yield answersWithUkimsNumber
    } else {
      Future.successful(userAnswers)
    }
  }
}
