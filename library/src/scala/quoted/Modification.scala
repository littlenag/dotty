package scala.quoted

/** Quoted modification that can transform an object, class, or trait by
 * adding, removing, or mutating methods, vals, etc.
 *
 *  TODO `Modification` has extension methods that are defined in `scala.quoted.Quotes`.
 *
 *  TODO add a way to inject source location for debug tracking
 */
sealed trait Modification

// Add a method to a trait, class, or object
//case class AddMethod(methodName: String, body:Expr[Any]) extends Modification

// Simple direct mixin added to the body of a trait
case class Mixin(mixin:Expr[Any]) extends Modification

// TODO use the expect way of combining these. Keep in mind, mods are ORDERED!

//case class Modifications(mods: List[Modification]) extends Modification

/** Constructors for modifications */
object Modification {

//  def addMethod(inline methodName:String)(body:Expr[Any]): Modification =
//    AddMethod(methodName, body)

  def mixin(body:Expr[Any]): Modification =
    Mixin(body)
}
