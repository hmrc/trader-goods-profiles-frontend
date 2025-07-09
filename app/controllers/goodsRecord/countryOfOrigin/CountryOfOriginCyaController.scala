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

package controllers.goodsRecord.countryOfOrigin

import cats.data
import cats.data.EitherNec
import com.google.inject.Inject
import connectors.{GoodsRecordConnector, OttConnector}
import controllers.BaseController
import controllers.actions.*
import models.*
import models.requests.DataRequest
import models.router.requests.PutRecordRequest
import navigation.GoodsRecordNavigator
import org.apache.pekko.Done
import pages.goodsRecord.*
import play.api.i18n.MessagesApi
import play.api.mvc.*
import queries.CountriesQuery
import repositories.SessionRepository
import services.{AuditService, AutoCategoriseService, CommodityService}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants.*
import utils.SessionData.{dataRemoved, dataUpdated, pageUpdated}
import viewmodels.checkAnswers.goodsRecord.*
import viewmodels.govuk.summarylist.*
import views.html.goodsRecord.CyaUpdateRecordView

import java.time.{LocalDate, ZoneId}
import scala.concurrent.{ExecutionContext, Future}

class CountryOfOriginCyaController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              identify: IdentifierAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              profileAuth: ProfileAuthenticateAction,
                                              val controllerComponents: MessagesControllerComponents,
                                              auditService: AuditService,
                                              view: CyaUpdateRecordView,
                                              goodsRecordConnector: GoodsRecordConnector,
                                              ottConnector: OttConnector,
                                              sessionRepository: SessionRepository,
                                              navigator: GoodsRecordNavigator,
                                              autoCategoriseService: AutoCategoriseService
                                            )(implicit ec: ExecutionContext)
  extends BaseController {

  private val errorMessage: String = "Unable to update Goods Record."

  def onPageLoad(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      goodsRecordConnector
        .getRecord(recordId)
        .flatMap { recordResponse =>
          UpdateGoodsRecord
            .validateCountryOfOrigin(
              request.userAnswers,
              recordId,
              recordResponse.category.isDefined
            ) match {
            case Right(_) =>
              val onSubmitAction =
                controllers.goodsRecord.countryOfOrigin.routes.CountryOfOriginCyaController.onSubmit(recordId)
              getCountryOfOriginAnswer(request.userAnswers, recordId).map {
                case Some(answer) =>
                  val list = SummaryListViewModel(
                    Seq(
                      CountryOfOriginSummary
                        .rowUpdateCya(answer, recordId, CheckMode)
                    )
                  )
                  Ok(view(list, onSubmitAction, countryOfOriginKey))
                case _ => Redirect(controllers.problem.routes.JourneyRecoveryController.onPageLoad().url)
              }

            case Left(errors) =>
              Future.successful(
                logErrorsAndContinue(
                  errorMessage,
                  controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId),
                  errors
                )
              )
          }
        }
        .recoverWith { case e: Exception =>
          logger.error(s"Unable to fetch record $recordId: ${e.getMessage}")
          Future.successful(
            navigator.journeyRecovery(
              Some(RedirectUrl(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId).url))
            )
          )
        }
    }

  private def getCountryOfOriginAnswer(userAnswers: UserAnswers, recordId: String)(implicit
                                                                                   request: Request[_]
  ): Future[Option[String]] =
    userAnswers.get(CountryOfOriginUpdatePage(recordId)) match {
      case Some(answer) =>
        userAnswers.get(CountriesQuery) match {
          case Some(countries) => Future.successful(Some(findCountryName(countries, answer)))
          case None =>
            getCountries(userAnswers).map { countries =>
              Some(findCountryName(countries, answer))
            }
        }
      case _ => Future.successful(None)
    }

  private def findCountryName(countries: Seq[Country], answer: String): String =
    countries.find(country => country.id == answer).map(_.description).getOrElse(answer)

  def getCountries(userAnswers: UserAnswers)(implicit request: Request[_]): Future[Seq[Country]] =
    for {
      countries <- ottConnector.getCountries
      updatedAnswersWithQuery <- Future.fromTry(userAnswers.set(CountriesQuery, countries))
      _ <- sessionRepository.set(updatedAnswersWithQuery)
    } yield countries

  private def handleValidateError[T](result: EitherNec[ValidationError, T]): Future[T] =
    result match {
      case Right(value) => Future.successful(value)
      case Left(errors) => Future.failed(GoodsRecordBuildFailure(errors))
    }

  private case class GoodsRecordBuildFailure(errors: data.NonEmptyChain[ValidationError]) extends Exception {
    private val errorsAsString = errors.toChain.toList.map(_.message).mkString(", ")

    override def getMessage: String = s"$errorMessage Missing pages: $errorsAsString"
  }

  private def updateGoodsRecordIfPutValueChanged(
                                                  newValue: String,
                                                  oldValue: String,
                                                  newUpdateGoodsRecord: UpdateGoodsRecord,
                                                  putRecordRequest: PutRecordRequest
                                                )(implicit hc: HeaderCarrier): Future[Done] =
    if (newValue != oldValue) {
      goodsRecordConnector.putGoodsRecord(
        putRecordRequest,
        newUpdateGoodsRecord.recordId
      )
    } else {
      Future.successful(Done)
    }
  def onSubmit(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      (for {
        oldRecord <- goodsRecordConnector.getRecord(recordId)

        originalCountryOfOrigin <-
          request.userAnswers
            .get(OriginalCountryOfOriginPage(recordId))
            .map(Future.successful)
            .getOrElse(Future.failed(new Exception(s"Original country of origin not found in session for $recordId")))

        countryOfOrigin <- handleValidateError(
          UpdateGoodsRecord.validateCountryOfOrigin(
            request.userAnswers,
            recordId,
            oldRecord.category.isDefined
          )
        )

        updateGoodsRecord = UpdateGoodsRecord(
          eori = request.eori,
          recordId = recordId,
          countryOfOrigin = Some(countryOfOrigin)
        )

        putGoodsRecord = PutRecordRequest(
          actorId = oldRecord.eori,
          traderRef = oldRecord.traderRef,
          comcode = oldRecord.comcode,
          goodsDescription = oldRecord.goodsDescription,
          countryOfOrigin = countryOfOrigin,
          category = None,
          assessments = oldRecord.assessments,
          supplementaryUnit = oldRecord.supplementaryUnit,
          measurementUnit = oldRecord.measurementUnit,
          comcodeEffectiveFromDate = oldRecord.comcodeEffectiveFromDate,
          comcodeEffectiveToDate = oldRecord.comcodeEffectiveToDate
        )

        _ = auditService.auditFinishUpdateGoodsRecord(recordId, request.affinityGroup, updateGoodsRecord)

        _ <- updateGoodsRecordIfPutValueChanged(
          newValue = countryOfOrigin,
          oldValue = oldRecord.countryOfOrigin,
          updateGoodsRecord,
          putGoodsRecord
        )

        cleanedAnswers <- Future.fromTry(
          request.userAnswers
            .remove(HasCountryOfOriginChangePage(recordId))
            .flatMap(_.remove(CountryOfOriginUpdatePage(recordId)))
            .flatMap(_.remove(OriginalCountryOfOriginPage(recordId)))
        )

        _ <- sessionRepository.set(cleanedAnswers)

        autoCategoriseScenario <- autoCategoriseService.autoCategoriseRecord(recordId, cleanedAnswers)

      } yield {
        val hasChanged = countryOfOrigin != originalCountryOfOrigin

        if (autoCategoriseScenario.isDefined && hasChanged) {
          Redirect(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId))
            .addingToSession("countryOfOriginChanged" -> hasChanged.toString)
            .removingFromSession(dataUpdated)
        } else if (hasChanged) {
          Redirect(controllers.goodsRecord.countryOfOrigin.routes.UpdatedCountryOfOriginController.onPageLoad(recordId))
        } else {
          Redirect(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId))
            .addingToSession("countryOfOriginChanged" -> "false")
            .removingFromSession(dataUpdated)
        }
      }).recover(handleRecover(recordId))
    }

  private def handleRecover(
                             recordId: String
                           )(implicit request: DataRequest[AnyContent]): PartialFunction[Throwable, Result] = {
    case e: GoodsRecordBuildFailure =>
      logErrorsAndContinue(
        e.getMessage,
        controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId)
      )

    case e: UpstreamErrorResponse if e.message.contains(openAccreditationErrorCode) =>
      Redirect(controllers.routes.RecordLockedController.onPageLoad(recordId))
        .removingFromSession(dataRemoved, dataUpdated, pageUpdated)
  }
}
