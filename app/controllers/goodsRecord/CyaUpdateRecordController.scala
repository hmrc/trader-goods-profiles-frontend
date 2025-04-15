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

package controllers.goodsRecord

import cats.data
import cats.data.EitherNec
import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.{GoodsRecordConnector, OttConnector}
import controllers.BaseController
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, ProfileAuthenticateAction}
import models.requests.DataRequest
import models.router.requests.PutRecordRequest
import models.{CheckMode, Country, NormalMode, UpdateGoodsRecord, UserAnswers, ValidationError}
import navigation.GoodsRecordNavigator
import org.apache.pekko.Done
import pages.goodsRecord.*
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import queries.CountriesQuery
import repositories.SessionRepository
import services.{AuditService, CommodityService}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants.{commodityCodeKey, countryOfOriginKey, goodsDescriptionKey, openAccreditationErrorCode, productReferenceKey}
import utils.SessionData.{dataRemoved, dataUpdated, pageUpdated}
import viewmodels.checkAnswers.goodsRecord.{CommodityCodeSummary, CountryOfOriginSummary, GoodsDescriptionSummary, ProductReferenceSummary}
import viewmodels.govuk.summarylist.*
import views.html.goodsRecord.CyaUpdateRecordView

import scala.concurrent.{ExecutionContext, Future}

