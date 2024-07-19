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
import forms.LongerCommodityCodeFormProvider
import models.{CheckMode, Mode}
import models.helper.UpdateRecordJourney
import models.requests.DataRequest
import navigation.Navigator
import pages.{CountryOfOriginPageJourney, HasCorrectGoodsLongerCommodityCodePage, LongerCommodityCodePage}
import play.api.data.FormError
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{LongerCommodityQuery, RecordCategorisationsQuery}
import repositories.SessionRepository
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.LongerCommodityCodeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LongerCommodityCodeController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: LongerCommodityCodeFormProvider,
  ottConnector: OttConnector,
  val controllerComponents: MessagesControllerComponents,
  view: LongerCommodityCodeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form        = formProvider()
  private val validLength = 6
  private val maxLength   = 10

  def onPageLoad(mode: Mode, recordId: String): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val previousAnswerOpt = request.userAnswers.get(LongerCommodityCodePage(recordId))
      val shortComcodeOpt   = getShortCommodityCodeOpt(recordId, request, validLength)
      val preparedForm      = previousAnswerOpt.map(answer => form.fill(answer)).getOrElse(form)

      shortComcodeOpt match {
        case Some(shortComcode) if shortComcode.length == validLength =>
          Ok(view(preparedForm, mode, shortComcode, recordId))
        case _                                                        =>
          Redirect(routes.JourneyRecoveryController.onPageLoad().url)
      }
  }

  def onSubmit(mode: Mode, recordId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      val shortComcodeOpt         = getShortCommodityCodeOpt(recordId, request, validLength)
      val currentlyCategorisedOpt =
        request.userAnswers
          .get(RecordCategorisationsQuery)
          .flatMap(_.records.get(recordId))
          .map(_.commodityCode.padTo(maxLength, "0").mkString)

      shortComcodeOpt match {
        case Some(shortComcode) if shortComcode.length == validLength =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, shortComcode, recordId))),
              value => {
                val longCommodityCode   = (shortComcode + value).padTo(maxLength, "0").mkString
                val shouldRedirectToCya = currentlyCategorisedOpt.contains(longCommodityCode) && mode == CheckMode
                updateAnswersAndProceedWithJourney(
                  mode,
                  recordId,
                  value,
                  longCommodityCode,
                  shortComcode,
                  shouldRedirectToCya
                )
              }
            )
        case _                                                        =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
      }
    }

  private def getShortCommodityCodeOpt(
    recordId: String,
    request: DataRequest[AnyContent],
    validLength: Int
  ): Option[String] =
    request.userAnswers
      .get(RecordCategorisationsQuery)
      .flatMap(_.records.get(recordId))
      .flatMap(_.originalCommodityCode.map(_.reverse.dropWhile(_ == "0").reverse.padTo(validLength, "0").mkString))

  private def updateAnswersAndProceedWithJourney(
    mode: Mode,
    recordId: String,
    value: String,
    longCommodityCode: String,
    shortCode: String,
    shouldRedirect: Boolean
  )(implicit request: DataRequest[AnyContent]) = {
    val countryOfOrigin: String = request.userAnswers.get(CountryOfOriginPageJourney).get
    (for {
      validCommodityCode      <- ottConnector.getCommodityCode(
                                   longCommodityCode,
                                   request.eori,
                                   request.affinityGroup,
                                   UpdateRecordJourney,
                                   countryOfOrigin,
                                   Some(recordId)
                                 )
      updatedAnswers          <- Future.fromTry(request.userAnswers.set(LongerCommodityCodePage(recordId), value))
      cleansedAnswers         <- if (shouldRedirect) {
                                   Future.successful(updatedAnswers)
                                 } else {
                                   Future.fromTry(updatedAnswers.remove(HasCorrectGoodsLongerCommodityCodePage(recordId)))
                                 }
      updatedAnswersWithQuery <- Future.fromTry(cleansedAnswers.set(LongerCommodityQuery(recordId), validCommodityCode))
      _                       <- sessionRepository.set(updatedAnswersWithQuery)
    } yield Redirect(
      navigator.nextPage(
        LongerCommodityCodePage(recordId, shouldRedirectToCya = shouldRedirect),
        mode,
        updatedAnswersWithQuery
      )
    )).recover { case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
      val formWithApiErrors =
        form.copy(errors = Seq(FormError("value", getMessage("longerCommodityCode.error.invalid"))))
      BadRequest(view(formWithApiErrors, mode, shortCode, recordId))
    }
  }

  private def getMessage(key: String)(implicit messages: Messages): String = messages(key)

}
