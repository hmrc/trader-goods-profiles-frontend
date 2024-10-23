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

import connectors.OttConnector
import controllers.actions._
import forms.CountryOfOriginFormProvider
import models.helper.GoodsDetailsUpdate
import models.requests.DataRequest
import models.{Country, Mode, UserAnswers}
import navigation.Navigator
import pages.{CountryOfOriginPage, CountryOfOriginUpdatePage, HasCountryOfOriginChangePage, QuestionPage}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.libs.json.Reads
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Request, Result}
import queries.CountriesQuery
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import utils.SessionData._
import views.html.CountryOfOriginView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CountryOfOriginController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  profileAuth: ProfileAuthenticateAction,
  formProvider: CountryOfOriginFormProvider,
  val controllerComponents: MessagesControllerComponents,
  ottConnector: OttConnector,
  view: CountryOfOriginView,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends BaseController {

  private def retrieveAndStoreCountryData(implicit
    hc: HeaderCarrier,
    request: DataRequest[_]
  ): Future[(Seq[Country], UserAnswers)] =
    for {
      countries               <- ottConnector.getCountries
      updatedAnswersWithQuery <- Future.fromTry(request.userAnswers.set(CountriesQuery, countries))
      _                       <- sessionRepository.set(updatedAnswersWithQuery)
    } yield (countries, updatedAnswersWithQuery)

  def onPageLoadCreate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val submitAction = routes.CountryOfOriginController.onSubmitCreate(mode)
      request.userAnswers
        .get(CountriesQuery) match {
        case Some(countries) => Future.successful(displayViewCreate(countries, submitAction, request.userAnswers))
        case None            =>
          retrieveAndStoreCountryData.map(countriesAndQuery =>
            displayViewCreate(countriesAndQuery._1, submitAction, countriesAndQuery._2)
          )
      }
    }

  private def displayViewCreate(countries: Seq[Country], action: Call, userAnswers: UserAnswers)(implicit
    request: Request[_]
  ): Result = {
    val form         = formProvider(countries)
    val preparedForm = prepareForm(CountryOfOriginPage, form, userAnswers)
    Ok(view(preparedForm, action, countries))
  }

  private def prepareForm[T](page: QuestionPage[T], form: Form[T], userAnswers: UserAnswers)(implicit
    rds: Reads[T]
  ): Form[T] =
    userAnswers.get(page).map(form.fill).getOrElse(form)

  private def submitForm(countries: Seq[Country], mode: Mode, userAnswers: UserAnswers)(implicit
    request: Request[_]
  ): Future[Result] = {
    val form = formProvider(countries)
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(view(formWithErrors, routes.CountryOfOriginController.onSubmitCreate(mode), countries))
          ),
        value =>
          for {
            updatedAnswers <- Future.fromTry(userAnswers.set(CountryOfOriginPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(CountryOfOriginPage, mode, updatedAnswers))
      )
  }

  def onSubmitCreate(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      request.userAnswers
        .get(CountriesQuery) match {
        case Some(countries) => submitForm(countries, mode, request.userAnswers)
        case None            => throw new Exception("Countries should have been populated on page load.")
      }
    }

  def onPageLoadUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val submitAction = routes.CountryOfOriginController.onSubmitUpdate(mode, recordId)

      request.userAnswers.get(HasCountryOfOriginChangePage(recordId)) match {
        case None =>
          auditService
            .auditStartUpdateGoodsRecord(
              request.eori,
              request.affinityGroup,
              GoodsDetailsUpdate,
              recordId
            )
        case _    =>
      }

      request.userAnswers
        .get(CountriesQuery) match {
        case Some(countries) =>
          Future.successful(displayViewUpdate(countries, submitAction, request.userAnswers, recordId))
        case None            =>
          retrieveAndStoreCountryData.map(countriesAndQuery =>
            displayViewUpdate(countriesAndQuery._1, submitAction, countriesAndQuery._2, recordId)
          )
      }
    }

  def onSubmitUpdate(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      request.userAnswers
        .get(CountriesQuery) match {
        case Some(countries) =>
          val form = formProvider(countries)
          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    view(formWithErrors, routes.CountryOfOriginController.onSubmitUpdate(mode, recordId), countries)
                  )
                ),
              value => {
                val oldValueOpt    = request.userAnswers.get(CountryOfOriginUpdatePage(recordId))
                val isValueChanged = oldValueOpt.exists(_ != value)

                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(CountryOfOriginUpdatePage(recordId), value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(CountryOfOriginUpdatePage(recordId), mode, updatedAnswers))
                  .addingToSession(dataUpdated -> isValueChanged.toString)
                  .addingToSession(pageUpdated -> countryOfOrigin)
              }
            )
        case None            => throw new Exception("Countries should have been populated on page load.")
      }
    }

  private def displayViewUpdate(countries: Seq[Country], action: Call, userAnswers: UserAnswers, recordId: String)(
    implicit request: Request[_]
  ): Result = {
    val form         = formProvider(countries)
    val preparedForm = prepareForm(CountryOfOriginUpdatePage(recordId), form, userAnswers)

    Ok(view(preparedForm, action, countries))
  }

}
