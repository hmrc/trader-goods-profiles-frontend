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

import javax.inject.Inject
import models.{Country, Mode, UserAnswers}
import navigation.Navigator
import pages.CountryOfOriginPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import queries.CountriesQuery
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CountryOfOriginView

import scala.concurrent.{ExecutionContext, Future}

class CountryOfOriginController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: CountryOfOriginFormProvider,
  val controllerComponents: MessagesControllerComponents,
  ottConnector: OttConnector,
  view: CountryOfOriginView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      request.userAnswers
        .get(CountriesQuery) match {
        case Some(countries) => Future.successful(displayView(countries, mode, request.userAnswers, recordId))
        case None            =>
          for {
            countries               <- ottConnector.getCountries
            updatedAnswersWithQuery <- Future.fromTry(request.userAnswers.set(CountriesQuery, countries))
            _                       <- sessionRepository.set(updatedAnswersWithQuery)
          } yield displayView(countries, mode, updatedAnswersWithQuery, recordId)
      }
    }

  def displayView(countries: Seq[Country], mode: Mode, userAnswers: UserAnswers, recordId: String)(implicit
    request: Request[_]
  ): Result = {
    val form         = formProvider(countries)
    val preparedForm = userAnswers.get(CountryOfOriginPage(recordId)) match {
      case None        => form
      case Some(value) => form.fill(value)
    }
    Ok(view(preparedForm, mode, countries, recordId))
  }

  def submitForm(countries: Seq[Country], mode: Mode, userAnswers: UserAnswers, recordId: String)(implicit
    request: Request[_]
  ): Future[Result] = {
    val form = formProvider(countries)
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, countries, recordId))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(userAnswers.set(CountryOfOriginPage(recordId), value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(CountryOfOriginPage(recordId), mode, updatedAnswers))
      )
  }

  def onSubmit(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      request.userAnswers
        .get(CountriesQuery) match {
        case Some(countries) => submitForm(countries, mode, request.userAnswers, recordId)
        case None            => throw new Exception("Countries should have been populated on page load.")
      }
    }
}
