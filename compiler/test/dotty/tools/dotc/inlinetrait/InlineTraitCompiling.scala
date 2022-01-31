package dotty.tools.dotc.inlinetrait

import dotty.tools.DottyTest
import dotty.tools.dotc.ast.{Trees,tpd}
import dotty.tools.dotc.core.Names._
import dotty.tools.dotc.core.Symbols._
import dotty.tools.dotc.core.Decorators._
import dotty.tools.dotc.core.Contexts.{Context, ctx}

import org.junit.Assert.assertEquals
import org.junit.Test

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

      // Probably need to inject a marker trait in there somehow
      trait IfTrueThenFooElseBar {

      }

      object IfTrueThenFooElseBar {
        // Would return mods to the class body in `template`
        // If the whole template changes, then that could be its own "unsafe" mod. That means
        // there is no reason to just have everything return Expr[Any] all the time.
        def evolved(b: Expr[Boolean])(template: Expr[Any])(using Quotes): TemplateMod = {
          if (b.valueOrError)
            Modification.mixin('{def foo: Int = 1})
          else
            Modification.mixin('{def bar: String = "1"})
        }
      }

        """, // grows becomes develops acquires evolves
      s"""
         | class ExpectMethodFoo __evolves IfTrueThenFooElseBar(true) {
         |    def maybeBaz = Option("fizz")
         | }
         |""".stripMargin
    )

    println("Inline Trait Test")

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