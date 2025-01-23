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

import connectors.OttConnector
import controllers.BaseController
import controllers.actions._
import forms.goodsRecord.CountryOfOriginFormProvider
import models.requests.DataRequest
import models.{Country, Mode, UserAnswers}
import navigation.GoodsRecordNavigator
import pages.QuestionPage
import pages.goodsRecord.CountryOfOriginPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.libs.json.Reads
import play.api.mvc._
import queries.CountriesQuery
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.goodsRecord.CountryOfOriginView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateCountryOfOriginController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: GoodsRecordNavigator,
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

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      val submitAction = controllers.goodsRecord.countryOfOrigin.routes.CreateCountryOfOriginController.onSubmit(mode)
      request.userAnswers
        .get(CountriesQuery) match {
        case Some(countries) => Future.successful(displayViewCreate(countries, submitAction, request.userAnswers, mode))
        case None            =>
          retrieveAndStoreCountryData.map(countriesAndQuery =>
            displayViewCreate(countriesAndQuery._1, submitAction, countriesAndQuery._2, mode)
          )
      }
    }

  private def displayViewCreate(countries: Seq[Country], action: Call, userAnswers: UserAnswers, mode: Mode)(implicit
    request: Request[_]
  ): Result = {
    val form         = formProvider(countries)
    val preparedForm = prepareForm(CountryOfOriginPage, form, userAnswers)
    Ok(view(preparedForm, action, countries, mode, recordId = None))
  }

  private def prepareForm[T](page: QuestionPage[T], form: Form[T], userAnswers: UserAnswers)(implicit
    rds: Reads[T]
  ): Form[T] =
    userAnswers.get(page).map(form.fill).getOrElse(form)

  private def submitForm(countries: Seq[Country], mode: Mode, userAnswers: UserAnswers, recordId: Option[String])(
    implicit request: Request[_]
  ): Future[Result] = {
    val form = formProvider(countries)
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              view(
                formWithErrors,
                controllers.goodsRecord.countryOfOrigin.routes.CreateCountryOfOriginController.onSubmit(mode),
                countries,
                mode,
                recordId
              )
            )
          ),
        value =>
          for {
            updatedAnswers <- Future.fromTry(userAnswers.set(CountryOfOriginPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(CountryOfOriginPage, mode, updatedAnswers))
      )
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen profileAuth andThen getData andThen requireData).async { implicit request =>
      request.userAnswers
        .get(CountriesQuery) match {
        case Some(countries) => submitForm(countries, mode, request.userAnswers, recordId = None)
        case None            => throw new Exception("Countries should have been populated on page load.")
      }
    }




}
