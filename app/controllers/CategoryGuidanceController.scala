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
import controllers.actions._
import logging.Logging
import models.helper.CategorisationUpdate
import models.{Category1NoExemptions, CategoryRecord, NoRedirectScenario, NormalMode, Scenario, StandardNoAssessments}
import navigation.Navigator
import pages.CategoryGuidancePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.CategorisationDetailsQuery
import services.{AuditService, CategorisationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.SessionData.{dataUpdated, pageUpdated}
import views.html.CategoryGuidanceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CategoryGuidanceController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  view: CategoryGuidanceView,
  auditService: AuditService,
  categorisationService: CategorisationService,
  navigator: Navigator,
  goodsRecordConnector: GoodsRecordConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      categorisationService
        .requireCategorisation(request, recordId)
        .flatMap { userAnswers =>
          val categorisationInfo = userAnswers.get(CategorisationDetailsQuery(recordId))
          val scenario           = categorisationInfo.map(Scenario.getRedirectScenarios)
          scenario match {
            case Some(Category1NoExemptions | StandardNoAssessments) =>
              CategoryRecord
                .build(userAnswers, request.eori, recordId)
                .map { categoryRecord =>
                  goodsRecordConnector
                    .updateCategoryAndComcodeForGoodsRecord(request.eori, recordId, categoryRecord)
                    .map { _ =>
                      Redirect(routes.CategorisationResultController.onPageLoad(recordId, scenario.get).url)
                        .removingFromSession(dataUpdated, pageUpdated)
                    }
                }
                .getOrElse(Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url)))
            case Some(NoRedirectScenario)                            =>
              Future.successful(Ok(view(recordId)).removingFromSession(dataUpdated, pageUpdated))
          }
        }
        .recover { e =>
          logger.error(s"Unable to start categorisation for record $recordId: ${e.getMessage}")
          Redirect(routes.JourneyRecoveryController.onPageLoad().url)
        }
    }

  def onSubmit(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      auditService
        .auditStartUpdateGoodsRecord(
          request.eori,
          request.affinityGroup,
          CategorisationUpdate,
          recordId
        )

      Redirect(navigator.nextPage(CategoryGuidancePage(recordId), NormalMode, request.userAnswers))
    }
}
