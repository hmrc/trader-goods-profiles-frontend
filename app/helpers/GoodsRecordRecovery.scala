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

package helpers

import exceptions.GoodsRecordBuildFailure
import models.requests.DataRequest
import play.api.Logger
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.Constants
import utils.SessionData.*

trait GoodsRecordRecovery {

  // Distinct logger name to avoid clash
  val recoveryLogger: Logger

  private val openAccreditationErrorCode: String = Constants.openAccreditationErrorCode

  def logErrorsAndRedirect(message: String, redirectTo: Call): Result = {
    recoveryLogger.warn(s"[GoodsRecordRecovery] $message")
    Redirect(redirectTo).flashing("error" -> message)
  }

  def handleRecover(recordId: String)(implicit request: DataRequest[AnyContent]): PartialFunction[Throwable, Result] = {
    case e: GoodsRecordBuildFailure =>
      logErrorsAndRedirect(
        e.getMessage,
        controllers.goodsRecord.routes.SingleRecordController.onPageLoad(recordId)
      )

    case e: UpstreamErrorResponse if e.message.contains(openAccreditationErrorCode) =>
      Redirect(controllers.routes.RecordLockedController.onPageLoad(recordId))
        .removingFromSession(dataRemoved, dataUpdated, pageUpdated)
  }
}
