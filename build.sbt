val projectName = IO.readLines(new File("PROJECT_NAME")).head
val v           = IO.readLines(new File("VERSION")).head
val sparkVersions: List[String] = IO.readLines(new File("sparkVersions")).map(_.trim)

val scala11 = "2.11.12"
val scala12 = "2.12.16"

val Spark233 = "2.3.3"
val Spark245 = "2.4.5"
val Spark312 = "3.1.2"

val sparkVersionSystem = System.getProperty("sparkVersion", sparkVersions.head)
val sparkVersion       = settingKey[String]("Spark version")

lazy val rootSettings =
  Seq(
    organization       := "com.leobenkel",
    homepage           := Some(url("https://github.com/leobenkel/ZparkIO")),
    licenses           := List("MIT" -> url("https://opensource.org/licenses/MIT")),
    developers         :=
      List(
        Developer(
          "leobenkel",
          "Leo Benkel",
          "",
          url("https://leobenkel.com")
        )
      ),
    sparkVersion       := sparkVersionSystem,
    crossScalaVersions := {
      sparkVersion.value match {
        case Spark233 => Seq(scala11)
        case Spark245 => Seq(scala11, scala12)
        case Spark312 => Seq(scala12)
      }
    },
    scalaVersion       := crossScalaVersions.value.head,
    resolvers += Resolver.sonatypeRepo("releases"),
    soteriaAddSemantic := false,
    version ~= (v => s"${sparkVersionSystem}_$v"),
    dynver ~= (v => s"${sparkVersionSystem}_$v")
  )

lazy val zioVersion = "2.0.2"

lazy val commonSettings =
  rootSettings ++
    Seq(
      libraryDependencies ++=
        Seq(
          // https://zio.dev/docs/getting_started.html
          "dev.zio"          %% "zio"        % zioVersion,
          // https://mvnrepository.com/artifact/org.apache.spark/spark-core
          "org.apache.spark" %% "spark-core" % sparkVersion.value % Provided,
          // https://mvnrepository.com/artifact/org.apache.spark/spark-sql
          "org.apache.spark" %% "spark-sql"  % sparkVersion.value % Provided,
          "org.scalatest"    %% "scalatest"  % "3.2.14"           % Test
        ),
      updateOptions          := updateOptions.value.withGigahorse(false),
      Test / publishArtifact := false,
      pomIncludeRepository   := (_ => false)
    )

lazy val root = (project in file("."))
  .aggregate(library, testHelper, tests, example1Mini, example2Small)
  .settings(
    name := s"${projectName}_root",
    rootSettings
  )

lazy val library = (project in file("Library")).settings(
  commonSettings,
  name := projectName
)

lazy val sparkTestingBaseVersion =
  sparkVersionSystem match {
    // https://mvnrepository.com/artifact/com.holdenkarau/spark-testing-base
    case Spark312 => "3.1.2_1.1.0"
    case _        => s"${sparkVersionSystem}_0.14.0"
  }

lazy val testHelper = (project in file("testModules/TestHelper"))
  .settings(
    commonSettings,
    name := s"$projectName-test",
    libraryDependencies ++=
      Seq(
        "com.holdenkarau"  %% "spark-testing-base" % sparkTestingBaseVersion,
        "org.apache.spark" %% "spark-hive"         % sparkVersion.value % Provided
      )
  )
  .dependsOn(library)

lazy val tests = (project in file("testModules/Tests"))
  .settings(
    commonSettings,
    name           := s"${projectName}_tests",
    publish / skip := true
  )
  .dependsOn(
    library,
    testHelper % Test
  )

lazy val libraryConfigsScallop = (project in file("configLibs/Scallop"))
  .settings(
    commonSettings,
    name := s"$projectName-config-scallop",
    libraryDependencies ++=
      Seq(
        // https://github.com/scallop/scallop
        "org.rogach" %% "scallop" % "4.1.0"
      )
  )
  .dependsOn(library)

lazy val example1Mini = (project in file("examples/Example1_mini"))
  .settings(
    commonSettings,
    name                      := s"${projectName}_example1_mini",
    publish / skip            := true,
    assembly / assemblyOption := soteriaAssemblySettings.value
  )
  .enablePlugins(DockerPlugin)
  .dependsOn(
    library,
    libraryConfigsScallop,
    testHelper % Test
  )

lazy val example2Small = (project in file("examples/Example2_small"))
  .settings(
    commonSettings,
    name                      := s"${projectName}_example2_small",
    publish / skip            := true,
    assembly / assemblyOption := soteriaAssemblySettings.value
  )
  .enablePlugins(DockerPlugin)
  .dependsOn(
    library,
    libraryConfigsScallop,
    testHelper % Test
  )
