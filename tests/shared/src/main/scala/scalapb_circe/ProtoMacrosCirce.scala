package scalapb_circe

import com.google.protobuf.InvalidProtocolBufferException
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

import scala.reflect.macros.blackbox
import language.experimental.macros
import scala.util.Try

object ProtoMacrosCirce {

  implicit class ProtoContext(private val c: StringContext) extends AnyVal {
    def struct(): com.google.protobuf.struct.Struct =
      macro ProtoMacrosCirce.protoStructInterpolation
    def value(): com.google.protobuf.struct.Value =
      macro ProtoMacrosCirce.protoValueInterpolation
  }

  implicit class FromJson[A <: GeneratedMessage with Message[A]](
    val companion: GeneratedMessageCompanion[A]
  ) extends AnyVal {
    def fromJsonConstant(json: String): A = macro ProtoMacrosCirce.fromJsonConstantImpl0[A]

    def fromJson(json: String): A = macro ProtoMacrosCirce.fromJsonImpl[A]

    def fromJsonDebug(json: String): A = macro ProtoMacrosCirce.fromJsonDebugImpl

    def fromJsonOpt(json: String): Option[A] =
      macro ProtoMacrosCirce.fromJsonOptImpl

    def fromJsonEither(json: String): Either[InvalidProtocolBufferException, A] =
      macro ProtoMacrosCirce.fromJsonEitherImpl

    def fromJsonTry(json: String): Try[A] =
      macro ProtoMacrosCirce.fromJsonTryImpl
  }
}

class ProtoMacrosCirce(override val c: blackbox.Context) extends scalapb_json.ProtoMacrosCommon(c) {

  import c.universe._

  override def fromJsonImpl[A: c.WeakTypeTag](json: c.Tree): c.Tree = {
    val A = weakTypeTag[A]
    q"_root_.scalapb_circe.JsonFormat.fromJsonString[$A]($json)"
  }

  override def fromJsonConstantImpl[A <: GeneratedMessage with Message[A]: c.WeakTypeTag: GeneratedMessageCompanion](
    string: String
  ): c.Tree = {
    val A = weakTypeTag[A]
    scalapb_circe.JsonFormat.fromJsonString[A](string)
    q"_root_.scalapb_circe.JsonFormat.fromJsonString[$A]($string)"
  }

  override protected[this] def protoString2Struct(string: String): c.Tree = {
    val json = io.circe.parser.parse(string).fold(throw _, identity)
    val struct = StructFormat.structParser(json)
    q"$struct"
  }

  override protected[this] def protoString2Value(string: String): c.Tree = {
    val json = io.circe.parser.parse(string).fold(throw _, identity)
    val value = StructFormat.structValueParser(json)
    q"$value"
  }
}
