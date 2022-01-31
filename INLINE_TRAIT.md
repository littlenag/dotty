# Inline Trait

expanding the capabilities of scala meta programming

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
     *  i think the trait macro needs to return something other than Expr[T]
     *
     *  how/when is the macro "code" run?
     *
     *  for now - restrict the grammar to adding new symbols (methods,vals,types)
     *
     *  a modification should act a bit like a state monad, when asking for
     *  current context the entity should look like it has had all transforms
     *  applied (event sourced model?)
     *    - that would mean modifications are TemplateBody => TemplateBody in general
     *    but limited to (TemplateBody, CurrentState) => ModRule for debugging context
     *    - should be able to see other mod rules?
     *
     */


s"""
|      // two methods
|      //   1) a special method in an Object, similar to derived
|      //     plus a special keyword to denote transformation by said method
|      //   2) inline trait + def[this] for purely simple additive changes to templates
|      //     still not quite clear how this would work, Expr as constructor body? special mixin method?
|
|      // how does this get integrated into the AST?
|      // should look like function application?
|      // deriving uses a specially named method call `derived` in the companion
|
|      // limited to what can be done with inline params and the self type
|      inline trait IfTrueThenFooElseBar(inline b: Boolean) { self =>
|         // this syntax is all about expressions that compute values
|         inline if (b.valueOrError)
|           export def foo: Int = 1
|         else
|           export def bar: String = "1"
|
|         // feels like export can already do this if
|         // you just define all sets that you want and then export just want you need
|
|         // maybe use the syntax of types? like match types?
|         // or maybe the part following def ___ becomes fair game?
|
|         def $${...}
|
|         // or maybe a special mixin function?
|      }
|
|      // could the export keyword be used?
|
|      // combine internal object with export clause?
|
|""".stripMargin

//
//  import scala.quoted._
//
//  object ExpandMacro {
//    def somemacroimpl(b: Expr[Boolean])(using Quotes): Expr[String] = {
//      summon[Quotes].appliedTo()
//      if (b.valueOrError)
//        '{"true"}
//      else
//        '{"false"}
//    }
//
//
//  }


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