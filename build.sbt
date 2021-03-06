import scalapb.compiler.Version._
import sbtrelease.ReleaseStateTransformations._
import sbtcrossproject.CrossPlugin.autoImport.crossProject

val Scala212 = "2.12.11"
val Scala213 = "2.13.1"
val scalatestVersion = "3.1.1"

val scalapbV = settingKey[String]("")

val tagName = Def.setting {
  s"v${if (releaseUseGlobalVersion.value) (version in ThisBuild).value else version.value}"
}

val tagOrHash = Def.setting {
  if (isSnapshot.value) sys.process.Process("git rev-parse HEAD").lineStream_!.head
  else tagName.value
}

val unusedWarnings = Seq("-Ywarn-unused")

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .in(file("core"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    commonSettings,
    name := UpdateReadme.scalapbJsonCommonName,
    description := "Json/Protobuf convertors for ScalaPB",
    mappings in (Compile, packageSrc) ++= (managedSources in Compile).value.map { f =>
      // https://github.com/sbt/sbt-buildinfo/blob/v0.7.0/src/main/scala/sbtbuildinfo/BuildInfoPlugin.scala#L58
      val buildInfoDir = "sbt-buildinfo"
      val path = if (f.getAbsolutePath.contains(buildInfoDir)) {
        (file(buildInfoPackage.value) / f
          .relativeTo((sourceManaged in Compile).value / buildInfoDir)
          .get
          .getPath).getPath
      } else {
        f.relativeTo((sourceManaged in Compile).value).get.getPath
      }
      (f, path)
    },
    buildInfoPackage := "scalapb_json",
    buildInfoObject := "ScalapbJsonCommonBuildInfo",
    buildInfoKeys := Seq[BuildInfoKey](
      "scalapbVersion" -> scalapbV,
      scalaVersion,
      version
    )
  )
  .jvmSettings(
    PB.targets in Test := Seq(
      PB.gens.java -> (sourceManaged in Test).value,
      scalapb.gen(javaConversions = true) -> (sourceManaged in Test).value
    ),
    libraryDependencies ++= Seq(
      "com.google.protobuf" % "protobuf-java-util" % protobufVersion % "test"
    )
  )
  .jsSettings(
    scalacOptions += {
      val a = (baseDirectory in LocalRootProject).value.toURI.toString
      val g =
        "https://raw.githubusercontent.com/scalapb-json/scalapb-json-common/" + tagOrHash.value
      s"-P:scalajs:mapSourceURI:$a->$g/"
    },
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.0.0-RC5",
    ),
  )
  .settings(
    scalapropsCoreSettings,
    libraryDependencies += "com.github.scalaprops" %%% "scalaprops" % "0.6.3" % "test",
    libraryDependencies += "org.scalatest" %%% "scalatest" % scalatestVersion % "test",
  )
  .platformsSettings(JSPlatform)(
    PB.targets in Test := Seq(
      scalapb.gen(javaConversions = false) -> (sourceManaged in Test).value
    )
  )

lazy val macros = project.settings(
  commonSettings,
  description := "Json/Protobuf convertor macros for ScalaPB",
  name := UpdateReadme.scalapbJsonMacrosName,
  libraryDependencies ++= Seq(
    "org.scalatest" %%% "scalatest" % scalatestVersion % "test",
    scalaOrganization.value % "scala-reflect" % scalaVersion.value,
  ),
)

lazy val macrosJava = project
  .settings(
    commonSettings,
    name := UpdateReadme.scalapbJsonMacrosJavaName,
    description := "Json/Protobuf convertor macros for ScalaPB with protobuf-java-util dependency",
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % scalatestVersion % "test",
      "com.google.protobuf" % "protobuf-java-util" % protobufVersion,
    )
  )
  .dependsOn(
    macros,
    coreJVM % "test->test",
  )

