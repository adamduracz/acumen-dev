package acumen.interpreters.enclosure.solver

import acumen.interpreters.enclosure._
import acumen.interpreters.enclosure.Interval._
import acumen.interpreters.enclosure.Rounding._
import acumen.interpreters.enclosure.Types._

object PlotTest extends App {
  implicit val rnd = Rounding(10)
  val H = Systems.BB(10, 0.5)
  val T = 0 /\ 0.5
  val delta = 0
  val m = 20
  val n = 200
  val K = 30
  val d = 0.1
  val res = SolveVt.solveVt(H.fields(Mode("Fly")), 0 /\ 1.5, Box("x" -> 5, "x'" -> 0), delta, m, n, "output")
  UnivariateAffineScalarEnclosure.plot(res.components.values.toSeq: _*)
}

object BouncingBall extends App with Solver {
  implicit val rnd = Rounding(10)
  val H = Systems.BB(10, 0.5)
  val T = Interval(0, 3)
  val Ss = Set(UncertainState(Mode("Fly"), Box("x" -> 5, "x'" -> 0)))
  val delta = 0
  val m = 20
  val n = 200
  val K = 30
  val d = 0.01
  val e = T.width match { case Interval(_, hi) => hi.doubleValue }
  val start = System.currentTimeMillis
  val res = solver(H, T, Ss, delta, m, n, K, d, e, T, "output")
  val end = System.currentTimeMillis
  val time = end - start
  println("computed " + res.size + " enclosures in " + time / 1000.0 + " seconds")
  val step = 1 //scala.math.max(1, res.size / 1000)
  var es = Seq[UnivariateAffineEnclosure]()
  var i = 0
  while (i < res.size) {
    es = es :+ res(i)
    i += step
  }
  //  res.map(println(_))
  UnivariateAffineScalarEnclosure.plot("x'' = -10, x'(0) = 0, x(0) = 5, min time step = 0.001")(res.map(_("x")): _*)
  //  AffineEnclosure.plot(es: _*)
}

object TwoTanks extends App with Solver {
  implicit val rnd = Rounding(20)
  val H = Systems.TT(2, 3, 4)
  val T = Interval(0, 2.5)
  val Ss = Set(UncertainState(Mode("FillLeft"), Box("left" -> 1, "right" -> 1)))
  val delta = 0
  val m = 20
  val n = 200
  val K = 30
  val d = math.pow(2,-21)
  val e = T.width match { case Interval(_, hi) => hi.doubleValue }
  val start = System.currentTimeMillis
  val res = solver(H, T, Ss, delta, m, n, K, d, e, T, "output")
  val end = System.currentTimeMillis
  val time = end - start
  println("computed " + res.size + " enclosures in " + time / 1000.0 + " seconds")
  // TODO implement plot for UnivariateAffineEnclosure
  UnivariateAffineScalarEnclosure.plot(res.flatMap(e => Seq(e("left"), e("right"))): _*)
}

object Saw extends App with Solver {
  implicit val rnd = Rounding(10)
  val H = Systems.Saw
  val T = Interval(0, 2.1)
  val Ss = Set(UncertainState(Mode("Down"), Box("x" -> 1)))
  val delta = 0
  val m = 20
  val n = 200
  val K = 30
  val d = 4.7e-7
  val e = T match { case Interval(_, hi) => hi.doubleValue / 2 }
  val start = System.currentTimeMillis
  val res = solver(H, T, Ss, delta, m, n, K, d, e, T, "output")
  val end = System.currentTimeMillis
  val time = end - start
  println("computed " + res.size + " enclosures in " + time / 1000.0 + " seconds")
  // TODO implement plot for UnivariateAffineEnclosure
  //  UnivariateAffineEnclosure.plot(res: _*)
}
