package com.meituan.firefly.node

case class Field(id: Option[Int], requiredness: Option[Requiredness], fieldType: Type, identifier: SimpleId, value: Option[ConstValue], comment: Option[String])

sealed abstract class Requiredness

object Requiredness {

  case object Required extends Requiredness

  case object Optional extends Requiredness

}
