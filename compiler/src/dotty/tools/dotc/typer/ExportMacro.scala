package dotty.tools.dotc.typer

import dotty.tools.dotc.ast.tpd

import scala.quoted.Quotes


// Plays the same role as scala.quoted.runtime.Expr does, but for export macros
object ExportMacro:

  /** A export macro splice is desugared by the compiler into a call to this method
    *
    *  Calling this method in source has undefined behavior at compile-time
    */
  def spliceDefns(x: Quotes ?=> scala.quoted.Expr[_]): List[tpd.MemberDef] = ???
