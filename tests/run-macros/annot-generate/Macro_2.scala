import scala.annotation.{experimental, MacroAnnotation}
import scala.quoted._

@experimental
class foo extends MacroAnnotation {
  def transform(using Quotes)(tree: quotes.reflect.Definition): List[quotes.reflect.Definition] =
    import quotes.reflect._
    tree match
      case DefDef(name, params, tpt, Some(t)) =>
        val rhs = '{
          @hello def foo(x: Int): Int = x + 1
          ${t.asExprOf[Int]}
        }.asTerm
        val newDef = DefDef.copy(tree)(name, params, tpt, Some(rhs))
        List(newDef)
}
