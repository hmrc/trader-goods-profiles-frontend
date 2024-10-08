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

import cats.data
import cats.data.EitherNec
import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.{GoodsRecordConnector, OttConnector}
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.requests.DataRequest
import models.router.requests.PutRecordRequest
import models.router.responses.GetGoodsRecordResponse
import models.{CheckMode, Commodity, Country, NormalMode, UpdateGoodsRecord, UserAnswers, ValidationError}
import navigation.Navigator
import org.apache.pekko.Done
import pages._
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import queries.CountriesQuery
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants.{commodityCodeKey, countryOfOriginKey, goodsDescriptionKey, traderReferenceKey}
import utils.SessionData.fromExpiredCommodityCodePage
import viewmodels.checkAnswers._
import viewmodels.govuk.summarylist._
import views.html.CyaUpdateRecordView

import scala.concurrent.{ExecutionContext, Future}

class CyaUpdateRecordController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  auditService: AuditService,
  view: CyaUpdateRecordView,
  goodsRecordConnector: GoodsRecordConnector,
  ottConnector: OttConnector,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  config: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val errorMessage: String = "Unable to update Goods Record."

  def onPageLoadCountryOfOrigin(recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      goodsRecordConnector
        .getRecord(request.eori, recordId)
        .flatMap { recordResponse =>
          UpdateGoodsRecord
            .buildCountryOfOrigin(
              request.userAnswers,
              request.eori,
              recordId,
              recordResponse.category.isDefined
            ) match {
            case Right(_)     =>
              val onSubmitAction = routes.CyaUpdateRecordController.onSubmitCountryOfOrigin(recordId)
              getCountryOfOriginAnswer(request.userAnswers, recordId).map { answer =>
                val list = SummaryListViewModel(
                  Seq(
                    CountryOfOriginSummary
                      .rowUpdateCya(answer, recordId, CheckMode)
                  )
                )
                Ok(view(list, onSubmitAction, countryOfOriginKey))
              }
            case Left(errors) =>
              Future.successful(
                logErrorsAndContinue(
                  errorMessage,
                  routes.SingleRecordController.onPageLoad(recordId),
                  errors
                )
              )
          }
        }
        .recoverWith { case e: Exception =>
          logger.error(s"Unable to fetch record $recordId: ${e.getMessage}")
          Future.successful(
            navigator.journeyRecovery(Some(RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)))
          )
        }
    }

  def onPageLoadGoodsDescription(recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      UpdateGoodsRecord.validateGoodsDescription(request.userAnswers, recordId) match {
        case Right(goodsDescription) =>
          val onSubmitAction = routes.CyaUpdateRecordController.onSubmitGoodsDescription(recordId)

          val list = SummaryListViewModel(
            Seq(GoodsDescriptionSummary.rowUpdateCya(goodsDescription, recordId, CheckMode))
          )
          Ok(view(list, onSubmitAction, goodsDescriptionKey))
        case Left(errors)            =>
          logErrorsAndContinue(
            errorMessage,
            routes.SingleRecordController.onPageLoad(recordId),
            errors
          )
      }
    }

  def onPageLoadTraderReference(recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      UpdateGoodsRecord.validateTraderReference(request.userAnswers, recordId) match {
        case Right(traderReference) =>
          val onSubmitAction = routes.CyaUpdateRecordController.onSubmitTraderReference(recordId)

          val list = SummaryListViewModel(
            Seq(TraderReferenceSummary.row(traderReference, recordId, CheckMode, recordLocked = false))
          )
          Ok(view(list, onSubmitAction, traderReferenceKey))
        case Left(errors)           =>
          logErrorsAndContinue(
            errorMessage,
            routes.SingleRecordController.onPageLoad(recordId),
            errors
          )
      }
    }

  def onPageLoadCommodityCode(recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      goodsRecordConnector
        .getRecord(request.eori, recordId)
        .flatMap { recordResponse =>
          val isCommCodeExpired = request.session.get(fromExpiredCommodityCodePage).contains("true")
          UpdateGoodsRecord
            .validateCommodityCode(
              request.userAnswers,
              recordId,
              recordResponse.category.isDefined,
              isCommCodeExpired
            ) match {
            case Right(commodity) =>
              val onSubmitAction = routes.CyaUpdateRecordController.onSubmitCommodityCode(recordId)

              val list = SummaryListViewModel(
                Seq(
                  CommodityCodeSummary
                    .rowUpdateCya(
                      commodity.commodityCode,
                      recordId,
                      CheckMode
                    )
                )
              )
              Future.successful(Ok(view(list, onSubmitAction, commodityCodeKey)))
            case Left(errors)     =>
              Future.successful(
                logErrorsAndContinue(
                  errorMessage,
                  routes.SingleRecordController.onPageLoad(recordId),
                  errors
                )
              )
          }
        }
        .recoverWith { case e: Exception =>
          logger.error(s"Unable to fetch record $recordId: ${e.getMessage}")
          Future.successful(
            navigator.journeyRecovery(Some(RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)))
          )
        }
    }

  private def getCountryOfOriginAnswer(userAnswers: UserAnswers, recordId: String)(implicit
    request: Request[_]
  ): Future[String] =
    userAnswers.get(CountryOfOriginUpdatePage(recordId)) match {
      case Some(answer) =>
        userAnswers.get(CountriesQuery) match {
          case Some(countries) => Future.successful(findCountryName(countries, answer))
          case None            =>
            getCountries(userAnswers).map { countries =>
              findCountryName(countries, answer)
            }
        }
    }

  private def findCountryName(countries: Seq[Country], answer: String): String =
    countries.find(country => country.id == answer).map(_.description).getOrElse(answer)

  def getCountries(userAnswers: UserAnswers)(implicit request: Request[_]): Future[Seq[Country]] =
    for {
      countries               <- ottConnector.getCountries
      updatedAnswersWithQuery <- Future.fromTry(userAnswers.set(CountriesQuery, countries))
      _                       <- sessionRepository.set(updatedAnswersWithQuery)
    } yield countries

  private def handleValidateError[T](result: EitherNec[ValidationError, T]): Future[T] =
    result match {
      case Right(value) => Future.successful(value)
      case Left(errors) => Future.failed(GoodsRecordBuildFailure(errors))
    }

  private case class GoodsRecordBuildFailure(errors: data.NonEmptyChain[ValidationError]) extends Exception {
    private val errorsAsString      = errors.toChain.toList.map(_.message).mkString(", ")
    override def getMessage: String = s"$errorMessage Missing pages: $errorsAsString"
  }

  private def updateTraderReferenceIfValueChanged[T](
    newValue: T,
    oldValue: T,
    newUpdateGoodsRecord: UpdateGoodsRecord
  )(implicit hc: HeaderCarrier): Future[Done] =
    if (newValue != oldValue) {

      // TODO: remove this flag when EIS has implemented the PATCH method - TGP-2417 and keep the call to patchGoodsRecord as default
      if (config.useEisPatchMethod) {
        goodsRecordConnector.patchGoodsRecord(
          newUpdateGoodsRecord
        )
      } else {
        goodsRecordConnector.updateGoodsRecord(
          newUpdateGoodsRecord
        )
      }

    } else {
      Future.successful(Done)
    }

  private def updateCommodityCodeAndSession(recordId: String, commodity: Commodity, oldRecord: GetGoodsRecordResponse)(
    implicit request: DataRequest[AnyContent]
  ): Future[Result] =
    for {
      // TODO: remove this flag when EIS has implemented the PATCH method - TGP-2417 and keep the call to putGoodsRecord as default
      _ <- if (config.useEisPatchMethod) {
             goodsRecordConnector.putGoodsRecord(
               PutRecordRequest(
                 actorId = request.eori,
                 traderRef = oldRecord.traderRef,
                 comcode = commodity.commodityCode,
                 goodsDescription = oldRecord.goodsDescription,
                 countryOfOrigin = oldRecord.countryOfOrigin,
                 category = oldRecord.category,
                 assessments = oldRecord.assessments,
                 supplementaryUnit = oldRecord.supplementaryUnit,
                 measurementUnit = oldRecord.measurementUnit,
                 comcodeEffectiveFromDate = oldRecord.comcodeEffectiveFromDate,
                 comcodeEffectiveToDate = oldRecord.comcodeEffectiveToDate
               ),
               recordId
             )
           } else {
             goodsRecordConnector.updateGoodsRecord(
               UpdateGoodsRecord(request.eori, recordId, commodityCode = Some(commodity))
             )
           }

      updatedAnswersWithChange <-
        Future.fromTry(request.userAnswers.remove(HasCommodityCodeChangePage(recordId)))
      updatedAnswers           <- Future.fromTry(updatedAnswersWithChange.remove(CommodityCodeUpdatePage(recordId)))
      _                        <- sessionRepository.set(updatedAnswers)
    } yield Redirect(navigator.nextPage(CyaUpdateRecordPage(recordId), NormalMode, updatedAnswers))

  private def updateCountryOfOriginAndSession(
    recordId: String,
    updateGoodsRecord: UpdateGoodsRecord,
    oldRecord: GetGoodsRecordResponse
  )(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] =
    for {
      // TODO: remove this flag when EIS has implemented the PATCH method - TGP-2417 and keep the call to putGoodsRecord as default
      _ <- if (config.useEisPatchMethod) {
             goodsRecordConnector.putGoodsRecord(
               PutRecordRequest(
                 actorId = request.eori,
                 traderRef = oldRecord.traderRef,
                 comcode = oldRecord.comcode,
                 goodsDescription = oldRecord.goodsDescription,
                 countryOfOrigin = updateGoodsRecord.countryOfOrigin.get,
                 category = oldRecord.category,
                 assessments = oldRecord.assessments,
                 supplementaryUnit = oldRecord.supplementaryUnit,
                 measurementUnit = oldRecord.measurementUnit,
                 comcodeEffectiveFromDate = oldRecord.comcodeEffectiveFromDate,
                 comcodeEffectiveToDate = oldRecord.comcodeEffectiveToDate
               ),
               recordId
             )
           } else {
             goodsRecordConnector.updateGoodsRecord(updateGoodsRecord)
           }

      updatedAnswersWithChange <-
        Future.fromTry(request.userAnswers.remove(HasCountryOfOriginChangePage(recordId)))
      updatedAnswers           <- Future.fromTry(updatedAnswersWithChange.remove(CountryOfOriginUpdatePage(recordId)))
      _                        <- sessionRepository.set(updatedAnswers)
    } yield Redirect(navigator.nextPage(CyaUpdateRecordPage(recordId), NormalMode, updatedAnswers))

  def onSubmitTraderReference(recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      (for {
        traderReference <- handleValidateError(UpdateGoodsRecord.validateTraderReference(request.userAnswers, recordId))
        _                = auditService.auditFinishUpdateGoodsRecord(
                             recordId,
                             request.affinityGroup,
                             UpdateGoodsRecord(request.eori, recordId, traderReference = Some(traderReference))
                           )
        oldRecord       <- goodsRecordConnector.getRecord(request.eori, recordId)
        _               <- updateTraderReferenceIfValueChanged(
                             traderReference,
                             oldRecord.traderRef,
                             UpdateGoodsRecord(request.eori, recordId, traderReference = Some(traderReference))
                           )
        updatedAnswers  <- Future.fromTry(request.userAnswers.remove(TraderReferenceUpdatePage(recordId)))
        _               <- sessionRepository.set(updatedAnswers)
      } yield Redirect(navigator.nextPage(CyaUpdateRecordPage(recordId), NormalMode, updatedAnswers))).recover {
        case e: GoodsRecordBuildFailure =>
          logErrorsAndContinue(e.getMessage, routes.SingleRecordController.onPageLoad(recordId))
      }
    }

  def onSubmitCountryOfOrigin(recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      goodsRecordConnector
        .getRecord(request.eori, recordId)
        .flatMap { recordResponse =>
          UpdateGoodsRecord
            .buildCountryOfOrigin(
              request.userAnswers,
              request.eori,
              recordId,
              recordResponse.category.isDefined
            ) match {
            case Right(model) =>
              auditService.auditFinishUpdateGoodsRecord(recordId, request.affinityGroup, model)
              updateCountryOfOriginAndSession(recordId, model, recordResponse)
            case Left(errors) =>
              Future.successful(
                logErrorsAndContinue(
                  errorMessage,
                  routes.SingleRecordController.onPageLoad(recordId),
                  errors
                )
              )
          }
        }
        .recoverWith { case e: Exception =>
          logger.error(s"Unable to fetch record $recordId: ${e.getMessage}")
          Future.successful(
            navigator.journeyRecovery(Some(RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)))
          )
        }
    }

  def onSubmitGoodsDescription(recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      UpdateGoodsRecord.validateGoodsDescription(request.userAnswers, recordId) match {
        case Right(goodsDescription) =>
          auditService.auditFinishUpdateGoodsRecord(
            recordId,
            request.affinityGroup,
            UpdateGoodsRecord(request.eori, recordId, goodsDescription = Some(goodsDescription))
          )
          for {
            // TODO: remove this flag when EIS has implemented the PATCH method - TGP-2417 and keep the call to patchGoodsRecord as default
            _ <- if (config.useEisPatchMethod) {
                   goodsRecordConnector.patchGoodsRecord(
                     UpdateGoodsRecord(request.eori, recordId, goodsDescription = Some(goodsDescription))
                   )
                 } else {
                   goodsRecordConnector.updateGoodsRecord(
                     UpdateGoodsRecord(request.eori, recordId, goodsDescription = Some(goodsDescription))
                   )
                 }

            updatedAnswers <- Future.fromTry(request.userAnswers.remove(GoodsDescriptionUpdatePage(recordId)))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(CyaUpdateRecordPage(recordId), NormalMode, updatedAnswers))
        case Left(errors)            =>
          Future.successful(
            logErrorsAndContinue(
              errorMessage,
              routes.SingleRecordController.onPageLoad(recordId),
              errors
            )
          )
      }
    }

  def onSubmitCommodityCode(recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      goodsRecordConnector
        .getRecord(request.eori, recordId)
        .flatMap { recordResponse =>
          val isCommCodeExpired = request.session.get(fromExpiredCommodityCodePage).contains("true")
          UpdateGoodsRecord
            .validateCommodityCode(
              request.userAnswers,
              recordId,
              recordResponse.category.isDefined,
              isCommCodeExpired
            ) match {
            case Right(commodity) =>
              auditService.auditFinishUpdateGoodsRecord(
                recordId,
                request.affinityGroup,
                UpdateGoodsRecord(request.eori, recordId, commodityCode = Some(commodity))
              )
              updateCommodityCodeAndSession(recordId, commodity, recordResponse)
            case Left(errors)     =>
              Future.successful(
                logErrorsAndContinue(
                  errorMessage,
                  routes.SingleRecordController.onPageLoad(recordId),
                  errors
                )
              )
          }
        }
        .recoverWith { case e: Exception =>
          logger.error(s"Unable to fetch record $recordId: ${e.getMessage}")
          Future.successful(
            navigator.journeyRecovery(Some(RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)))
          )
        }
    }

}
