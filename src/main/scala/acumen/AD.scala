package acumen

import interpreters.enclosure.Interval

import util.ASTUtil

/**
 * Taylor series arithmetic.
 *
 * Based on description from book "Validated Numerics" by
 * Warrick Tucker, pages 121 and 122.
 */
object AD extends App {
  
  // FIXME Should this really be a partial ordering? 
  //       Do the properties hold for intervals?
  trait Num[V] extends PartialOrdering[V] {
    def add(l: V, r: V): V
    def sub(l: V, r: V): V
    def mul(l: V, r: V): V
    def div(l: V, r: V): V
    def neg(x: V): V
    def sin(x: V): V
    def cos(x: V): V
    def exp(x: V): V
    def log(x: V): V
    def lift(i: Int): V
    def zero: V
    def one: V
  }
  implicit class NumOps[V](val l: V)(implicit ev: Num[V]) {
    def +(r: V): V = ev.add(l, r)
    def -(r: V): V = ev.sub(l, r)
    def *(r: V): V = ev.mul(l, r)
    def /(r: V): V = ev.div(l, r)
    def < (r: V): Boolean = ev.lt(l, r)
    def > (r: V): Boolean = ev.gt(l, r)
    def <= (r: V): Boolean = ev.lteq(l, r)
    def >= (r: V): Boolean = ev.gteq(l, r)
    def unary_- = ev.neg(l)
    def sin: V = ev.sin(l)
    def cos: V = ev.cos(l)
    def exp: V = ev.exp(l)
    def log: V = ev.log(l)
    def zero: V = ev.zero
    def one: V = ev.one
  }

