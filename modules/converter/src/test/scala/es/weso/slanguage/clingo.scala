package es.weso.slanguage

object Clingo {

  sealed trait Term extends Product with Serializable {
    def show: String
  }

  sealed trait SimpleTerm extends Term {
    def show: String
  }
  final case class Const(name: String) extends SimpleTerm {
    require(!name.isEmpty, s"Constant($name) must not be empty")
    require(name.head.isLower, s"Constant($name) must start by a lowercase")
    override def show: String = name
  }
  final case class StrTerm(str: String) extends SimpleTerm {
    override def show: String = "\"" ++ str ++ "\""
  }
  final case class IntTerm(n: Int) extends SimpleTerm {
    override def show: String = n.toString
  }
  final case class StringTerm(str: String) extends SimpleTerm {
    override def show: String = "\"" ++ str ++ "\""
  }
  final case class Var(name: String) extends SimpleTerm {
    require(!name.isEmpty, s"Variable($name) must not be empty")
    require(name.head.isUpper, s"Variable($name) must start by uppercase")
    override def show: String = name
  }
  final case object Undef extends SimpleTerm {
    override def show: String = "_"
  }
  final case class Func(name: String, terms: List[Term]) extends Term {
    require(!name.isEmpty, s"Function($name,...) must not be empty")
    require(name.head.isLower, s"Function($name,...) must start by a lowercase")
    override def show: String = s"$name(${terms.map(_.show).mkString(",")})"
  }
  final case class Tuple(terms: List[Term]) extends Term {
    override def show: String = s"(${terms.map(_.show).mkString(",")}))"
  }

  sealed trait Atom extends Product with Serializable {
    def show: String
  }

  /**
  * Constant atom
    * @param c constant
    */
  final case class Constant(c: Const) extends Atom {
    override def show: String = c.show
  }

  /**
  * Function atom
    * @param f
    */
  final case class Function(f: Func) extends Atom {
    override def show: String = f.show
  }

  sealed trait Literal extends Product with Serializable {
   def show: String
  }
  final case class Pos(a: Atom) extends Literal {
    override def show: String = a.show
  }
  final case class Neg(a: Atom) extends Literal {
    override def show: String = s"not ${a.show}"
  }

  private def showBody(body: List[Literal]): String = {
    body.map(_.show).mkString(",")
  }

  sealed trait Statement extends Product with Serializable {
    def show: String
  }
  final case class Rule(head: Literal, body: List[Literal]) extends Statement {
    override def show: String = s"${head.show}:-${showBody(body)}"
  }
  final case class Fact(head: Literal) extends Statement {
    override def show: String = head.show
  }
  final case class Constraint(body: List[Literal]) extends Statement {
    override def show: String = s":-${showBody(body)}"
  }
  final case class ShowDirective(name: String, args: Int) extends Statement {
    require(args >= 0, s"ShowDirective($name,$args), args $args must be >= 0")
    override def show: String = s"#show ${name}/$args"
  }

  case class Program(statements: List[Statement]) {
    def show: String = {
      val zero = ""
      def comb(s: Statement, rest: String
              ): String = s"${s.show}.\n$rest"
      statements.foldRight(zero)(comb)
    }
  }
}