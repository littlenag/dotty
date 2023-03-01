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

  //@Test
  def expandMacro: Unit = {
    println("Expand Macro Test")

    List(
      s"""
         | import scala.quoted._
         | object ExpandMacro {
         |   def somemacroimpl(b: Expr[Boolean])(using Quotes): Expr[String] = {
         |     if (b.valueOrError)
         |       '{"true"}
         |     else
         |       '{"false"}
         |   }
         | }
        """.stripMargin,
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
  def exportMacroSimple: Unit = {
    // List[quotes.reflect.Definition]
    // scala needs someway to quote STATEMENTS
    // handles expressions and types, but not the class building DSL
    val sources = List(
      """
        | import scala.quoted._
        |
        | object TestMacro {
        |   def dothis(b: Boolean)(using Quotes): List[quotes.reflect.Definition] = {
        |     import quotes.reflect.*
        |     if (b) {
        |        // def withFizzle = 12
        |        val helloSymbol = Symbol.newVal(Symbol.spliceOwner, Symbol.freshName("hello"), TypeRepr.of[String], Flags.EmptyFlags, Symbol.noSymbol)
        |        val helloVal = ValDef(helloSymbol, Some(Literal(StringConstant("Hello, World!"))))
        |        List(helloVal)
        |      } else {
        |        // def withSwizzle = "swizzle"
        |        val holaSymbol = Symbol.newVal(Symbol.spliceOwner, Symbol.freshName("hola"), TypeRepr.of[String], Flags.EmptyFlags, Symbol.noSymbol)
        |        val holaVal = ValDef(holaSymbol, Some(Literal(StringConstant("Hola, World!"))))
        |        List(holaVal)
        |      }
        |   }
        | }
      """.stripMargin,

      // syntax for a "bundle of statements"

      // Syntax for generated objects
      // inline object Foo = ${TestMacro.dothis(true)}

      // issues
      // check splice outside inline
      //  - needs to be ok inside either import or export

      """
        | class Foo {
        |   export ${TestMacro.dothis(true)}._
        | }
      """.stripMargin
    )

    println("Export Macro Test")

    checkCompile("typer", sources) { (tree, context) =>
      given Context = context

      import dotty.tools.dotc.printing.*

      val printer = new RefinedPrinter(context)

      println("COMPILED TREE:")
      println(tree.toText(printer).show)
      //val bar = tree.find(tree => tree.symbol.name == termName("bar")).get
      //assertEquals("trait Too", bar.symbol.owner.show)
    }
  }
}

