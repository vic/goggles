package goggles.macros.interpret.dsl

import goggles.macros.interpret._
import goggles.macros.interpret.features._
import goggles.macros.interpret.infrastructure._
import goggles.macros.lex._
import goggles.macros.parse._
import goggles.macros.errors._

trait LensModeDslImpl extends LensModeDsl
  with Contextual 
  with DslModeContext
  with StringContextInterpreter
  with LensExprInterpreter
  with InterpreterActions
  with EachFeature
  with IndexFeature
  with InterpolatedLensRefFeature
  with NamedLensRefFeature
  with PossibleFeature


class LensModeDsl  {
  this: Contextual with StringContextInterpreter 
                   with DslModeContext 
                   with LensExprInterpreter => 

  import AST._
                 
  override def mode: DslMode = DslMode.Lens

  def lens(args: c.Expr[Any]*): MacroResult[c.Type, c.Tree] = {

    type Interpret[A] = Parse[c.Type, c.Expr[Any], A]

    val errorOrAst: Either[GogglesError[c.Type], ComposedLensExpr] =
      Parser.parseUnappliedLens(Lexer(contextStringParts))

    val finalTree: Interpret[c.Tree] =
      for {
        ast <- Parse.fromEither(errorOrAst)
        tree <- interpretComposedLensExpr(ast)
      } yield tree

      val (errorOrTree, macroState) = finalTree.eval(args.toList)
      MacroResult(errorOrTree, macroState.infos, Nil, SourcePosition.getErrorOffset(mode, macroState))
  }

}