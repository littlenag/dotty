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
  def expandMacro: Unit = {
    println("Expand Macro Test")

    List(
      s"""

  import scala.quoted._

  object ExpandMacro {
    def somemacroimpl(b: Expr[Boolean])(using Quotes): Expr[String] = {
      if (b.valueOrError)
        '{"true"}
      else
        '{"false"}
    }


  }

        """,
      s"""
         | object foo {
         |
         |   inline def somemacro(inline b: Boolean): String =
         |      $${ExpandMacro.somemacroimpl('b)}
         |   def expandedInlineValue = somemacro(true) + "_foo"
         |
         | }
         |""".stripMargin
    ).map { source =>

      // various phases
      //val phase = "typer"
      //val phase = "inlining"
      val phase = "typer"

      checkCompile(phase, source) { (tree, context) =>
        given Context = context

        import dotty.tools.dotc.printing.*

        val printer = new RefinedPrinter(context)

        println(tree.toString)
        println(tree.toText(printer).show)
        //val bar = tree.find(tree => tree.symbol.name == termName("bar")).get
        //assertEquals("trait Too", bar.symbol.owner.show)
      }
    }


  }

  @Test
  def inlineTraitSimple: Unit = {
    val sources = List(
      s"""

      import scala.quoted._

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
        // as with inline defs we call a static method returning an Expr[T]
        // maybe want this to be a StatementMod?
        $${traitBody('b)}
      }

        """,
      s"""
         | object ExpectMethodFoo extends IfTrueThenFooElseBar(true)
         |""".stripMargin
    )

    println("Inline Trait Test")

    /**
     * expanding an inline trait will have to happen within
     * the typer itself, much like the deriving clause works.
     *
     * trying after the typer will fail type checking
     *
     * pre-typer can't work, because you'll want expansion to happen
     * with values having inferred types
     *
     * interaction with derived? expansion should occur first
     *
     * goals:
     *   - inject one of two methods into a class depending on the type parameter
     *   - some way to take the class body as input during expansion
     *   - some way to append instructions to be evaluation during class expansion
     *   - annotations -> inline traits
     *   - case class A -> repr -> case class B
     *
     *
     * inline needs to work something like a Template => Template lambda
     *
     * can an inline trait
     *   - extend interfaces? probably should
     *   - declare regular defs and vals? probably should
     *   - be matchable? maybe?
     *   - open, final methods, denote methods open for re-writing? private open? new modifier?
     *     - soft keyword?
     *
     *
     *  add/remove/rename/restructure methods as a function of literal arguments and types
     *
     *  what is the execution model?
     *    - are all types fully known before executing the modifiers?
     *
     *  Use @specialized to mark a method that may be transformed by an inline trait
     *  by whose name will name the same
     *  @specialized def foo: Any
     *
     *  Use `erased` to indicate input vals, defs, and types?
     *
     *  input object pattern? declare an erased object named `args` that the
     *  trait can take as input?
     *
     *  do we want templates and macros?
     *
     */

    checkCompile("typer", sources) { (tree, context) =>
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