package es.weso.shex.validator

import cats._
import es.weso.rbe.interval.{IntLimit, IntOrUnbounded, Unbounded}
import es.weso.rbe.{Schema => _, Star => _, _}
import es.weso.shex._


object table {

  type ConstraintsMap = Map[ConstraintRef, Option[ShapeExpr]]
  type PathsMap = Map[Path, Set[ConstraintRef]]
  type ResultPair = (CTable, Rbe[ConstraintRef])

  case class ConstraintRef(n: Int) extends AnyVal {
    override def toString(): String = s"C$n"
  }

  implicit lazy val orderingConstraintRef = new Ordering[ConstraintRef] {
    def compare(c1: ConstraintRef, c2: ConstraintRef): Int = {
      Ordering[Int].compare(c1.n, c2.n)
    }
  }

  // Constraints table
  case class CTable(constraints: ConstraintsMap,
                    paths: PathsMap,
                    elems: Int,
                    schema: Schema
                   ) {

    def addPath(p: Path, n: ConstraintRef): PathsMap =
      paths.updated(p, paths.get(p).getOrElse(Set()) + n)

    def getShapeExpr(cref: ConstraintRef): Option[ShapeExpr] = {
      constraints.get(cref).flatten
    }

    lazy val isAmbiguous: Boolean = {
      paths.values.map(_.size).exists(_ > 1)
    }

  }

  object CTable {
    def empty: CTable = CTable(Map(), Map(), 0, Schema.empty)

    def mkTable(te: TripleExpr): ResultPair =
      mkTableAux(te, CTable.empty)

    def mkTableAux(te: TripleExpr, current: CTable): ResultPair = {
      te match {
        case e: EachOf => {
          val zero: ResultPair = (current, Empty)

          def comb(pair: ResultPair, te: TripleExpr): ResultPair = {
            val (currentTable, currentRbe) = pair
            val (newTable, newRbe) = mkTableAux(te, currentTable)
            val rbe =
              if (Cardinality.isDefault(e.min, e.max))
                And(currentRbe, newRbe)
              else Repeat(And(currentRbe, newRbe), e.min, max2IntOrUnbounded(e.max))
            (newTable, rbe)
          }

          e.expressions.foldLeft(zero)(comb)
        }
        case e: SomeOf => {
          // TODO: Not sure the empty case is right...should use "reduce" ?
          val zero: ResultPair = (current, Empty)

          def comb(pair: ResultPair, te: TripleExpr): ResultPair = {
            val (currentTable, currentRbe) = pair
            val (newTable, newRbe) = mkTableAux(te, currentTable)
            val rbe =
              if (Cardinality.isDefault(e.min, e.max))
                Or(currentRbe, newRbe)
              else Repeat(Or(currentRbe, newRbe), e.min, max2IntOrUnbounded(e.max))
            (newTable, rbe)
          }

          e.expressions.foldLeft(zero)(comb)
        }
        case i: Inclusion =>
          throw new Exception("CTable: Not implemented table generation for inclusion")

        case tc: TripleConstraint => {
          val newElems = current.elems + 1
          val cref = ConstraintRef(newElems)
          val newTable = current.copy(
            elems = newElems,
            constraints = current.constraints + (cref -> tc.valueExpr),
            paths = current.addPath(tc.path, cref)
          )
          val symbol = Symbol(cref, tc.min, max2IntOrUnbounded(tc.max))
          println(s"Making table for tc $tc. Negated: ${tc.negated}")
          (newTable,
            if (tc.negated) Repeat(symbol,0,0)
            else symbol)
        }
      }
    }

    def max2IntOrUnbounded(m: Max): IntOrUnbounded = {
      m match {
        case IntMax(v) => IntLimit(v)
        case Star => Unbounded
      }
    }
  }

  implicit lazy val showCTable = new Show[CTable] {
    override def show(table: CTable) = {
      s"""CTable { constraints: ${table.constraints.toString}\n
         | paths: ${table.paths.toString}""".stripMargin
    }
  }
}