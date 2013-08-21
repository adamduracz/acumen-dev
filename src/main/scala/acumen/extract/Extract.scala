// Extract a Hybrid automaton

// Mostly follows algo.txt in https://bitbucket.org/effective/paper-esaes
//  (commit e9a85f9779f26f8719d43d41599074b5725a2332)
//  The SPLIT, TRANSFORM and FLATTEN are all done at once during the
//  extraction into a normal form in which all other transformation
//  are done.

package acumen
package extract

import scala.collection.mutable.{ ListMap => MutListMap, ArrayBuffer }
import scala.util.control.Breaks.{ break, breakable }

import Pretty._
import Cond.getName

/***************************************************************************
 * The algorithm
 ***************************************************************************/

class Extract(prog: Prog, 
              val allowSeqIfs: Boolean = false) extends MainClass(prog)
{
  // Constant
  val MODE = Name("$mode", 0)
  val MODE_VAR = Dot(Var(Name("self", 0)), MODE)

  // Builds the initial data structures
  extract(allowSeqIfs)

  // Removes simulator assigns at the top level.  Needs to be done before 
  //  uniquify is called.  (note that foreach is applied to an Option
  //  not a collection; (the overloading of names in Scala like this
  //  is rather unfortunate and confusing on by view -kevina))
  discrIfs.data.get(Nil).foreach(extractSimulatorAssigns(_))

  // Push down all continuous actions to the deepest nested if.
  // Each reaming if becomes a mode.
  contIfs.pushDown

  // Now do magic (FIXME: define magic)
  contIfs.data.values.foreach { cIf =>
    val dIf = discrIfs.addwEmpties(cIf.conds)
    dIf.actions += Assign(MODE_VAR, Lit(GStr(cIf.label)))
  }

  // Now that we added the magic we can push down the discrete actions
  discrIfs.pushDown

  // If we are not already in a mode after a reset go into the special
  // D0 mode
  discrIfs.data.values.foreach { dIf => 
    if (getName(dIf.actions.last.lhs).orNull != MODE) {
      dIf.actions += Assign(MODE_VAR, Lit(GStr("D0")))
    }
  }

  // Generate a list of possible modes, this is the 
  // speical "D0" mode and all the contifs
  val modes = new ContIf(Nil, "D0") +: contIfs.data.values.toList

  // For every mode add indiscreetly add all the discrIfs as possible
  // events
  modes.foreach { m =>
    m.resets = discrIfs.data.values.toList
  }

  //
  // Converts the modes into a big switch and create a new model
  //

  val theSwitch = Switch(MODE_VAR,
                         modes.map{m => Clause(GStr(m.label),
                                               Cond.toExpr(m.claims),
                                               m.resets.map(_.toAST) ++
                                               m.actions.map(Continuously(_)).toList)})
  val newMain = ClassDef(origDef.name,
                         origDef.fields,
                         init :+ Init(MODE,ExprRhs(Lit(GStr("D0")))), 
                         List(theSwitch) ++ simulatorAssigns.map(Discretely(_)))

  val res = new Prog(List(newMain))
}

