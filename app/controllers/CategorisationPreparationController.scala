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

import connectors.GoodsRecordConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ProfileAuthenticateAction}
import logging.Logging
import models.NormalMode
import navigation.Navigator
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.CategorisationDetailsQuery2
import repositories.SessionRepository
import services.CategorisationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import pages.CategorisationPreparationPage

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CategorisationPreparationController @Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  categorisationService: CategorisationService,
  goodsRecordsConnector: GoodsRecordConnector,
  sessionRepository: SessionRepository,
  navigator: Navigator
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with Logging {

  def startCategorisation(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      (for {
        goodsRecord        <- goodsRecordsConnector.getRecord(request.eori, recordId)
        categorisationInfo <-
          categorisationService
            .getCategorisationInfo(request, goodsRecord.comcode, goodsRecord.countryOfOrigin, recordId)
        updatedUserAnswers <-
          Future.fromTry(request.userAnswers.set(CategorisationDetailsQuery2(recordId), categorisationInfo))
        _                  <- sessionRepository.set(updatedUserAnswers)
      } yield {
        Redirect(navigator.nextPage(CategorisationPreparationPage(recordId), NormalMode, updatedUserAnswers))
      })
        .recover { e =>
          logger.error(s"Unable to start categorisation for record $recordId: ${e.getMessage}")
          Redirect(routes.JourneyRecoveryController.onPageLoad().url)
        }

    }

}
