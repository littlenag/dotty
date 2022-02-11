Expanded Metaprogramming
========================

Scala 3's current metaprogramming approach solves many of the problems 

 - macro annotations
 - white box macros
 - no
 
https://scalacenter.github.io/scala-3-migration-guide/docs/macros/macro-libraries.html
https://github.com/lampepfl/dotty/pull/1693
https://github.com/lampepfl/dotty/issues/1694
https://contributors.scala-lang.org/t/annotation-macros/1211/20
https://users.scala-lang.org/t/macro-annotations-replacement-in-scala-3/7374
https://docs.scala-lang.org/overviews/macros/annotations.html
https://www.reddit.com/r/scala/comments/ce9v9l/can_dotty_macro_add_method_to_class/

https://www.scala-lang.org/blog/2018/04/30/in-a-nutshell.html
 - promised macro annotations, but that never materialized


Something less ambiguous than annotations
 - normally annotations are just data
 - and may be picked up by something else
 - Java has changed perspectives a bit in this case

Something less powerful than macro annotations
 - too unconstrained in what they were allowed to do


## Problem

Hygiene and referential transparency by default.

odersky really wants to avoid creating language dialects

implementation options
 - syntactic transformation (preprocessor)
 - AST transformation (macro annotations, __evolves)
 - AST modifications (returned list of mods)
 - exported decls

treat target grammar not as AST, but as higher-level decls the language grammar captures

# Examples and Use Cases

# Scalameta

https://scalameta.org/

how does scalameta fit into this?

why is it not a good solution? 
 - feels equivalent to a C preprocessor
 - purely mechanical syntactic transformations
 - can't express type driven expansion (no live givens/implicits to expand code using)
 - complex build integration, has to generate the code that will eventually be compiled

what can it not solve?

run-time metaprogramming
compile-time metaprogramming

pre-processor level
syntactic-level
semantic-level
run-time-level

links with specialization
links with macro annotations

concrete use-cases
* from a case class auto-generate a builder pattern + validators
* schema evolution from base case class and migration decls
* generate client or server impls from a trait
* override toString or other fields to hide security-related content
* mixin logging/metrics/etc boilerplate


capabilities matrix:
* replace/add/remove
  * shouldn't be able to remove (in general), except for specially marked decls
  * though could be useful for case classes and copy/apply
  * then removal becomes "prevent generation of"
* methods or fields or inner objects or whatever
* add/remove traits, base classes
  * understand use case better... 
* on a single instance or for every instance (class-level)
  * obviously runtime metaprogramming...

=> compatible with type safety
=> how does it work with Selectable?

annotations usually transform entire classes
 * could annotate method generate or transform single method?

`export ${macro}`
 - could act like a mixin for a class or trait body

the main complications
 - how to handle the companion object
 - how to extend class with new traits

part of the goal is for the macro to receive a typed rather than raw AST to transform
avoid the two step process in scala 2 of compiling macros in one module, then can run in another

should be able to override an existing method definition with another one
 - @ToStringObfuscate - override toString for security purposes

# Solution

## why have a new syntatic construct? why not just make macro annotations work?

principle of least surprise

annotations can't take arguments

