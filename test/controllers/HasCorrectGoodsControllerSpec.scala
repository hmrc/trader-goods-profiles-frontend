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

import base.SpecBase
import base.TestConstants.{testEori, testRecordId}
import config.FrontendAppConfig
import connectors.GoodsRecordConnector
import forms.HasCorrectGoodsFormProvider
import models.router.requests.PutRecordRequest
import models.{Commodity, NormalMode, UpdateGoodsRecord}
import navigation.{FakeNavigation, Navigation}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import pages._
import pages.goodsRecord.{CommodityCodeUpdatePage, HasCommodityCodeChangePage, HasCountryOfOriginChangePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{CommodityQuery, CommodityUpdateQuery, LongerCommodityQuery}
import repositories.SessionRepository
import services.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.HasCorrectGoodsView

import java.time.Instant
import scala.concurrent.Future

class HasCorrectGoodsControllerSpec extends SpecBase with MockitoSugar {

  private def onwardRoute = Call("GET", "/foo")

  val formProvider = new HasCorrectGoodsFormProvider()
  private val form = formProvider()

  private lazy val journeyRecoveryContinueUrl =
    controllers.goodsRecord.routes.SingleRecordController.onPageLoad(testRecordId).url

  private val warningPage: HasCountryOfOriginChangePage = HasCountryOfOriginChangePage(testRecordId)

  private val record = goodsRecordResponse(
    Instant.parse("2022-11-18T23:20:19Z"),
    Instant.parse("2022-11-18T23:20:19Z")
  ).copy(recordId = testRecordId, eori = testEori)

  private val expectedPayload =
    UpdateGoodsRecord(testEori, testRecordId, commodityCode = Some(testCommodity))

  "HasCorrectGoodsController" - {

    "For create journey" - {
      lazy val hasCorrectGoodsCreateRoute =
        routes.HasCorrectGoodsController.onPageLoadCreate(NormalMode).url
      lazy val onSubmitAction: Call       =
        routes.HasCorrectGoodsController.onSubmitCreate(NormalMode)
      val page: QuestionPage[Boolean]     = HasCorrectGoodsPage

      "must return OK and the correct view for a GET" in {

        val userAnswers =
          emptyUserAnswers
            .set(CommodityQuery, Commodity("654321", List("Description", "Other"), Instant.now, None))
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsCreateRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HasCorrectGoodsView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            Commodity("654321", List("Description", "Other"), Instant.now, None),
            onSubmitAction,
            NormalMode,
            None
          )(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect on GET to JourneyRecovery Page if user doesn't have commodity answer" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsCreateRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val commodity   = Commodity("654321", List("Description"), Instant.now, None)
        val userAnswers = emptyUserAnswers
          .set(CommodityQuery, commodity)
          .success
          .value
          .set(page, true)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsCreateRoute)

          val view = application.injector.instanceOf[HasCorrectGoodsView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), commodity, onSubmitAction, NormalMode, None)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page when valid data is submitted" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsCreateRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must redirect on POST to JourneyRecovery Page if user doesn't have commodity answer" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsCreateRoute)
              .withFormUrlEncodedBody(("value", ""))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val commodity = Commodity("654321", List("Description"), Instant.now, None)

        val userAnswers =
          emptyUserAnswers.set(CommodityQuery, commodity).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsCreateRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[HasCorrectGoodsView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, commodity, onSubmitAction, NormalMode, None)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsCreateRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsCreateRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "for the Longer Commodity Code Journey" - {

      lazy val hasCorrectGoodsRoute =
        routes.HasCorrectGoodsController
          .onPageLoadLongerCommodityCode(NormalMode, testRecordId)
          .url
      lazy val onSubmitAction: Call =
        routes.HasCorrectGoodsController.onSubmitLongerCommodityCode(NormalMode, testRecordId)

      "for a GET" - {

        "must return OK and the correct view" in {

          val userAnswers =
            emptyUserAnswers
              .set(
                LongerCommodityQuery(testRecordId),
                Commodity("654321", List("Description", "Other"), Instant.now, None)
              )
              .success
              .value

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .build()

          running(application) {
            val request = FakeRequest(GET, hasCorrectGoodsRoute)

            val result = route(application, request).value

            val view = application.injector.instanceOf[HasCorrectGoodsView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(
              form,
              Commodity("654321", List("Description", "Other"), Instant.now, None),
              onSubmitAction,
              NormalMode,
              None
            )(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to JourneyRecovery Page if user doesn't have commodity answer" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, hasCorrectGoodsRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }

        "must populate the view correctly when the question has previously been answered" in {

          val commodity   = Commodity("654321", List("Description"), Instant.now, None)
          val userAnswers = emptyUserAnswers
            .set(LongerCommodityQuery(testRecordId), commodity)
            .success
            .value
            .set(HasCorrectGoodsLongerCommodityCodePage(testRecordId), true)
            .success
            .value

          val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

          running(application) {
            val request = FakeRequest(GET, hasCorrectGoodsRoute)

            val view = application.injector.instanceOf[HasCorrectGoodsView]

            val result = route(application, request).value

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(form.fill(true), commodity, onSubmitAction, NormalMode, None)(
              request,
              messages(application)
            ).toString
          }
        }

        "must redirect to Journey Recovery if no existing data is found" in {

          val application = applicationBuilder(userAnswers = None).build()

          running(application) {
            val request = FakeRequest(GET, hasCorrectGoodsRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }

      }

      "for a POST" - {
        "must redirect to the next page when valid data is submitted" in {

          val userAnswers =
            emptyUserAnswers
              .set(
                LongerCommodityQuery(testRecordId),
                Commodity("654321", List("Description", "Other"), Instant.now, None)
              )
              .success
              .value

          val mockSessionRepository = mock[SessionRepository]

          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application =
            applicationBuilder(userAnswers = Some(userAnswers))
              .overrides(
                bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
                bind[SessionRepository].toInstance(mockSessionRepository)
              )
              .build()

          running(application) {
            val request =
              FakeRequest(POST, hasCorrectGoodsRoute)
                .withFormUrlEncodedBody(("value", "true"))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url
          }

        }

        "must redirect to JourneyRecovery Page if user doesn't have commodity answer" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

          running(application) {
            val request =
              FakeRequest(POST, hasCorrectGoodsRoute)
                .withFormUrlEncodedBody(("value", ""))

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController
              .onPageLoad()
              .url
          }
        }

        "must return a Bad Request and errors when invalid data is submitted" in {

          val commodity = Commodity("654321", List("Description"), Instant.now, None)

          val userAnswers =
            emptyUserAnswers.set(LongerCommodityQuery(testRecordId), commodity).success.value

          val mockSessionRepository = mock[SessionRepository]

          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application = applicationBuilder(userAnswers = Some(userAnswers))
            .overrides(
              bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

          running(application) {
            val request =
              FakeRequest(POST, hasCorrectGoodsRoute)
                .withFormUrlEncodedBody(("value", ""))

            val boundForm = form.bind(Map("value" -> ""))

            val view = application.injector.instanceOf[HasCorrectGoodsView]

            val result = route(application, request).value

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual view(boundForm, commodity, onSubmitAction, NormalMode, None)(
              request,
              messages(application)
            ).toString
          }
        }

      }

    }

    "For update journey" - {
      lazy val hasCorrectGoodsUpdateRoute =
        routes.HasCorrectGoodsController.onPageLoadUpdate(NormalMode, testRecordId).url
      lazy val onSubmitAction: Call       =
        routes.HasCorrectGoodsController.onSubmitUpdate(NormalMode, testRecordId)
      val page: QuestionPage[Boolean]     = HasCorrectGoodsCommodityCodeUpdatePage(testRecordId)
      val updatePage                      = CommodityCodeUpdatePage(testRecordId)

      "must return OK and the correct view for a GET" in {

        val userAnswers =
          emptyUserAnswers
            .set(CommodityUpdateQuery(testRecordId), Commodity("654321", List("Description"), Instant.now, None))
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsUpdateRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[HasCorrectGoodsView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(
            form,
            Commodity("654321", List("Description"), Instant.now, None),
            onSubmitAction,
            NormalMode,
            None
          )(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect on GET to JourneyRecovery Page if user doesn't have commodity answer" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsUpdateRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val commodity   = Commodity("654321", List("Description"), Instant.now, None)
        val userAnswers = emptyUserAnswers
          .set(CommodityUpdateQuery(testRecordId), commodity)
          .success
          .value
          .set(page, true)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsUpdateRoute)

          val view = application.injector.instanceOf[HasCorrectGoodsView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), commodity, onSubmitAction, NormalMode, None)(
            request,
            messages(application)
          ).toString
        }
      }

      "onSubmit" - {

        "when true to change code is submitted" - {

          "must redirect to the next page when valid data is submitted" in {

            val userAnswers = emptyUserAnswers
              .set(updatePage, testCommodity.commodityCode)
              .success
              .value
              .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
              .success
              .value
              .set(warningPage, true)
              .success
              .value
              .set(HasCommodityCodeChangePage(testRecordId), true)
              .success
              .value
              .set(CommodityUpdateQuery(testRecordId), testCommodity)
              .success
              .value

            val mockGoodsRecordConnector = mock[GoodsRecordConnector]
            val mockAuditService         = mock[AuditService]
            val mockSessionRepository    = mock[SessionRepository]

            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))
            when(mockGoodsRecordConnector.putGoodsRecord(any(), any())(any())).thenReturn(Future.successful(Done))
            when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
              .successful(record)
            when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                  bind[AuditService].toInstance(mockAuditService)
                )
                .build()

            running(application) {
              val request =
                FakeRequest(POST, hasCorrectGoodsUpdateRoute)
                  .withFormUrlEncodedBody(("value", "true"))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual onwardRoute.url
            }
          }

          "must PUT the goods record, cleanse the data and redirect to the Goods record Page" in {

            val userAnswers = emptyUserAnswers
              .set(updatePage, testCommodity.commodityCode)
              .success
              .value
              .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
              .success
              .value
              .set(warningPage, true)
              .success
              .value
              .set(HasCommodityCodeChangePage(testRecordId), true)
              .success
              .value
              .set(CommodityUpdateQuery(testRecordId), testCommodity)
              .success
              .value

            val newRecord = PutRecordRequest(
              actorId = record.eori,
              traderRef = record.traderRef,
              comcode = testCommodity.commodityCode,
              goodsDescription = record.goodsDescription,
              countryOfOrigin = record.countryOfOrigin,
              category = None,
              assessments = record.assessments,
              supplementaryUnit = record.supplementaryUnit,
              measurementUnit = record.measurementUnit,
              comcodeEffectiveFromDate = record.comcodeEffectiveFromDate,
              comcodeEffectiveToDate = record.comcodeEffectiveToDate
            )

            val mockGoodsRecordConnector = mock[GoodsRecordConnector]
            val mockAuditService         = mock[AuditService]
            val mockSessionRepository    = mock[SessionRepository]

            when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

            when(mockGoodsRecordConnector.putGoodsRecord(any(), any())(any())).thenReturn(Future.successful(Done))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))
            when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
              .successful(record)

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                  bind[AuditService].toInstance(mockAuditService)
                )
                .build()

            running(application) {
              val request = FakeRequest(POST, hasCorrectGoodsUpdateRoute)
                .withFormUrlEncodedBody(("value", "true"))

              val result = route(application, request).value
              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual onwardRoute.url

              verify(mockGoodsRecordConnector).putGoodsRecord(eqTo(newRecord), eqTo(testRecordId))(any())
              verify(mockSessionRepository, times(2)).set(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService)
                  .auditFinishUpdateGoodsRecord(
                    eqTo(testRecordId),
                    eqTo(AffinityGroup.Individual),
                    eqTo(expectedPayload)
                  )(
                    any()
                  )
              }
            }
          }

          "must PATCH the goods record, cleanse the data and redirect to the Goods record Page" in {

            val userAnswers = emptyUserAnswers
              .set(updatePage, testCommodity.commodityCode)
              .success
              .value
              .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
              .success
              .value
              .set(warningPage, true)
              .success
              .value
              .set(HasCommodityCodeChangePage(testRecordId), true)
              .success
              .value
              .set(CommodityUpdateQuery(testRecordId), testCommodity)
              .success
              .value

            val mockGoodsRecordConnector = mock[GoodsRecordConnector]
            val mockAuditService         = mock[AuditService]
            val mockSessionRepository    = mock[SessionRepository]
            val mockFrontendAppConfig    = mock[FrontendAppConfig]

            when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

            when(mockGoodsRecordConnector.updateGoodsRecord(any())(any())).thenReturn(Future.successful(Done))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))
            when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
              .successful(record)

            when(mockFrontendAppConfig.useEisPatchMethod) thenReturn false

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                  bind[AuditService].toInstance(mockAuditService),
                  bind[FrontendAppConfig].toInstance(mockFrontendAppConfig)
                )
                .build()

            running(application) {
              val request = FakeRequest(POST, hasCorrectGoodsUpdateRoute)
                .withFormUrlEncodedBody(("value", "true"))

              val result = route(application, request).value
              status(result) mustEqual SEE_OTHER

              redirectLocation(result).value mustEqual onwardRoute.url

              verify(mockGoodsRecordConnector).updateGoodsRecord(any())(any())
              verify(mockSessionRepository, times(2)).set(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService)
                  .auditFinishUpdateGoodsRecord(
                    eqTo(testRecordId),
                    eqTo(AffinityGroup.Individual),
                    eqTo(expectedPayload)
                  )(
                    any()
                  )
              }
            }
          }

          "when commodity code has not been changed must not update the goods record and redirect to the Home Page" in {

            val answer          = Commodity(record.comcode, List("test"), validityStartDate, None)
            val expectedPayload = UpdateGoodsRecord(testEori, testRecordId, commodityCode = Some(answer))

            val userAnswers = emptyUserAnswers
              .set(updatePage, answer.commodityCode)
              .success
              .value
              .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
              .success
              .value
              .set(warningPage, true)
              .success
              .value
              .set(HasCommodityCodeChangePage(testRecordId), true)
              .success
              .value
              .set(CommodityUpdateQuery(testRecordId), answer)
              .success
              .value

            val mockConnector         = mock[GoodsRecordConnector]
            val mockAuditService      = mock[AuditService]
            val mockSessionRepository = mock[SessionRepository]

            when(mockConnector.getRecord(any(), any())(any())).thenReturn(Future.successful(record))
            when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
              .thenReturn(Future.successful(Done))
            when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
                  bind[SessionRepository].toInstance(mockSessionRepository),
                  bind[GoodsRecordConnector].toInstance(mockConnector),
                  bind[AuditService].toInstance(mockAuditService)
                )
                .build()

            running(application) {
              val request = FakeRequest(POST, hasCorrectGoodsUpdateRoute)
                .withFormUrlEncodedBody(("value", "true"))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual onwardRoute.url

              verify(mockConnector, never()).patchGoodsRecord(any())(any())
              verify(mockConnector).getRecord(eqTo(testEori), eqTo(testRecordId))(any())

              withClue("must call the audit connector with the supplied details") {
                verify(mockAuditService, atLeastOnce())
                  .auditFinishUpdateGoodsRecord(
                    eqTo(testRecordId),
                    eqTo(AffinityGroup.Individual),
                    eqTo(expectedPayload)
                  )(
                    any()
                  )
              }
            }
          }

          "when user answers cannot create an update goods record" - {
            "must not submit anything, and redirect to Journey Recovery" in {

              val mockGoodsRecordConnector = mock[GoodsRecordConnector]
              val mockAuditService         = mock[AuditService]
              val mockSessionRepository    = mock[SessionRepository]

              when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
                .successful(record)
              when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

              val application =
                applicationBuilder(userAnswers = Some(emptyUserAnswers))
                  .overrides(
                    bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                    bind[SessionRepository].toInstance(mockSessionRepository),
                    bind[AuditService].toInstance(mockAuditService)
                  )
                  .build()

              running(application) {
                val request = FakeRequest(POST, hasCorrectGoodsUpdateRoute)
                  .withFormUrlEncodedBody(("value", "true"))

                val result = route(application, request).value

                status(result) mustEqual SEE_OTHER
                redirectLocation(result).value mustEqual
                  controllers.problem.routes.JourneyRecoveryController
                    .onPageLoad(Some(RedirectUrl(journeyRecoveryContinueUrl)))
                    .url

                verify(mockGoodsRecordConnector, atLeastOnce()).getRecord(any(), any())(any())
              }
            }

            "must not submit anything when record is not found, and must let the play error handler deal with connector failure" in {

              val mockAuditService         = mock[AuditService]
              val mockSessionRepository    = mock[SessionRepository]
              val mockGoodsRecordConnector = mock[GoodsRecordConnector]

              when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
              when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
                .failed(new RuntimeException("Something went very wrong"))

              val application =
                applicationBuilder(userAnswers = Some(emptyUserAnswers))
                  .overrides(
                    bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                    bind[SessionRepository].toInstance(mockSessionRepository),
                    bind[AuditService].toInstance(mockAuditService)
                  )
                  .build()

              running(application) {
                val request = FakeRequest(POST, hasCorrectGoodsUpdateRoute)
                  .withFormUrlEncodedBody(("value", "true"))
                intercept[RuntimeException] {
                  await(route(application, request).value)
                  verify(mockGoodsRecordConnector, atLeastOnce()).getRecord(any(), any())(any())
                }
              }
            }

            "must let the play error handler deal with connector failure when updating" in {
              val userAnswers = emptyUserAnswers
                .set(updatePage, testCommodity.commodityCode)
                .success
                .value
                .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
                .success
                .value
                .set(warningPage, true)
                .success
                .value
                .set(HasCommodityCodeChangePage(testRecordId), true)
                .success
                .value
                .set(CommodityUpdateQuery(testRecordId), testCommodity)
                .success
                .value

              val mockGoodsRecordConnector = mock[GoodsRecordConnector]
              val mockAuditService         = mock[AuditService]
              val mockSessionRepository    = mock[SessionRepository]

              when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))
              when(mockAuditService.auditFinishUpdateGoodsRecord(any(), any(), any())(any))
                .thenReturn(Future.successful(Done))
              when(mockGoodsRecordConnector.patchGoodsRecord(any())(any()))
                .thenReturn(Future.failed(new RuntimeException("Connector failed")))
              when(mockGoodsRecordConnector.getRecord(any(), any())(any())) thenReturn Future
                .successful(record)

              val application =
                applicationBuilder(userAnswers = Some(userAnswers))
                  .overrides(
                    bind[GoodsRecordConnector].toInstance(mockGoodsRecordConnector),
                    bind[AuditService].toInstance(mockAuditService),
                    bind[SessionRepository].toInstance(mockSessionRepository)
                  )
                  .build()

              running(application) {
                val request = FakeRequest(POST, hasCorrectGoodsUpdateRoute)
                  .withFormUrlEncodedBody(("value", "true"))

                intercept[RuntimeException] {
                  await(route(application, request).value)
                }

                withClue("must call the audit connector with the supplied details") {
                  verify(mockAuditService, atLeastOnce())
                    .auditFinishUpdateGoodsRecord(
                      eqTo(testRecordId),
                      eqTo(AffinityGroup.Individual),
                      eqTo(expectedPayload)
                    )(
                      any()
                    )
                  verify(mockGoodsRecordConnector, atLeastOnce()).getRecord(any(), any())(any())
                  verify(mockGoodsRecordConnector, atLeastOnce()).putGoodsRecord(any(), any())(any())
                }
              }
            }

          }
        }

        "when false to change code is submitted" - {
          "must redirect to correct page" in {

            val userAnswers = emptyUserAnswers
              .set(updatePage, testCommodity.commodityCode)
              .success
              .value
              .set(HasCorrectGoodsCommodityCodeUpdatePage(testRecordId), true)
              .success
              .value
              .set(warningPage, true)
              .success
              .value
              .set(HasCommodityCodeChangePage(testRecordId), false)
              .success
              .value
              .set(CommodityUpdateQuery(testRecordId), testCommodity)
              .success
              .value

            val mockSessionRepository = mock[SessionRepository]

            when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

            val application =
              applicationBuilder(userAnswers = Some(userAnswers))
                .overrides(
                  bind[Navigation].toInstance(new FakeNavigation(onwardRoute)),
                  bind[SessionRepository].toInstance(mockSessionRepository)
                )
                .build()

            running(application) {
              val request =
                FakeRequest(POST, hasCorrectGoodsUpdateRoute)
                  .withFormUrlEncodedBody(("value", "false"))

              val result = route(application, request).value

              status(result) mustEqual SEE_OTHER
              redirectLocation(result).value mustEqual onwardRoute.url
            }
          }
        }
      }

      "must redirect on POST to JourneyRecovery Page if user doesn't have commodity answer" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsUpdateRoute)
              .withFormUrlEncodedBody(("value", ""))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val commodity = Commodity("654321", List("Description"), Instant.now, None)

        val userAnswers =
          emptyUserAnswers.set(CommodityUpdateQuery(testRecordId), commodity).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsUpdateRoute)
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[HasCorrectGoodsView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, commodity, onSubmitAction, NormalMode, None)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, hasCorrectGoodsUpdateRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, hasCorrectGoodsUpdateRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.problem.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

    }
  }

}