class CyaUpdateRecordController @Inject() (
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
  config: FrontendAppConfig,
  commodityService: CommodityService
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val errorMessage: String = "Unable to update Goods Record."

  def onPageLoadCountryOfOrigin(recordId: String): Action[AnyContent] =
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
                controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitCountryOfOrigin(recordId)
              getCountryOfOriginAnswer(request.userAnswers, recordId).map {
                case Some(answer) =>
                  val list = SummaryListViewModel(
                    Seq(
                      CountryOfOriginSummary
                        .rowUpdateCya(answer, recordId, CheckMode)
                    )
                  )
                  Ok(view(list, onSubmitAction, countryOfOriginKey))
                case _            => Redirect(controllers.problem.routes.JourneyRecoveryController.onPageLoad().url)
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

  def onPageLoadGoodsDescription(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      UpdateGoodsRecord.validateGoodsDescription(request.userAnswers, recordId) match {
        case Right(goodsDescription) =>
          val onSubmitAction =
            controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitGoodsDescription(recordId)

          val list = SummaryListViewModel(
            Seq(GoodsDescriptionSummary.rowUpdateCya(goodsDescription, recordId, CheckMode))
          )
          Ok(view(list, onSubmitAction, goodsDescriptionKey))
        case Left(errors)            =>
          logErrorsAndContinue(
            errorMessage,
            controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId),
            errors
          )
      }
    }

  def onPageLoadproductReference(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData) { implicit request =>
      UpdateGoodsRecord.validateproductReference(request.userAnswers, recordId) match {
        case Right(productReference) =>
          val onSubmitAction =
            controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitproductReference(recordId)

          val list = SummaryListViewModel(
            Seq(ProductReferenceSummary.row(productReference, recordId, CheckMode, recordLocked = false))
          )
          Ok(view(list, onSubmitAction, productReferenceKey))
        case Left(errors)            =>
          logErrorsAndContinue(
            errorMessage,
            controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId),
            errors
          )
      }
    }

  def onPageLoadCommodityCode(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      goodsRecordConnector
        .getRecord(recordId)
        .flatMap { recordResponse =>
          commodityService.isCommodityCodeValid(recordResponse.comcode, recordResponse.countryOfOrigin)(
            request
          ) flatMap { isCommCodeValid =>
            UpdateGoodsRecord
              .validateCommodityCode(
                request.userAnswers,
                recordId,
                recordResponse.category.isDefined,
                !isCommCodeValid
              ) match {
              case Right(commodity) =>
                val onSubmitAction =
                  controllers.goodsRecord.routes.CyaUpdateRecordController.onSubmitCommodityCode(recordId)

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
                    controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId),
                    errors
                  )
                )
            }
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
          case None            =>
            getCountries(userAnswers).map { countries =>
              Some(findCountryName(countries, answer))
            }
        }
      case _            => Future.successful(None)
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
    private val errorsAsString = errors.toChain.toList.map(_.message).mkString(", ")

    override def getMessage: String = s"$errorMessage Missing pages: $errorsAsString"
  }

  private def updateGoodsRecordIfValueChanged(
    newValue: String,
    oldValue: String,
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

  private def updateGoodsRecordIfPutValueChanged(
    newValue: String,
    oldValue: String,
    newUpdateGoodsRecord: UpdateGoodsRecord,
    putRecordRequest: PutRecordRequest
  )(implicit hc: HeaderCarrier): Future[Done] =
    if (newValue != oldValue) {

      // TODO: remove this flag when EIS has implemented the PATCH method - TGP-2417 and keep the call to putGoodsRecord as default
      if (config.useEisPatchMethod) {
        goodsRecordConnector.putGoodsRecord(
          putRecordRequest,
          newUpdateGoodsRecord.recordId
        )
      } else {
        goodsRecordConnector.updateGoodsRecord(
          newUpdateGoodsRecord
        )
      }

    } else {
      Future.successful(Done)
    }

  def onSubmitproductReference(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      (for {
        productReference  <-
          handleValidateError(UpdateGoodsRecord.validateproductReference(request.userAnswers, recordId))
        updateGoodsRecord <-
          Future.successful(UpdateGoodsRecord(request.eori, recordId, productReference = Some(productReference)))
        _                  = auditService.auditFinishUpdateGoodsRecord(recordId, request.affinityGroup, updateGoodsRecord)
        oldRecord         <- goodsRecordConnector.getRecord(recordId)
        _                 <- updateGoodsRecordIfValueChanged(productReference, oldRecord.traderRef, updateGoodsRecord)
        updatedAnswers    <- Future.fromTry(request.userAnswers.remove(ProductReferenceUpdatePage(recordId)))
        _                 <- sessionRepository.set(updatedAnswers)
      } yield Redirect(navigator.nextPage(CyaUpdateRecordPage(recordId), NormalMode, updatedAnswers)))
        .recover(handleRecover(recordId))
    }

  def onSubmitCountryOfOrigin(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      (for {
        oldRecord                <- goodsRecordConnector.getRecord(recordId)
        countryOfOrigin          <-
          handleValidateError(
            UpdateGoodsRecord.validateCountryOfOrigin(request.userAnswers, recordId, oldRecord.category.isDefined)
          )
        updateGoodsRecord        <-
          Future.successful(UpdateGoodsRecord(request.eori, recordId, countryOfOrigin = Some(countryOfOrigin)))
        putGoodsRecord           <- Future.successful(
                                      PutRecordRequest(
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
                                    )
        _                         = auditService.auditFinishUpdateGoodsRecord(recordId, request.affinityGroup, updateGoodsRecord)
        _                        <- updateGoodsRecordIfPutValueChanged(
                                      countryOfOrigin,
                                      oldRecord.countryOfOrigin,
                                      updateGoodsRecord,
                                      putGoodsRecord
                                    )
        updatedAnswersWithChange <- Future.fromTry(request.userAnswers.remove(HasCountryOfOriginChangePage(recordId)))
        updatedAnswers           <- Future.fromTry(updatedAnswersWithChange.remove(CountryOfOriginUpdatePage(recordId)))
        _                        <- sessionRepository.set(updatedAnswers)
      } yield Redirect(navigator.nextPage(CyaUpdateRecordPage(recordId), NormalMode, updatedAnswers)))
        .recover(handleRecover(recordId))
    }

  def onSubmitGoodsDescription(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      (for {
        goodsDescription  <-
          handleValidateError(UpdateGoodsRecord.validateGoodsDescription(request.userAnswers, recordId))
        updateGoodsRecord <-
          Future.successful(UpdateGoodsRecord(request.eori, recordId, goodsDescription = Some(goodsDescription)))
        _                  = auditService.auditFinishUpdateGoodsRecord(recordId, request.affinityGroup, updateGoodsRecord)
        oldRecord         <- goodsRecordConnector.getRecord(recordId)
        _                 <- updateGoodsRecordIfValueChanged(goodsDescription, oldRecord.goodsDescription, updateGoodsRecord)
        updatedAnswers    <- Future.fromTry(request.userAnswers.remove(GoodsDescriptionUpdatePage(recordId)))
        _                 <- sessionRepository.set(updatedAnswers)
      } yield Redirect(navigator.nextPage(CyaUpdateRecordPage(recordId), NormalMode, updatedAnswers)))
        .recover(handleRecover(recordId))
    }

  def onSubmitCommodityCode(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      (for {
        oldRecord                <- goodsRecordConnector.getRecord(recordId)
        isCommodityValid         <- commodityService.isCommodityCodeValid(oldRecord.comcode, oldRecord.countryOfOrigin)
        commodity                <-
          handleValidateError(
            UpdateGoodsRecord
              .validateCommodityCode(
                request.userAnswers,
                recordId,
                oldRecord.category.isDefined,
                !isCommodityValid
              )
          )
        updateGoodsRecord        <-
          Future.successful(UpdateGoodsRecord(request.eori, recordId, commodityCode = Some(commodity)))
        putGoodsRecord           <- Future.successful(
                                      PutRecordRequest(
                                        actorId = oldRecord.eori,
                                        traderRef = oldRecord.traderRef,
                                        comcode = commodity.commodityCode,
                                        goodsDescription = oldRecord.goodsDescription,
                                        countryOfOrigin = oldRecord.countryOfOrigin,
                                        category = None,
                                        assessments = oldRecord.assessments,
                                        supplementaryUnit = oldRecord.supplementaryUnit,
                                        measurementUnit = oldRecord.measurementUnit,
                                        comcodeEffectiveFromDate = oldRecord.comcodeEffectiveFromDate,
                                        comcodeEffectiveToDate = oldRecord.comcodeEffectiveToDate
                                      )
                                    )
        _                         = auditService.auditFinishUpdateGoodsRecord(recordId, request.affinityGroup, updateGoodsRecord)
        _                        <- updateGoodsRecordIfPutValueChanged(
                                      commodity.commodityCode,
                                      oldRecord.comcode,
                                      updateGoodsRecord,
                                      putGoodsRecord
                                    )
        updatedAnswersWithChange <-
          Future.fromTry(request.userAnswers.remove(HasCommodityCodeChangePage(recordId)))
        updatedAnswers           <- Future.fromTry(updatedAnswersWithChange.remove(CommodityCodeUpdatePage(recordId)))
        _                        <- sessionRepository.set(updatedAnswers)
      } yield Redirect(navigator.nextPage(CyaUpdateRecordPage(recordId), NormalMode, updatedAnswers)))
        .recover(handleRecover(recordId))
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