lazy val tests = crossProject(JVMPlatform, JSPlatform)
  .settings(
    commonSettings,
    noPublish,
  )
  .configure(_ dependsOn (macros, macrosJava))
  .platformsSettings(JVMPlatform, JSPlatform)(
    libraryDependencies += "org.scalatest" %%% "scalatest" % scalatestVersion % "test",
  )

lazy val testsJVM = tests.jvm
lazy val testsJS = tests.js

commonSettings

lazy val noPublish = Seq(
  PgpKeys.publishLocalSigned := {},
  PgpKeys.publishSigned := {},
  publishLocal := {},
  publish := {},
  publishArtifact in Compile := false
)

noPublish

lazy val commonSettings = Def.settings(
  scalapbV := scalapbVersion,
  unmanagedResources in Compile += (baseDirectory in LocalRootProject).value / "LICENSE.txt",
  scalaVersion := Scala212,
  crossScalaVersions := Seq(Scala212, Scala213),
  scalacOptions ++= unusedWarnings,
  Seq(Compile, Test).flatMap(c => scalacOptions in (c, console) --= unusedWarnings),
  scalacOptions ++= Seq("-feature", "-deprecation", "-language:existentials"),
  licenses += ("MIT", url("https://opensource.org/licenses/MIT")),
  organization := "io.github.scalapb-json",
  Project.inConfig(Test)(sbtprotoc.ProtocPlugin.protobufConfigSettings),
  PB.targets in Compile := Nil,
  // Can't use -v380
  // https://github.com/scalapb/ScalaPB/commit/ff99b075625fe684ce2eef7686d587fdbbf19b62
  // https://github.com/scalapb/ScalaPB/commit/d3cc69515ea90f1af7eaf2732d22facb6c9e95e3
  PB.protocVersion := "-v371",
  PB.protoSources in Test := Seq(baseDirectory.value.getParentFile / "shared/src/test/protobuf"),
  libraryDependencies ++= Seq(
    "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapbV.value,
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapbV.value % "protobuf,test",
    "com.lihaoyi" %%% "utest" % "0.7.4" % "test"
  ),
  testFrameworks += new TestFramework("utest.runner.Framework"),
  pomExtra in Global := {
    <url>https://github.com/scalapb-json/scalapb-json-common</url>
      <scm>
        <connection>scm:git:github.com/scalapb-json/scalapb-json-common.git</connection>
        <developerConnection>scm:git:git@github.com:scalapb-json/scalapb-json-common.git</developerConnection>
        <url>github.com/scalapb-json/scalapb-json-common.git</url>
        <tag>{tagOrHash.value}</tag>
      </scm>
      <developers>
        <developer>
          <id>xuwei-k</id>
          <name>Kenji Yoshida</name>
          <url>https://github.com/xuwei-k</url>
        </developer>
      </developers>
  },
  publishTo := sonatypePublishToBundle.value,
  scalacOptions in (Compile, doc) ++= {
    val t = tagOrHash.value
    Seq(
      "-sourcepath",
      (baseDirectory in LocalRootProject).value.getAbsolutePath,
      "-doc-source-url",
      s"https://github.com/scalapb-json/scalapb-json-common/tree/${t}€{FILE_PATH}.scala"
    )
  },
  ReleasePlugin.extraReleaseCommands,
  commands += Command.command("updateReadme")(UpdateReadme.updateReadmeTask),
  releaseTagName := tagName.value,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    UpdateReadme.updateReadmeProcess,
    tagRelease,
    ReleaseStep(
      action = { state =>
        val extracted = Project extract state
        extracted
          .runAggregated(PgpKeys.publishSigned in Global in extracted.get(thisProjectRef), state)
      },
      enableCrossBuild = true
    ),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    UpdateReadme.updateReadmeProcess,
    pushChanges
  )
)

val coreJVM = core.jvm
val coreJS = core.js

commonSettings
publishArtifact := false
publish := {}
publishLocal := {}
PgpKeys.publishSigned := {}
PgpKeys.publishLocalSigned := {}
