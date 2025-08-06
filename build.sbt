import play.sbt.routes.RoutesKeys
import sbt.Def
import scoverage.ScoverageKeys
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val appName = "trader-goods-profiles-frontend"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.6"

// Define base scalac options once
val baseScalacOptions = Seq(
  "-encoding", "UTF-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-release", "11",
  "-Wconf:src=routes/.*:s",
  "-Wconf:src=html/.*:s"
).distinct

// Set global scalacOptions once with distinct to avoid duplicates
ThisBuild / scalacOptions := baseScalacOptions

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    inConfig(Compile)(Seq(
      scalacOptions := (scalacOptions.value ++ baseScalacOptions).distinct
    )),
    inConfig(Test)(Seq(
      scalacOptions := (scalacOptions.value ++ baseScalacOptions).distinct
    )),
    inConfig(Test)(testSettings),

    ThisBuild / useSuperShell := false,
    name := appName,

    RoutesKeys.routesImport ++= Seq(
      "models._",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
    ),

    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.config._",
      "views.ViewUtils._",
      "models.Mode",
      "controllers.routes._",
      "viewmodels.govuk.all._"
    ),

    PlayKeys.playDefaultPort := 10905,

    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*handlers.*;.*components.*;.*Routes.*",
    ScoverageKeys.coverageExcludedPackages := "viewmodels.*,views.*",
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,

    libraryDependencies ++= AppDependencies(),

    retrieveManaged := true,

    Concat.groups := Seq(
      "javascripts/application.js" ->
        group(Seq("javascripts/app.js"))
    ),

    pipelineStages := Seq(digest),

    Assets / pipelineStages := Seq(concat)
  )

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork := true,
  unmanagedSourceDirectories += baseDirectory.value / "test-utils"
)

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(
    inConfig(Compile)(Seq(
      scalacOptions := (scalacOptions.value ++ baseScalacOptions).distinct
    )),
    inConfig(Test)(Seq(
      scalacOptions := (scalacOptions.value ++ baseScalacOptions).distinct
    ))
  )

addCommandAlias("testAndCoverage", ";clean;coverage;test;it/test;coverageReport")
addCommandAlias("prePR", ";scalafmt;test:scalafmt;testAndCoverage")
addCommandAlias("preMerge", ";scalafmtCheckAll;testAndCoverage")
