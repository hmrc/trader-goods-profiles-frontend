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
import com.google.inject.Inject
import connectors.{GoodsRecordConnector, OttConnector}
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import logging.Logging
import models.{CheckMode, Country, NormalMode, UpdateGoodsRecord, UserAnswers, ValidationError}
import navigation.Navigator
import pages._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import queries.CountriesQuery
import repositories.SessionRepository
import services.{AuditService, CategorisationService}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
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
  categorisationService: CategorisationService,
  navigator: Navigator
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

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
                Ok(view(list, onSubmitAction))
              }
            case Left(errors) =>
              Future.successful(
                logErrorsAndContinue(errors, recordId)
              )
          }
        }
        .recoverWith { case e: Exception =>
          logger.error(s"Unable to fetch record $recordId: ${e.getMessage}")
          Future.successful(
            Redirect(
              routes.JourneyRecoveryController
                .onPageLoad(Some(RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)))
            )
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
          Ok(view(list, onSubmitAction))
        case Left(errors)            =>
          logErrorsAndContinue(errors, recordId)
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
          Ok(view(list, onSubmitAction))
        case Left(errors)           =>
          logErrorsAndContinue(errors, recordId)
      }
    }

  def onPageLoadCommodityCode(recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      goodsRecordConnector
        .getRecord(request.eori, recordId)
        .flatMap { recordResponse =>
          UpdateGoodsRecord
            .validateCommodityCode(request.userAnswers, recordId, recordResponse.category.isDefined) match {
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
              Future.successful(Ok(view(list, onSubmitAction)))
            case Left(errors)     =>
              Future.successful(
                logErrorsAndContinue(errors, recordId)
              )
          }
        }
        .recoverWith { case e: Exception =>
          logger.error(s"Unable to fetch record $recordId: ${e.getMessage}")
          Future.successful(
            Redirect(
              routes.JourneyRecoveryController
                .onPageLoad(Some(RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)))
            )
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

  def onSubmitTraderReference(recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      UpdateGoodsRecord.validateTraderReference(request.userAnswers, recordId) match {
        case Right(traderReference) =>
          auditService.auditFinishUpdateGoodsRecord(
            recordId,
            request.affinityGroup,
            UpdateGoodsRecord(request.eori, recordId, traderReference = Some(traderReference))
          )
          for {
            _              <- goodsRecordConnector.updateGoodsRecord(
                                UpdateGoodsRecord(request.eori, recordId, traderReference = Some(traderReference))
                              )
            updatedAnswers <- Future.fromTry(request.userAnswers.remove(TraderReferenceUpdatePage(recordId)))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(CyaUpdateRecordPage(recordId), NormalMode, updatedAnswers))
        case Left(errors)           =>
          Future.successful(
            logErrorsAndContinue(errors, recordId)
          )
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
              for {
                _                        <- goodsRecordConnector.updateGoodsRecord(model)
                updatedAnswersWithChange <-
                  Future.fromTry(request.userAnswers.remove(HasCountryOfOriginChangePage(recordId)))
                updatedAnswers           <- Future.fromTry(updatedAnswersWithChange.remove(CountryOfOriginUpdatePage(recordId)))
                _                        <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(CyaUpdateRecordPage(recordId), NormalMode, updatedAnswers))
            case Left(errors) =>
              Future.successful(
                logErrorsAndContinue(errors, recordId)
              )
          }
        }
        .recoverWith { case e: Exception =>
          logger.error(s"Unable to fetch record $recordId: ${e.getMessage}")
          Future.successful(
            Redirect(
              routes.JourneyRecoveryController
                .onPageLoad(Some(RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)))
            )
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
            _              <- goodsRecordConnector.updateGoodsRecord(
                                UpdateGoodsRecord(request.eori, recordId, goodsDescription = Some(goodsDescription))
                              )
            updatedAnswers <- Future.fromTry(request.userAnswers.remove(GoodsDescriptionUpdatePage(recordId)))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(CyaUpdateRecordPage(recordId), NormalMode, updatedAnswers))
        case Left(errors)            =>
          Future.successful(
            logErrorsAndContinue(errors, recordId)
          )
      }
    }

  def onSubmitCommodityCode(recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      goodsRecordConnector
        .getRecord(request.eori, recordId)
        .flatMap { recordResponse =>
          UpdateGoodsRecord
            .validateCommodityCode(request.userAnswers, recordId, recordResponse.category.isDefined) match {
            case Right(commodity) =>
              auditService.auditFinishUpdateGoodsRecord(
                recordId,
                request.affinityGroup,
                UpdateGoodsRecord(request.eori, recordId, commodityCode = Some(commodity), category = Some(1))
              )
              for {
                _                        <- goodsRecordConnector.updateGoodsRecord(
                                              UpdateGoodsRecord(request.eori, recordId, commodityCode = Some(commodity), category = Some(1))
                                            )
                updatedAnswersWithChange <-
                  Future.fromTry(request.userAnswers.remove(HasCommodityCodeChangePage(recordId)))
                updatedAnswers           <- Future.fromTry(updatedAnswersWithChange.remove(CommodityCodeUpdatePage(recordId)))
                _                        <- sessionRepository.set(updatedAnswers)
                _                        <- categorisationService.updateCategorisationWithUpdatedCommodityCode(request, recordId)
              } yield Redirect(navigator.nextPage(CyaUpdateRecordPage(recordId), NormalMode, updatedAnswers))
            case Left(errors)     =>
              Future.successful(
                logErrorsAndContinue(errors, recordId)
              )
          }
        }
        .recoverWith { case e: Exception =>
          logger.error(s"Unable to fetch record $recordId: ${e.getMessage}")
          Future.successful(
            Redirect(
              routes.JourneyRecoveryController
                .onPageLoad(Some(RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url)))
            )
          )
        }
    }

  def logErrorsAndContinue(errors: data.NonEmptyChain[ValidationError], recordId: String): Result = {
    val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")

    logger.error(s"Unable to update Goods Record.  Missing pages: $errorMessages")
    Redirect(
      routes.JourneyRecoveryController.onPageLoad(
        Some(RedirectUrl(routes.SingleRecordController.onPageLoad(recordId).url))
      )
    )
  }
}