  implicit object IntIsNum extends Num[Int] {
    def add(l: Int, r: Int): Int = l + r
    def sub(l: Int, r: Int): Int = l - r
    def mul(l: Int, r: Int): Int = l * r
    def div(l: Int, r: Int): Int = l / r
    def neg(x: Int): Int = -x
    def sin(x: Int): Int = ???
    def cos(x: Int): Int = ???
    def exp(x: Int): Int = x.exp
    def log(x: Int): Int = x.log
    def lift(x: Int): Int = x
    def zero: Int = 0
    def one: Int = 1
    def tryCompare(l: Int, r: Int): Option[Int] = Some(l compareTo r)
    def lteq(l: Int, r: Int): Boolean = l <= r
  }
  implicit object DoubleIsNum extends Num[Double] {
    def add(l: Double, r: Double): Double = l + r
    def sub(l: Double, r: Double): Double = l - r
    def mul(l: Double, r: Double): Double = l * r
    def div(l: Double, r: Double): Double = l / r
    def neg(x: Double): Double = -x
    def sin(x: Double): Double = Math.sin(x)
    def cos(x: Double): Double = Math.cos(x)
    def exp(x: Double): Double = Math.exp(x)
    def log(x: Double): Double = Math.log(x)
    def lift(x: Int): Double = x
    def zero: Double = 0
    def one: Double = 1
    def tryCompare(l: Double, r: Double): Option[Int] = Some(l compareTo r)
    def lteq(l: Double, r: Double): Boolean = l <= r
  }
  implicit object IntervalIsNum extends Num[Interval] {
    def add(l: Interval, r: Interval): Interval = l + r
    def sub(l: Interval, r: Interval): Interval = l - r
    def mul(l: Interval, r: Interval): Interval = l * r
    def div(l: Interval, r: Interval): Interval = l / r
    def neg(x: Interval): Interval = -x
    def sin(x: Interval): Interval = x.sin
    def cos(x: Interval): Interval = x.cos
    def exp(x: Interval): Interval = x.exp
    def log(x: Interval): Interval = x.log
    def lift(x: Int): Interval = Interval(x)
    def zero: Interval = Interval.zero
    def one: Interval = Interval.one
    // FIXME Check if something needs to be overridden
    def lteq(l: Interval, r: Interval): Boolean = l lessThanOrEqualTo r
    def tryCompare(l: Interval, r: Interval): Option[Int] =
      if (l == r)         Some(0)
      else if (lteq(l,r)) Some(1) 
      else if (lteq(r,l)) Some(-1)
      else                None
  }
  class DifAsNum[V: Num] extends Num[Dif[V]] {
    /* Caches */
    val mulCache = collection.mutable.HashMap[(Dif[V], Dif[V]), Dif[V]]()
    val divCache = collection.mutable.HashMap[(Dif[V], Dif[V]), Dif[V]]()
    val sinAndCosCache = collection.mutable.HashMap[Dif[V], (/*sin*/Dif[V], /*cos*/Dif[V])]()
    val expCache = collection.mutable.HashMap[Dif[V], Dif[V]]()
    val logCache = collection.mutable.HashMap[Dif[V], Dif[V]]()
    /* Constants */
    val evVIsNum = implicitly[Num[V]]
    val zeroOfV = evVIsNum.zero
    val oneOfV = evVIsNum.one
    /* Num instance */
    def add(l: Dif[V], r: Dif[V]): Dif[V] = Dif((l.coeff, r.coeff).zipped.map(_ + _))
    def sub(l: Dif[V], r: Dif[V]): Dif[V] = Dif((l.coeff, r.coeff).zipped.map(_ - _))
    def mul(l: Dif[V], r: Dif[V]): Dif[V] =
      // FIXME Implement using mutable data, but keep the immutable interface
      mulCache.getOrElseUpdate((l, r),
        Dif((for (k <- 0 until l.size) yield ((0 to k).foldLeft(zeroOfV) {
          case (sum, i) => sum + (l(i) * r(k - i))
        })).toVector))
    def div(l: Dif[V], r: Dif[V]): Dif[V] =
      // FIXME Extend using l’Hopital’s rule
      divCache.getOrElseUpdate((l, r), Dif {
        val n = l.size
        val coeff = new collection.mutable.ArraySeq[V](n)
        coeff(0) = l(0) / r(0)
        for (k <- 1 until n)
          coeff(k) = (l(k) - (0 to k - 1).foldLeft(zeroOfV) {
            case (sum, i) => sum - (coeff(i) * r(k - i))
          }) / r(0)
        coeff.toVector
      })
    def neg(x: Dif[V]): Dif[V] = Dif(x.coeff.map(y => -y))
    def sin(x: Dif[V]): Dif[V] = sinAndCos(x)._1
    def cos(x: Dif[V]): Dif[V] = sinAndCos(x)._2
    private def sinAndCos(x: Dif[V]): (Dif[V],Dif[V]) =
      sinAndCosCache.getOrElseUpdate(x,{
        val n = x.size
        val sinCoeff = new collection.mutable.ArraySeq[V](n)
        val cosCoeff = new collection.mutable.ArraySeq[V](n)
        sinCoeff(0) = x(0).sin
        cosCoeff(0) = x(0).cos
        for (k <- 1 until n) {
          val (sck, cck) = (1 to k).foldLeft(zeroOfV, zeroOfV) {
            case ((sckSum, cckSum), i) => 
              val ixi = evVIsNum.lift(i) * x(i)
              ( sckSum + ixi * cosCoeff(k - i)
              , cckSum + ixi * sinCoeff(k - i) )
          }
          val kL = evVIsNum.lift(k)
          sinCoeff(k) =  sck / kL
          cosCoeff(k) = -cck / kL
        }
        (Dif(sinCoeff.toVector), Dif(cosCoeff.toVector))
      })
    def exp(x: Dif[V]): Dif[V] =
      expCache.getOrElseUpdate(x, Dif {
        val n = x.size
        val coeff = new collection.mutable.ArraySeq[V](n)
        coeff(0) = x(0).exp
        for (k <- 1 until n)
          coeff(k) = ((1 to k).foldLeft(zeroOfV) {
            case (sum, i) => sum + evVIsNum.lift(i) * x(i) * coeff(k-i)
          }) / evVIsNum.lift(k)
        coeff.toVector
      })
    /** Natural logarithm */
    def log(x: Dif[V]): Dif[V] =
      logCache.getOrElseUpdate(x, Dif {
        val n = x.size
        val coeff = new collection.mutable.ArraySeq[V](n)
        val x0 = x(0)
        coeff(0) = x0.log
        for (k <- 1 until n) {
          val kL = evVIsNum.lift(k)          
          coeff(k) = (x(k) - ((1 to k - 1).foldLeft(zeroOfV) {
            case (sum, i) => sum + evVIsNum.lift(i) * coeff(i) * x(k-i)
          }) / kL) / x0
        }
        coeff.toVector
      })
    // TODO Add square function according to Griewank book
    def lift(x: Int): Dif[V] = Dif.constant(evVIsNum lift x)
    // FIXME Test these definitions
    def zero: Dif[V] = Dif.fill(zeroOfV)
    def one: Dif[V] = Dif.constant(oneOfV)
    def tryCompare(l: Dif[V], r: Dif[V]): Option[Int] = evVIsNum.tryCompare(l(0), r(0))
    def lteq(l: Dif[V], r: Dif[V]): Boolean = evVIsNum.lteq(l(0), r(0))
  }
  implicit object IntDifIsNum extends DifAsNum[Int]
  implicit object DoubleDifIsNum extends DifAsNum[Double]
  implicit object IntervalDifIsNum extends DifAsNum[Interval]
  
