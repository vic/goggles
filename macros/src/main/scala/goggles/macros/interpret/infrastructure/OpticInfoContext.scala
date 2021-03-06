package goggles.macros.interpret.infrastructure

import goggles.macros.interpret._
import goggles.macros.errors.{UserError, InternalError}

trait OpticInfoContext {
    this: Contextual => 


  def getArgLabel(tree: c.Tree): String = {
    val pos = tree.pos
    val src = new String(pos.source.content)
    val label = src.substring(pos.start, pos.end)

    if (label.forall(_.isUnicodeIdentifierPart)) s"$$$label"
    else s"$${$label}"
  }

  def getLastOpticInfo(name: String): Interpret[OpticInfo[c.Type]] = {
    Parse.getLastOpticInfo[c.Type, c.Expr[Any]].flatMap {
      case Some(info) => Parse.pure(info)
      case None => Parse.raiseError(InternalError.OpticInfoNotFound(name))
    }
  }
  
  def storeOpticInfo(label: String, sourceType: c.Type, targetType: c.Type, opticType: OpticType): Interpret[Unit] = {
    for {
      lastInfo <- Parse.getLastOpticInfo[c.Type, c.Expr[Any]]
      nextOpticType = lastInfo match {
        case Some(info) => info.compositeOpticType.compose(opticType)
        case None => Some(opticType)
      }
      fromOptic = lastInfo.fold(opticType)(_.compositeOpticType)
      composed <- Parse.fromOption(nextOpticType,
                                    UserError.WrongKindOfOptic(label, sourceType, targetType, fromOptic, opticType))
      _ <- Parse.storeOpticInfo(OpticInfo(label, sourceType.resultType, targetType.resultType, opticType, composed))
    } yield ()
  }

  def getLastTargetType(name: String): Interpret[c.Type] =
    getLastOpticInfo(name).map(_.targetType)
}