clear user intention


    object IfTrueThenFooElseBar {
      def evolved(b: Expr[Boolean])(template: Expr[Any])(using Quotes): TemplateMod = {
        if b.valueOrError then
          Modification.mixin('{def foo: Int = 1})
        else
          Modification.mixin('{def bar: String = "1"})
      }
    }

    class HasMethodFoo __evolves IfTrueThenFooElseBar(true)
    class HasMethodBar __evolves IfTrueThenFooElseBar(false)

    class HasMethodBar = IfTrueThenFooElseBar(false, class _ [T](t:T) extends Foo,Bar {
      // ...
    })

anonymous dsl for classes/traits/methods
have to be bound to a name to become "real"

    object IfTrueThenFooElseBar {
      def evolved(b: Boolean)(template: Expr[Any])(using Quotes): TemplateMod = {
        if b.valueOrError then
          Modification.mixin('{def foo: Int = 1})
        else
          Modification.mixin('{def bar: String = "1"})
      }
    }

    class HasMethodFoo __evolves IfTrueThenFooElseBar(true)
    class HasMethodBar __evolves IfTrueThenFooElseBar(false)

////

    import scala.annotation.StaticAnnotation
    import scala.language.experimental.macros
    import scala.reflect.macros.whitebox

    class ToStringObfuscate(fieldsToObfuscate: String*) extends StaticAnnotation {
      def macroTransform(annottees: Any*): Any = macro ToStringObfuscateImpl.impl
    }



////

would f-bounded types make this easier?

    // enforced by the compiler to be actual literal values?
    inline trait ToStringObfuscate(fieldsToObfuscate: String*)(optional implicits for quotes? context? base type?) {
      // export inline / override
      export override ${
        override def toString: String = {
          scala.runtime.ScalaRunTime._toString(this.copy(..$fieldReplacements))
        }
      }
    }

    object ToStringObfuscate {
      // magic method here to transform companion object?
    }

    case class User(name:String, password:String) inline ToStringObfuscate("password")


what kinds of arguments can you pass to an annotation?
 - probably can't pass a Repr too easily

# Discussion

expanding the capabilities of scala meta programming

can add classes to classpath?

dsl to directly register annotations for processing with the compiler?
 - rather than having it pickup indirectly, have code that registers
 - the behavior directly with the compiler?

expanding an inline trait will have to happen within
the typer itself, much like the deriving clause works.

trying after the typer will fail type checking

pre-typer can't work, because you'll want expansion to happen
with values having inferred types

interaction with derived? expansion should occur first

interaction with specialized? 

path to run-time metaprogramming?

goals:
  - inject one of two methods into a class depending on the type parameter
  - some way to take the class body as input during expansion
  - some way to append instructions to be evaluation during class expansion
  - annotations -> inline traits
  - case class A -> repr -> case class B


inline needs to work something like a Template => Template lambda

can an inline trait
  - extend interfaces? probably should
  - declare regular defs and vals? probably should
  - be matchable? maybe?
  - open, final methods, denote methods open for re-writing? private open? new modifier?
    - soft keyword?


 add/remove/rename/restructure methods as a function of literal arguments and types

 what is the execution model?
   - are all types fully known before executing the modifiers?

 Use @specialized to mark a method that may be transformed by an inline trait
 by whose name will name the same
 @specialized def foo: Any

 Use `erased` to indicate input vals, defs, and types?

 input object pattern? declare an erased object named `args` that the
 trait can take as input?

 do we want templates and macros?

 i think the trait macro needs to return something other than Expr[T]

 how/when is the macro "code" run?

 for now - restrict the grammar to adding new symbols (methods,vals,types)

 a modification should act a bit like a state monad, when asking for
 current context the entity should look like it has had all transforms
 applied (event sourced model?)
   - that would mean modifications are TemplateBody => TemplateBody in general
   but limited to (TemplateBody, CurrentState) => ModRule for debugging context
   - should be able to see other mod rules?


two methods
  1) a special method in an Object, similar to derived
    plus a special keyword to denote transformation by said method
  2) inline trait + def[this] for purely simple additive changes to templates
    still not quite clear how this would work, Expr as constructor body? special mixin method?

how does this get integrated into the AST?
should look like function application?
deriving uses a specially named method call `derived` in the companion

limited to what can be done with inline params and the self type

 could the `export` keyword be used?

 combine internal object with export clause?
 
    import scala.quoted._
    
    object ExpandMacro {
      def somemacroimpl(b: Expr[Boolean])(using Quotes): Expr[String] = {
        summon[Quotes].appliedTo()
        if (b.valueOrError)
          '{"true"}
        else
          '{"false"}
      }
    }

    object example {
      import scala.quoted._

      def somemacroimpl(b: Expr[Boolean])(using Quotes): Expr[String] = {
        if (b.valueOrError)
          '{"true"}
        else
          '{"false"}
      }

      inline def somemacro(inline b: Boolean): String =
        ${somemacroimpl('b)}
    }

    object uses {
      import example._

      somemacro(true)
    }


///////////////////


groovy like meta programming in scala

register traits (unit of behavior) with the meta class to mixin behavior

val e = new Expando()

trait Named {
  var name: String
}

Expando.metaclass.addBehavior[Named]

// flow typing?
val en = e.as[Named]
en.name = "bob"
