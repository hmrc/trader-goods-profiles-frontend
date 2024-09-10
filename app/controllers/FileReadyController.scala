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

import connectors.DownloadDataConnector
import controllers.actions._
import models.DownloadDataStatus.{FileReadySeen, FileReadyUnseen}
import models.DownloadDataSummary
import navigation.Navigator
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.DateTimeFormats.dateTimeFormat
import views.html.FileReadyView

import java.time.{Instant, ZoneOffset}
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileReadyController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  navigator: Navigator,
  profileAuth: ProfileAuthenticateAction,
  downloadDataConnector: DownloadDataConnector,
  val controllerComponents: MessagesControllerComponents,
  view: FileReadyView
)(implicit ec: ExecutionContext)
    extends BaseController {

  def onPageLoad(): Action[AnyContent] = (identify andThen profileAuth andThen getData andThen requireData).async {
    implicit request =>
      downloadDataConnector.getDownloadDataSummary(request.eori).flatMap {
        case Some(downloadDataSummary) =>
          downloadDataSummary.status match {
            case FileReadySeen | FileReadyUnseen =>
              downloadDataSummary.fileInfo match {
                case Some(info) =>
                  downloadDataConnector
                    .submitDownloadDataSummary(
                      DownloadDataSummary(request.eori, FileReadySeen, downloadDataSummary.fileInfo)
                    )
                    .flatMap { _ =>
                      downloadDataConnector.getDownloadData(request.eori).map {
                        case Some(downloadData) =>
                          Ok(
                            view(
                              info.fileSize,
                              downloadData.downloadURL,
                              convertToDateString(info.fileCreated),
                              convertToDateString(info.fileCreated.plus(info.retentionDays.toInt, ChronoUnit.DAYS))
                            )
                          )
                        case None               => navigator.journeyRecovery()
                      }
                    }
                case _          => Future.successful(navigator.journeyRecovery())
              }
            case _                               => Future.successful(navigator.journeyRecovery())
          }
        case _                         => Future.successful(navigator.journeyRecovery())
      }
  }

  def convertToDateString(instant: Instant)(implicit messages: Messages): String = {
    implicit val lang: Lang = messages.lang
    instant.atZone(ZoneOffset.UTC).toLocalDate().format(dateTimeFormat())
  }
}
