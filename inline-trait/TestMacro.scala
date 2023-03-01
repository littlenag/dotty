 import scala.quoted._

 object TestMacro {
   def dothis(b: Boolean)(using Quotes): Expr[Any] = {
     import quotes.reflect._
     if (b)
       '{
         object fizzle {
           def withFizzle = 12
         }
       }
     else
       '{
         object swizzle {
           def withSwizzle = "swizzle"
         }
       }
   }
 }
 