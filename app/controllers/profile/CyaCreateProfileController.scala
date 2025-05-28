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

import com.google.inject.Inject
import connectors.TraderProfileConnector
import controllers.BaseController
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ProfileCheckAction}
import models.helper.CreateProfileJourney
import models.{NormalMode, TraderProfile}
import navigation.ProfileNavigator
import pages.profile.CyaCreateProfilePage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditService, DataCleansingService}
import viewmodels.checkAnswers.profile._
import viewmodels.govuk.summarylist._
import views.html.profile.CyaCreateProfileView

import scala.concurrent.{ExecutionContext, Future}

class CyaCreateProfileController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  checkProfile: ProfileCheckAction,
  val controllerComponents: MessagesControllerComponents,
  view: CyaCreateProfileView,
  traderProfileConnector: TraderProfileConnector,
  auditService: AuditService,
  navigator: ProfileNavigator,
  dataCleansingService: DataCleansingService
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val errorMessage: String = "Unable to create Trader profile."

  def onPageLoad(): Action[AnyContent] = (identify andThen checkProfile andThen getData andThen requireData) {
    implicit request =>
      TraderProfile.build(request.userAnswers, request.eori) match {
        case Right(_)     =>
          val list = SummaryListViewModel(
            rows = Seq(
              UkimsNumberSummary.row(request.userAnswers),
              HasNirmsSummary.row(request.userAnswers),
              NirmsNumberSummary.row(request.userAnswers),
              HasNiphlSummary.row(request.userAnswers),
              NiphlNumberSummary.row(request.userAnswers)
            ).flatten
          )
          Ok(view(list))
        case Left(errors) =>
          dataCleansingService.deleteMongoData(request.userAnswers.id, CreateProfileJourney)
          logErrorsAndContinue(errorMessage, controllers.profile.routes.ProfileSetupController.onPageLoad(), errors)
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen checkProfile andThen getData andThen requireData).async {
    implicit request =>
      TraderProfile.build(request.userAnswers, request.eori) match {
        case Right(model) =>
          traderProfileConnector
            .submitTraderProfile(model)
            .flatMap { _ =>
              auditService.auditProfileSetUp(model, request.affinityGroup).map { _ =>
                dataCleansingService.deleteMongoData(request.userAnswers.id, CreateProfileJourney)
                Redirect(navigator.nextPage(CyaCreateProfilePage, NormalMode, request.userAnswers))
              }
            }
            .recover { case ex: Exception =>
              logger.error(s"Failed to submit trader profile: ${ex.getMessage}")
              Redirect(controllers.profile.routes.ProfileSetupController.onPageLoad())
            }

        case Left(errors) =>
          dataCleansingService.deleteMongoData(request.userAnswers.id, CreateProfileJourney)
          Future.successful(
            logErrorsAndContinue(errorMessage, controllers.profile.routes.ProfileSetupController.onPageLoad(), errors)
          )
      }

  }

}
