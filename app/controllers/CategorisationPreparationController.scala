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
import models.{CategoryRecord2, NormalMode, UserAnswers}
import navigation.Navigator
import org.apache.pekko.Done
import pages.CategorisationPreparationPage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.CategorisationDetailsQuery2
import repositories.SessionRepository
import services.CategorisationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

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
        goodsRecord            <- goodsRecordsConnector.getRecord(request.eori, recordId)
        categorisationInfo     <-
          categorisationService
            .getCategorisationInfo(request, goodsRecord.comcode, goodsRecord.countryOfOrigin, recordId)
        updatedUserAnswers     <-
          Future.fromTry(request.userAnswers.set(CategorisationDetailsQuery2(recordId), categorisationInfo))
        _                      <- sessionRepository.set(updatedUserAnswers)
        needToUpdateGoodsRecord = categorisationInfo.categoryAssessmentsThatNeedAnswers.isEmpty
        _                      <- if (needToUpdateGoodsRecord) updateCategory(updatedUserAnswers, request.eori, recordId)
                                  else Future.successful(Done)
      } yield Redirect(navigator.nextPage(CategorisationPreparationPage(recordId), NormalMode, updatedUserAnswers)))
        .recover { e =>
          logger.error(s"Unable to start categorisation for record $recordId: ${e.getMessage}")
          Redirect(routes.JourneyRecoveryController.onPageLoad().url)
        }

    }

  def startLongerCategorisation(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      (for {
        goodsRecord <- goodsRecordsConnector.getRecord(request.eori, recordId)
        longerComCode <- Future.fromTry(Try(request.userAnswers.get(LongerCommodityQuery2(recordId)).get))
        categorisationInfo <-
          categorisationService
            .getCategorisationInfo(request, longerComCode, goodsRecord.countryOfOrigin, recordId)
        updatedUserAnswers <-
          Future.fromTry(request.userAnswers.set(LongerCategorisationDetailsQuery(recordId), categorisationInfo))
        _                      <- sessionRepository.set(updatedUserAnswers)
      } yield Redirect(navigator.nextPage(CategorisationPreparationPage(recordId), NormalMode, updatedUserAnswers)))
        .recover { e =>
          logger.error(s"Unable to start categorisation for record $recordId: ${e.getMessage}")
          Redirect(routes.JourneyRecoveryController.onPageLoad().url)
        }
    }

  //TODO startRecategorisation
  //Needs to save longer commodity code to the LongerCategorisationDetailsQuery
  //Needs to do Marcy's thing from before where it copies across old answers

  final case class CategoryRecordBuildFailure(error: String) extends Exception {
    override def getMessage: String = s"Failed to build category record: $error"
  }

  private def updateCategory(updatedUserAnswers: UserAnswers, eori: String, recordId: String)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    CategoryRecord2.build(updatedUserAnswers, eori, recordId, categorisationService) match {
      case Right(record) => goodsRecordsConnector.updateCategoryAndComcodeForGoodsRecord2(eori, recordId, record)
      case Left(errors)  =>
        val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")

        Future.failed(CategoryRecordBuildFailure(errorMessages))
    }

}
