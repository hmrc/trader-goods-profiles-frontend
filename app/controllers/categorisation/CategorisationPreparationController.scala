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

package controllers.categorisation

import connectors.GoodsRecordConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ProfileAuthenticateAction}
import controllers.{BaseController, routes}
import models.Scenario.getResultAsInt
import models.helper.CategorisationUpdate
import models.ott.CategorisationInfo
import models.{CategoryRecord, Mode, NormalMode, UserAnswers}
import navigation.CategorisationNavigator
import org.apache.pekko.Done
import pages.categorisation.{CategorisationPreparationPage, HasSupplementaryUnitPage}
import pages.RecategorisationPreparationPage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{CategorisationDetailsQuery, LongerCategorisationDetailsQuery, LongerCommodityQuery}
import repositories.SessionRepository
import services.{AuditService, CategorisationService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants.Category2AsInt
import utils.SessionData.{dataRemoved, dataUpdated, pageUpdated}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

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
  navigator: CategorisationNavigator,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends BaseController {

  def startCategorisation(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      (for {
        goodsRecord        <- goodsRecordsConnector.getRecord(request.eori, recordId)
        categorisationInfo <-
          categorisationService
            .getCategorisationInfo(request, goodsRecord.comcode, goodsRecord.countryOfOrigin, recordId)
        _                   = auditService.auditStartUpdateGoodsRecord(
                                request.eori,
                                request.affinityGroup,
                                CategorisationUpdate,
                                recordId,
                                Some(categorisationInfo)
                              )
        updatedUserAnswers <-
          Future.fromTry(request.userAnswers.set(CategorisationDetailsQuery(recordId), categorisationInfo))
        _                  <- sessionRepository.set(updatedUserAnswers)
        _                  <- updateCategory(updatedUserAnswers, request.eori, request.affinityGroup, recordId, categorisationInfo)
      } yield Redirect(navigator.nextPage(CategorisationPreparationPage(recordId), NormalMode, updatedUserAnswers))
        .removingFromSession(dataUpdated, pageUpdated, dataRemoved))
        .recover { e =>
          logger.error(s"Unable to start categorisation for record $recordId: ${e.getMessage}")
          Redirect(routes.JourneyRecoveryController.onPageLoad().url)
        }

    }

  def startLongerCategorisation(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      (for {
        goodsRecord               <- goodsRecordsConnector.getRecord(request.eori, recordId)
        shorterCategorisationInfo <-
          Future.fromTry(Try(request.userAnswers.get(CategorisationDetailsQuery(recordId)).get))

        oldLongerCategorisationInfoOpt = request.userAnswers.get(LongerCategorisationDetailsQuery(recordId))
        longerComCode                 <- Future.fromTry(Try(request.userAnswers.get(LongerCommodityQuery(recordId)).get))
        newLongerCategorisationInfo   <-
          categorisationService
            .getCategorisationInfo(
              request,
              longerComCode.commodityCode,
              goodsRecord.countryOfOrigin,
              recordId,
              longerCode = true
            )

        updatedUASuppUnit <-
          Future.fromTry(
            cleanupSupplementaryUnit(
              request.userAnswers,
              recordId,
              shorterCategorisationInfo,
              newLongerCategorisationInfo
            )
          )

        updatedUACatInfo <-
          Future.fromTry(updatedUASuppUnit.set(LongerCategorisationDetailsQuery(recordId), newLongerCategorisationInfo))

        updatedUAReassessmentAnswers <- updateReassessmentAnswers(
                                          oldLongerCategorisationInfoOpt,
                                          newLongerCategorisationInfo,
                                          updatedUACatInfo,
                                          recordId,
                                          shorterCategorisationInfo
                                        )

        _ <- updateCategory(
               updatedUAReassessmentAnswers,
               request.eori,
               request.affinityGroup,
               recordId,
               newLongerCategorisationInfo
             )
        _ <- sessionRepository.set(updatedUAReassessmentAnswers)
      } yield Redirect(
        navigator.nextPage(RecategorisationPreparationPage(recordId), mode, updatedUAReassessmentAnswers)
      ))
        .recover { e =>
          logger.error(s"Unable to start categorisation for record $recordId: ${e.getMessage}")
          Redirect(routes.JourneyRecoveryController.onPageLoad().url)
        }
    }

  private def cleanupSupplementaryUnit(
    userAnswers: UserAnswers,
    recordId: String,
    shorterCatInfo: CategorisationInfo,
    longerCatInfo: CategorisationInfo
  ) = {

    val oldLongerCatInfo = userAnswers.get(LongerCategorisationDetailsQuery(recordId))

    val currentMeasureUnit = oldLongerCatInfo.map(_.measurementUnit).getOrElse(shorterCatInfo.measurementUnit)

    if (currentMeasureUnit != longerCatInfo.measurementUnit) {
      userAnswers.remove(HasSupplementaryUnitPage(recordId))
    } else {
      Success(userAnswers)
    }
  }

  private case class CategoryRecordBuildFailure(error: String) extends Exception {
    override def getMessage: String = s"Failed to build category record: $error"
  }

  private def updateCategory(
    updatedUserAnswers: UserAnswers,
    eori: String,
    affinityGroup: AffinityGroup,
    recordId: String,
    categorisationInfo: CategorisationInfo
  )(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    if (
      categorisationInfo.categoryAssessmentsThatNeedAnswers.isEmpty && !categorisationInfo.isCommCodeExpired
      && !isSupplementaryUnitQuestionToBeAnswered(categorisationInfo, updatedUserAnswers, recordId)
    ) {
      CategoryRecord.build(updatedUserAnswers, eori, recordId, categorisationService) match {
        case Right(record) =>
          auditService.auditFinishCategorisation(
            eori,
            affinityGroup,
            recordId,
            record
          )

          val result = for {
            oldRecord <- goodsRecordsConnector.getRecord(eori, recordId)
            _         <- goodsRecordsConnector.updateCategoryAndComcodeForGoodsRecord(eori, recordId, record, oldRecord)
          } yield Done

          result

        case Left(errors) =>
          val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")
          Future.failed(CategoryRecordBuildFailure(errorMessages))
      }
    } else {
      Future.successful(Done)
    }

  private def isSupplementaryUnitQuestionToBeAnswered(
    catInfo: CategorisationInfo,
    updatedUserAnswers: UserAnswers,
    recordId: String
  ) = {
    val scenario = categorisationService.calculateResult(catInfo, updatedUserAnswers, recordId)
    catInfo.measurementUnit.isDefined && getResultAsInt(scenario) == Category2AsInt
  }

  private def updateReassessmentAnswers(
    oldLongerCategorisationInfoOpt: Option[CategorisationInfo],
    newLongerCategorisationInfo: CategorisationInfo,
    updatedUACatInfo: UserAnswers,
    recordId: String,
    shorterCategorisationInfo: CategorisationInfo
  ): Future[UserAnswers] = {

    val isNewOneTheSameAsOldOne = oldLongerCategorisationInfoOpt.exists(_.equals(newLongerCategorisationInfo))

    if (isNewOneTheSameAsOldOne) {
      Future.successful(updatedUACatInfo)
    } else {
      Future.fromTry(
        categorisationService.updatingAnswersForRecategorisation(
          updatedUACatInfo,
          recordId,
          shorterCategorisationInfo,
          newLongerCategorisationInfo
        )
      )
    }
  }
}