  case class Dif[V](coeff: Vector[V]) {
    import Dif._
    require(coeff.size == dim, s"Tried to create Dif with incompatible dimension (${coeff.size} ~= $dim).")
    def apply(i: Int) = coeff(i)
    def size: Int = coeff.size
  }
  object Dif {
    val dim: Int = 10 // FIXME Expose this as a simulator parameter
    /** Lift a constant value of type A to a Dif. */
    def constant[V: Num](a: V) = padWithZeros(Vector(a, implicitly[Num[V]].zero))
    /** Lift a variable of type V to a Dif. The value a is the current value of the variable. */
    def variable[V: Num](a: V) = padWithZeros(Vector(a, implicitly[Num[V]].one))
    /** Use a sequence of values of type V as the coefficients of a Dif.
     *  The sequence is padded to dim with the zero of V. */
    def apply[V: Num](as: V*) = padWithZeros(as.toVector)
    /** Use a repeated sequence of the value a as the coefficients of a Dif. */
    def fill[V: Num](a: V) = Dif(Vector.fill(dim)(a))
    /** Create a Dif by filling the missing positions up to dim with the zero of V. */
    private def padWithZeros[V: Num](v: Vector[V]): Dif[V] = Dif(v.padTo(dim, implicitly[Num[V]].zero))
  }
  
  /** Lift all numeric values in a CStore into Difs */
  def lift(st: CStore): CStore = st.mapValues(_.mapValues{
    case VLit(GInt(i)) => VLit(GIntDif(Dif.constant(i)))
    case VLit(GDouble(n)) => VLit(GDoubleDif(Dif.constant(n)))
    case v => v 
  })
  
  /** Lift all numeric values in an Expr into Difs */
  def lift(e: Expr): Expr = new acumen.util.ASTMap {
    override def mapExpr(e: Expr): Expr = (e match {
      case Lit(GInt(i)) => Lit(GIntDif(Dif.constant(i)))
      case Lit(GDouble(d)) => Lit(GDoubleDif(Dif.constant(d)))
      case _ => super.mapExpr(e)
    }).setPos(e.pos)
  }.mapExpr(e)
 
  /** Lower all Dif values in a CStore into the corresponding numeric value */
  def lower(st: CStore): CStore = {
    st.mapValues(_.mapValues{
      case VLit(GIntDif(Dif(v))) => VLit(GInt(v(0)))
      case VLit(GDoubleDif(Dif(v))) => VLit(GDouble(v(0)))
      case v => v 
    }) 
  }
  
  /** Lower all Dif values in an Expr into the corresponding numeric value */
  def lower(e: Expr): Expr = new acumen.util.ASTMap {
    override def mapExpr(e: Expr): Expr = (e match {
      case Lit(GIntDif(Dif(v))) => Lit(GInt(v(0)))
      case Lit(GDoubleDif(Dif(v))) => Lit(GDouble(v(0)))
      case _ => super.mapExpr(e)
    }).setPos(e.pos)
  }.mapExpr(e)
  
  /* Sandbox */
  
  val s: CStore = Map(CId(0) -> Map(Name("x",0) -> VLit(GDouble(-3.0))))

  println("s: " + s)
  println("lift(s): " + lift(s))
  println("lower(lift(s)): " + lower(lift(s)))
  
//  val dd = Dif(1.0, 2.0)
//  val id = Dif(Interval(1.0), Interval(1.0))

//  def divide[A: Num](l: A, r: A): A = l / r

//  println(divide(1.0, 2.0))
//  println(divide(dd, dd))
//  println(divide(id, id))
//  println(Dif.variable(Interval(1)) * Dif.variable(Interval(1)))

}
