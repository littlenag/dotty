package dotty.tools.dotc.inlinetrait

import dotty.tools.DottyTest
import dotty.tools.dotc.ast.{Trees,tpd}
import dotty.tools.dotc.core.Names._
import dotty.tools.dotc.core.Symbols._
import dotty.tools.dotc.core.Decorators._
import dotty.tools.dotc.core.Contexts.{Context, ctx}

import org.junit.Assert.assertEquals
import org.junit.Test

//object example {
//  import scala.quoted._
//
//  def somemacroimpl(b: Expr[Boolean])(using Quotes): Expr[String] = {
//    if (b.valueOrError)
//      '{"true"}
//    else
//      '{"false"}
//  }
//
//  inline def somemacro(inline b: Boolean): String =
//    ${somemacroimpl('b)}
//}
//
//object uses {
//  import example._
//
//  somemacro(true)
//}


class InlineTraitCompiling extends DottyTest {

  private def newContext = {
    initialCtx.setSetting(ctx.settings.color, "never")
  }
  ctx = newContext

  import tpd._

  @Test
  def inlineTraitSimple: Unit = {
    val source = s"""

      import scala.quoted._

      object some_macros {
        def somemacroimpl(b: Expr[Boolean])(using Quotes): Expr[String] = {
          if (b.valueOrError)
            '{"true"}
          else
            '{"false"}
        }

        inline def somemacro(inline b: Boolean): String = $${somemacroimpl('b)}

        somemacro(true)

      }

      // Should return a TraitBody, but Any will work for today
      def traitBody(b: Expr[Boolean])(using Quotes): Expr[Any] = {
        if (b.valueOrError)
          // inject a method named foo: Int = 1
          // have to quote to capture the AST
          '{def foo: Int = 1}
        else
          // inject a method named bar: String = "1"
          '{def bar: String = "1"}
      }

      inline trait IfTrueThenFooElseBar(inline b: Boolean) { self =>
        // with an inline defs wants this to be a call to a static method returning an Expr[T]
        $${traitBody('b)}
      }

      // want syntax to evaluate a statement in a particular implicit context (override context)

      object ExpectMethodFoo extends IfTrueThenFooElseBar(true)
    """

    checkCompile("typer", source) { (tree, context) =>
      given Context = context

      import dotty.tools.dotc.printing.*

      val printer = new RefinedPrinter(context)

      println(tree.toText(printer).show)
      //val bar = tree.find(tree => tree.symbol.name == termName("bar")).get
      //assertEquals("trait Too", bar.symbol.owner.show)
    }
  }
}

    // inline trait Fizz(inline b: Boolean) { self =>
    //   // want a shortcut for this kind of syntax
    //   inline if (b) {
    //     //erased val sc = summon[StatementContext]
    //     // treat as an erased value
    //     //given StatementContext = this
    //     // shortcut could be what?

    //     // other keywords to leverage?
    //     //export
    //     //extension

    //     // how to lift a statement and turn it into data?

    //     def[this] foo: Int = 1
    //   } else {
    //     def[this] bar: String = "1"
    //   }
    // }