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

package controllers.actions

import models.UserAnswers

import javax.inject.Inject
import models.requests.{AuthorisedRequest, DataRequest, OptionalDataRequest}
import play.api.mvc.{ActionTransformer, Result}
import repositories.SessionRepository
import services.SessionService

import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalActionImpl @Inject() (
  val sessionService: SessionService
)(implicit val executionContext: ExecutionContext)
    extends DataRetrievalAction {

  // got to any page
  // get answers
  // if empty create
  // and redirect to start page

  override protected def refine[A](request: OptionalDataRequest[A]): Future[Either[Result, DataRequest[A]]] =
    (for {
      userAnswers <- sessionService.getUserAnswers(request.internalId)
      either <- getOrCreateAndRedirect()
    } yield either)

  }

  private def getOrCreateAndRedirect(userAnswers: Option[UserAnswers], request: AuthorisedRequest[_]) = {
    userAnswers match {
      case Some(userAnswers) =>
        Right(DataRequest(request.request, request.internalId, userAnswers, request.eori))

      case None =>
        val emptyUserAnswers = UserAnswers(request.internalId, None)
        sessionService.setUserAnswers(emptyUserAnswers).map { userAnswers =>
          DataRequest(request.request, request.internalId, userAnswers, request.eori)
        }
       // Left(Profile set up page)
    }
  }
}

trait DataRetrievalAction extends ActionTransformer[AuthorisedRequest, OptionalDataRequest]
