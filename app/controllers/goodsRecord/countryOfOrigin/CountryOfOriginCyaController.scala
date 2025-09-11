/*
 * Copyright 2025 HM Revenue & Customs
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

import cats.data.EitherNec
import com.google.inject.Inject
import connectors.{GoodsRecordConnector, OttConnector}
import controllers.BaseController
import controllers.actions.*
import exceptions.GoodsRecordBuildFailure
import helpers.GoodsRecordRecovery
import models.*
import navigation.GoodsRecordNavigator
import org.apache.pekko.Done
import pages.goodsRecord.*
import play.api.i18n.MessagesApi
import play.api.mvc.*
import queries.CountriesQuery
import repositories.SessionRepository
import services.{AuditService, AutoCategoriseService, GoodsRecordUpdateService}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.Constants.*
import utils.SessionData.dataUpdated
import viewmodels.checkAnswers.goodsRecord.*
import viewmodels.govuk.summarylist.*
import views.html.goodsRecord.CyaUpdateRecordView
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

class CountryOfOriginCyaController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  val controllerComponents: MessagesControllerComponents,
  auditService: AuditService,
  navigator: GoodsRecordNavigator,
  view: CyaUpdateRecordView,
  goodsRecordConnector: GoodsRecordConnector,
  ottConnector: OttConnector,
  sessionRepository: SessionRepository,
  autoCategoriseService: AutoCategoriseService,
  goodsRecordUpdateService: GoodsRecordUpdateService
)(implicit ec: ExecutionContext)
    extends BaseController
    with GoodsRecordRecovery {

  override val recoveryLogger: Logger = Logger(this.getClass)

  private val errorMessage: String = "Unable to update Goods Record."

  def onPageLoad(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      goodsRecordConnector
        .getRecord(recordId)
        .flatMap { recordResponse =>
          UpdateGoodsRecord.validateCountryOfOrigin(
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
                      CountryOfOriginSummary.rowUpdateCya(answer, recordId, CheckMode)
                    )
                  )
                  Ok(view(list, onSubmitAction, countryOfOriginKey))

                case None =>
                  Redirect(
                    controllers.problem.routes.JourneyRecoveryController.onPageLoad(
                      Some(RedirectUrl(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId).url))
                    )
                  )
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
        .recoverWith { case e =>
          Future.successful(
            navigator.journeyRecovery(
              Some(RedirectUrl(controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId).url))
            )
          )
        }
    }

  def onSubmit(recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      (for {
        // Fetch existing record
        oldRecord <- goodsRecordConnector.getRecord(recordId)

        // Get original country from session
        originalCountry <-
          request.userAnswers
            .get(OriginalCountryOfOriginPage(recordId))
            .map(Future.successful)
            .getOrElse(Future.failed(new Exception(s"Original country of origin not found in session for $recordId")))

        countryOfOrigin <-
          handleValidateError(
            UpdateGoodsRecord.validateCountryOfOrigin(request.userAnswers, recordId, oldRecord.category.isDefined)
          )

        oldValNormalized        =
          Option(originalCountry).map(_.trim).getOrElse("")
        newValNormalized        =
          countryOfOrigin.trim
        countryHasReallyChanged = oldValNormalized != newValNormalized

        updateGoodsRecord = UpdateGoodsRecord(
                              eori = request.eori,
                              recordId = recordId,
                              countryOfOrigin = Some(countryOfOrigin)
                            )

        _ = auditService.auditFinishUpdateGoodsRecord(recordId, request.affinityGroup, updateGoodsRecord)

        // PATCH backend if needed
        _              <- goodsRecordUpdateService.updateIfChanged(
                            oldValue = oldRecord.countryOfOrigin.trim,
                            newValue = newValNormalized,
                            updateGoodsRecord = updateGoodsRecord,
                            oldRecord = oldRecord,
                            patch = true
                          )

        // Update session with new country
        updatedAnswers <-
          Future.fromTry(request.userAnswers.set(OriginalCountryOfOriginPage(recordId), countryOfOrigin))
        _              <- sessionRepository.set(updatedAnswers)

        // Clean temporary session pages
        cleanedAnswers <- Future.fromTry(
                            updatedAnswers
                              .remove(HasCountryOfOriginChangePage(recordId))
                              .flatMap(_.remove(CountryOfOriginUpdatePage(recordId)))
                          )
        _              <- sessionRepository.set(cleanedAnswers)

        // Get categorisation info
        categorisationInfoOpt <- autoCategoriseService.getCategorisationInfoForRecord(recordId, cleanedAnswers)
        isAutoCategorisable    = categorisationInfoOpt.exists(_.isAutoCategorisable)

        // Remove manual category ONLY if country actually changed AND record is not auto-categorisable
        _ <- if (countryHasReallyChanged && !isAutoCategorisable) {
               goodsRecordUpdateService.removeManualCategory(request.eori, recordId, oldRecord)
             } else Future.successful(Done)

      } yield {
        // Redirect logic
        val redirect =
          if (!countryHasReallyChanged) {
            // Country did not change -> go to SingleRecord
            controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId)
          } else if (isAutoCategorisable) {
            // Auto-categorisable -> SingleRecord
            controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId)
          } else {
            // Manual & country changed -> UpdatedCountry page
            controllers.goodsRecord.countryOfOrigin.routes.UpdatedCountryOfOriginController.onPageLoad(recordId)
          }

        // Banner only for auto-categorised + country changed
        val showBanner = countryHasReallyChanged && isAutoCategorisable

        Redirect(redirect)
          .addingToSession(
            "countryOfOriginChanged"        -> countryHasReallyChanged.toString,
            "showCommodityCodeChangeBanner" -> showBanner.toString
          )
          .removingFromSession(dataUpdated)
      }).recover(handleRecover(recordId))
    }

  private def handleValidateError[T](result: EitherNec[ValidationError, T]): Future[T] =
    result match {
      case Right(value) => Future.successful(value)
      case Left(errors) => Future.failed(GoodsRecordBuildFailure(errors))
    }

  private def getCountryOfOriginAnswer(
    userAnswers: UserAnswers,
    recordId: String
  )(implicit request: Request[_]): Future[Option[String]] =
    userAnswers.get(CountryOfOriginUpdatePage(recordId)) match {
      case Some(answer) =>
        userAnswers.get(CountriesQuery) match {
          case Some(countries) => Future.successful(Some(findCountryName(countries, answer)))
          case None            =>
            getCountries(userAnswers).map { countries =>
              Some(findCountryName(countries, answer))
            }
        }
      case None         => Future.successful(None)
    }

  private def findCountryName(countries: Seq[Country], answer: String): String =
    countries.find(_.id == answer).map(_.description).getOrElse(answer)

  private def getCountries(userAnswers: UserAnswers)(implicit request: Request[_]): Future[Seq[Country]] =
    for {
      countries      <- ottConnector.getCountries
      updatedAnswers <- Future.fromTry(userAnswers.set(CountriesQuery, countries))
      _              <- sessionRepository.set(updatedAnswers)
    } yield countries
}
