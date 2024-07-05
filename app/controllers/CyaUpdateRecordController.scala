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
import models.PageUpdate._
import models.{Country, CountryOfOriginPageUpdate, PageUpdate, UpdateGoodsRecord, UserAnswers, ValidationError}
import pages.CountryOfOriginPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import queries.CountriesQuery
import repositories.SessionRepository
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
  view: CyaUpdateRecordView,
  goodsRecordConnector: GoodsRecordConnector,
  ottConnector: OttConnector,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(recordId: String, pageUpdate: PageUpdate): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      UpdateGoodsRecord.build(request.userAnswers, request.eori, recordId, pageUpdate) match {
        case Right(_)     =>
          pageUpdate match {
            case CountryOfOriginPageUpdate =>
              getCountryOfOriginAnswer(request.userAnswers, recordId).map { answer =>
                displayView(recordId, pageUpdate, answer)
              }
            case _                         =>
              Future.successful(displayView(recordId, pageUpdate, getAnswer(request.userAnswers, recordId, pageUpdate)))
          }
        case Left(errors) => Future.successful(logErrorsAndContinue(errors))
      }
    }

  def getCountryOfOriginAnswer(userAnswers: UserAnswers, recordId: String)(implicit
    request: Request[_]
  ): Future[String] =
    userAnswers.get(CountryOfOriginPage) match {
      case Some(answer) =>
        userAnswers.get(CountriesQuery) match {
          case Some(countries) => Future.successful(findCountryName(countries, answer))
          case None            =>
            getCountries(userAnswers).map { countries =>
              findCountryName(countries, answer)
            }
        }
    }

  def getAnswer(userAnswers: UserAnswers, recordId: String, pageUpdate: PageUpdate)(implicit
    request: Request[_]
  ): String =
    userAnswers.get(getPage(pageUpdate, recordId)) match {
      case Some(answer) => answer
    }

  def findCountryName(countries: Seq[Country], answer: String)(implicit
    request: Request[_]
  ): String =
    countries.find(country => country.id == answer).map(_.description).getOrElse(answer)

  def getCountries(userAnswers: UserAnswers)(implicit request: Request[_]): Future[Seq[Country]] =
    for {
      countries               <- ottConnector.getCountries
      updatedAnswersWithQuery <- Future.fromTry(userAnswers.set(CountriesQuery, countries))
      _                       <- sessionRepository.set(updatedAnswersWithQuery)
    } yield countries

  def displayView(recordId: String, pageUpdate: PageUpdate, answer: String)(implicit
    request: Request[_]
  ): Result = {
    val list = SummaryListViewModel(
      rows = Seq(
        UpdateRecordSummary.row(
          answer,
          getPageUpdateLabel(pageUpdate),
          getPageUpdateHidden(pageUpdate),
          getPageUpdateChangeLink(pageUpdate, recordId)
        )
      )
    )
    Ok(view(list, recordId, pageUpdate))
  }

  def onSubmit(recordId: String, pageUpdate: PageUpdate): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      UpdateGoodsRecord.build(request.userAnswers, request.eori, recordId, pageUpdate) match {
        case Right(model) =>
          for {
            _              <- goodsRecordConnector.updateGoodsRecord(model)
            updatedAnswers <- Future.fromTry(request.userAnswers.remove(getPage(pageUpdate, recordId)))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(routes.HomePageController.onPageLoad())
        case Left(errors) => Future.successful(logErrorsAndContinue(errors))
      }
    }

  def logErrorsAndContinue(errors: data.NonEmptyChain[ValidationError]): Result = {
    val errorMessages = errors.toChain.toList.map(_.message).mkString(", ")

    //TODO: route to correct location
    val continueUrl = RedirectUrl(routes.HomePageController.onPageLoad().url)

    logger.warn(s"Unable to update Goods Record.  Missing pages: $errorMessages")
    Redirect(routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)))
  }
}
