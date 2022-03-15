package dotty.tools.dotc.typer

import dotty.tools.dotc.ast.untpd
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Symbols.Symbol

class ExportMacroInfo(symf: Context ?=> Symbol,
                      val qualifier: untpd.Tree
                     ) {

}
