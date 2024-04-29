package services

import config.FrontendAppConfig
import models.{InternalId, TraderGoodsProfile, UserAnswers}
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import repositories.SessionRepository
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.duration.{DurationInt}
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps

class SessionServiceISpec extends AnyFreeSpec
  with Matchers
  with DefaultPlayMongoRepositorySupport[UserAnswers]
  with ScalaFutures
  with IntegrationPatience
  with OptionValues
  with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global;

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
  private val mockAppConfig = mock[FrontendAppConfig]

  when(mockAppConfig.cacheTtl) thenReturn 1

  protected override val repository = new SessionRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  "Session Service" - {

    val sessionService = new SessionService(repository)

    "createUserAnswers should create answers in the sessionRepository" in {
      val id = InternalId("test_id_1")
      Await.result(sessionService.createUserAnswers(id).value, 3 seconds)
      val answersInRepository = find(Filters.equal("_id", id.value)).futureValue.headOption
      answersInRepository match {
        case Some(createdAnswers: UserAnswers) => {
          createdAnswers.id mustEqual id.value
        }
        case None => {
          fail("No user answers created")
        }
      }
    }

    "readUserAnswers should read answers if present in the sessionRepository" in {
      val id = InternalId("test_id_2")
      val presentAnswers = UserAnswers(id.value, None, Instant.ofEpochSecond(1))
      Await.result(repository.set(presentAnswers), 3 seconds)
      val result = Await.result(sessionService.readUserAnswers(id).value, 3 seconds)
      result match {
        case Left(_) => fail("Session repository should not error")
        case Right(None) => fail("Session service should not fail to get answers present")
        case Right(Some(answers)) => {
          answers.id shouldEqual id.value
        }
      }
    }

    "updateUserAnswers should update answers if present in the sessionRepository" in {
      val id = InternalId("test_id_3")
      val presentAnswers = UserAnswers(id.value, None, Instant.ofEpochSecond(1))
      val answersToUpdate = presentAnswers.copy(traderGoodsProfile = Some(TraderGoodsProfile()))

      Await.result(repository.set(presentAnswers), 3 seconds)

      val result = Await.result(sessionService.updateUserAnswers(answersToUpdate).value, 3 seconds)
      result match {
        case Left(_) => fail("Session repository should not error")
        case Right(_) => {}
      }

      val answersInRepository = find(Filters.equal("_id", id.value)).futureValue.headOption
      answersInRepository match {
        case Some(updatedAnswers: UserAnswers) => {
          updatedAnswers.id mustEqual id.value
          updatedAnswers.traderGoodsProfile mustEqual answersToUpdate.traderGoodsProfile
        }
        case None => {
          fail("No user answers updated")
        }
      }
    }

  }

}